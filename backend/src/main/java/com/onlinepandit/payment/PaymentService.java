package com.onlinepandit.payment;

import com.onlinepandit.booking.Booking;
import com.onlinepandit.booking.BookingRepository;
import com.onlinepandit.identity.UserAccount;
import com.onlinepandit.identity.UserAccountRepository;
import com.onlinepandit.notification.NotificationService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Currency;
import java.util.Locale;
import com.onlinepandit.marketplace.Pandit;
import com.onlinepandit.marketplace.PanditRepository;

@Service
public class PaymentService {
    private final PaymentRepository payments;
    private final BookingRepository bookings;
    private final UserAccountRepository users;
    private final NotificationService notifications;
    private final ApplicationEventPublisher events;
    private final PanditRepository pandits;
    @Value("${app.payments.platform-fee-bps:1000}")
    private int platformFeeBps = 1000;
    private final String stripeSecret;
    private final String stripeWebhookSecret;
    private final String successUrl;
    private final String cancelUrl;

    public PaymentService(PaymentRepository payments, BookingRepository bookings, UserAccountRepository users,
                          NotificationService notifications, ApplicationEventPublisher events,
                          PanditRepository pandits,
                          @Value("${app.payments.stripe.secret-key:}") String stripeSecret,
                          @Value("${app.payments.stripe.webhook-secret:}") String stripeWebhookSecret,
                          @Value("${app.payments.stripe.success-url}") String successUrl,
                          @Value("${app.payments.stripe.cancel-url}") String cancelUrl) {
        this.payments = payments;
        this.bookings = bookings;
        this.users = users;
        this.notifications = notifications;
        this.events = events;
        this.pandits = pandits;
        this.stripeSecret = stripeSecret;
        this.stripeWebhookSecret = stripeWebhookSecret;
        this.successUrl = successUrl;
        this.cancelUrl = cancelUrl;
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public CheckoutResponse createCheckout(Long bookingId, Long userId) {
        requireStripeSecret();
        Booking booking = bookings.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        if (!booking.getUserId().equals(userId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (!"CONFIRMED".equals(booking.getBookingStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only a confirmed booking can be paid");
        }
        if ("CANCELLED".equals(booking.getBookingStatus()) || "COMPLETED".equals(booking.getBookingStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This booking is not payable");
        }
        if ("PAID".equals(booking.getPaymentStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This booking has already been paid");
        }

        Pandit pandit = pandits.findById(booking.getPanditId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Practitioner not found"));
        if (pandit.getStripeAccountId() == null || !pandit.isStripePayoutsEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Practitioner has not completed payment onboarding");
        }
        if (platformFeeBps < 0 || platformFeeBps >= 10_000) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Platform fee configuration is invalid");
        }
        BigDecimal platformFee = booking.getTotalAmount().multiply(BigDecimal.valueOf(platformFeeBps))
            .divide(BigDecimal.valueOf(10_000), 2, RoundingMode.HALF_UP);
        if (platformFee.compareTo(BigDecimal.ZERO) <= 0 || platformFee.compareTo(booking.getTotalAmount()) >= 0) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Platform fee must be between zero and the booking amount");
        }
        PaymentTransaction payment = payments.findByBookingId(bookingId).orElseGet(() ->
            payments.save(new PaymentTransaction(bookingId, userId, booking.getTotalAmount(), booking.getCurrency(), platformFee)));
        if (payment.getCheckoutUrl() != null && "CREATED".equals(payment.getPaymentStatus())) {
            return checkoutResponse(payment);
        }
        if (!"CREATED".equals(payment.getPaymentStatus()) && !"EXPIRED".equals(payment.getPaymentStatus()) &&
                !"FAILED".equals(payment.getPaymentStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A payment is already being processed");
        }
        int attempt = payment.startNewCheckoutAttempt();
        payments.save(payment);

        UserAccount customer = users.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        try {
            Stripe.apiKey = stripeSecret;
            Currency paymentCurrency = Currency.getInstance(payment.getCurrency());
            int fractionDigits = paymentCurrency.getDefaultFractionDigits();
            if (fractionDigits < 0) throw new IllegalArgumentException("Unsupported Stripe currency precision");
            long minorUnits = payment.getAmount().movePointRight(fractionDigits).longValueExact();
            long feeMinorUnits = payment.getPlatformFee().movePointRight(fractionDigits).longValueExact();
            String currency = paymentCurrency.getCurrencyCode().toLowerCase(Locale.ROOT);
            SessionCreateParams.LineItem.PriceData.ProductData product = SessionCreateParams.LineItem.PriceData.ProductData.builder()
                    .setName("OnlinePandit booking #" + bookingId)
                    .build();
            SessionCreateParams.LineItem.PriceData price = SessionCreateParams.LineItem.PriceData.builder()
                    .setCurrency(currency)
                    .setUnitAmount(minorUnits)
                    .setProductData(product)
                    .build();
            SessionCreateParams.LineItem lineItem = SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(price)
                    .build();
                SessionCreateParams.PaymentIntentData paymentIntent = SessionCreateParams.PaymentIntentData.builder()
                    .setApplicationFeeAmount(feeMinorUnits)
                    .setTransferData(SessionCreateParams.PaymentIntentData.TransferData.builder()
                        .setDestination(pandit.getStripeAccountId())
                        .build())
                    .build();
                SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl + "&session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(cancelUrl)
                    .setClientReferenceId(bookingId.toString())
                    .setCustomerEmail(customer.getEmail())
                    .putMetadata("bookingId", bookingId.toString())
                    .putMetadata("paymentId", payment.getId().toString())
                    .setPaymentIntentData(paymentIntent)
                    .addLineItem(lineItem)
                    .build();
            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey("onlinepandit-checkout-" + payment.getId() + "-" + attempt)
                    .build();
            Session session = Session.create(params, options);
            payment.attachCheckout(session.getId(), session.getUrl());
            booking.setPaymentStatus("PROCESSING");
            payments.save(payment);
            bookings.save(booking);
            return checkoutResponse(payment);
        } catch (StripeException | ArithmeticException | IllegalArgumentException exception) {
            payment.setStatus("FAILED");
            payments.save(payment);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not create payment checkout", exception);
        }
    }

    @Transactional
    public void processStripeWebhook(String rawBody, String signature) {
        if (stripeWebhookSecret.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Payment webhook is not configured");
        Event event;
        try {
            event = Webhook.constructEvent(rawBody, signature, stripeWebhookSecret);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe webhook signature");
        }
        if (!"checkout.session.completed".equals(event.getType()) &&
            !"checkout.session.async_payment_succeeded".equals(event.getType()) &&
            !"checkout.session.async_payment_failed".equals(event.getType()) &&
            !"checkout.session.expired".equals(event.getType())) return;

        Session session = (Session) event.getDataObjectDeserializer().getObject()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe event payload"));
        PaymentTransaction payment = payments.findByProviderSessionId(session.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment session not found"));
        Booking booking = bookings.findByIdForUpdate(payment.getBookingId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        payment = payments.findByProviderSessionIdForUpdate(session.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment session not found"));
        if (!payment.getBookingId().toString().equals(session.getClientReferenceId()) ||
                !payment.getBookingId().toString().equals(session.getMetadata().get("bookingId"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe session does not match booking");
        }
        if (!payment.getCurrency().equalsIgnoreCase(session.getCurrency()) ||
                session.getAmountTotal() == null ||
                payment.getAmount().movePointRight(2).longValueExact() != session.getAmountTotal()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe amount or currency does not match booking");
        }

        if ("checkout.session.expired".equals(event.getType())) {
            payment.setStatus("EXPIRED");
            booking.setPaymentStatus("PENDING");
            payments.save(payment);
            bookings.save(booking);
            return;
        }
        if ("checkout.session.async_payment_failed".equals(event.getType())) {
            payment.setStatus("FAILED");
            booking.setPaymentStatus("PENDING");
            payments.save(payment);
            bookings.save(booking);
            return;
        }
        if ("PAID".equals(payment.getPaymentStatus())) return;
        if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) return;
        if (!"CONFIRMED".equals(booking.getBookingStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking is no longer payable; manual refund review is required");
        }
        if (payment.markPaid(session.getPaymentIntent())) {
            booking.setPaymentStatus("PAID");
            bookings.save(booking);
            notifications.create(payment.getUserId(), "PAYMENT", "Payment received",
                    "Payment for booking " + booking.getId() + " was confirmed. Receipt " + payment.getReceiptNumber() + " is ready.");
                events.publishEvent(new PaymentConfirmedEvent(payment.getId()));
        }
        payments.save(payment);
    }

    @Transactional(readOnly = true)
    public PaymentTransaction getPayment(Long paymentId, Long userId, String role) {
        PaymentTransaction payment = payments.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        if (!"ADMIN".equals(role) && !payment.getUserId().equals(userId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return payment;
    }

    @Transactional(readOnly = true)
    public byte[] createReceiptPdf(Long paymentId, Long userId, String role) {
        PaymentTransaction payment = getPayment(paymentId, userId, role);
        if (!"PAID".equals(payment.getPaymentStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Receipt is available after verified payment");
        Booking booking = bookings.findById(payment.getBookingId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 20);
                content.newLineAtOffset(60, 735);
                content.showText("OnlinePanditDotG - Payment receipt");
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(0, -35);
                content.showText("Receipt: " + payment.getReceiptNumber());
                content.newLineAtOffset(0, -22);
                content.showText("Booking: #" + booking.getId());
                content.newLineAtOffset(0, -22);
                content.showText("Amount paid: " + payment.getCurrency() + " " + payment.getAmount().toPlainString());
                content.newLineAtOffset(0, -22);
                content.showText("Payment provider: Stripe");
                content.newLineAtOffset(0, -22);
                content.showText("Payment reference: " + safe(payment.getProviderPaymentId()));
                content.newLineAtOffset(0, -22);
                content.showText("Paid at: " + payment.getPaidAt());
                content.newLineAtOffset(0, -40);
                content.showText("Booking status: " + booking.getBookingStatus());
                content.endText();
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate receipt", exception);
        }
    }

    @Transactional(readOnly = true)
    public byte[] createReceiptPdfForBooking(Long bookingId, Long userId, String role) {
        PaymentTransaction payment = payments.findByBookingId(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        return createReceiptPdf(payment.getId(), userId, role);
    }

    private void requireStripeSecret() {
        if (stripeSecret.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe payments are not configured");
    }

    private static CheckoutResponse checkoutResponse(PaymentTransaction payment) {
        return new CheckoutResponse(payment.getId(), payment.getBookingId(), payment.getPaymentStatus(), payment.getCheckoutUrl());
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) return "Not available";
        return new String(value.getBytes(StandardCharsets.US_ASCII), StandardCharsets.US_ASCII).replaceAll("[^A-Za-z0-9_ -]", "?");
    }

    public record CheckoutResponse(Long paymentId, Long bookingId, String paymentStatus, String checkoutUrl) { }
}

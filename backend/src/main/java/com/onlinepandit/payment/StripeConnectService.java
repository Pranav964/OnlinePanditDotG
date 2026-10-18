package com.onlinepandit.payment;

import com.onlinepandit.marketplace.Pandit;
import com.onlinepandit.marketplace.PanditRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class StripeConnectService {
    private final PanditRepository pandits;
    private final String stripeSecret;
    private final String webhookSecret;
    private final String refreshUrl;
    private final String returnUrl;

    public StripeConnectService(PanditRepository pandits,
            @Value("${app.payments.stripe.secret-key:}") String stripeSecret,
            @Value("${app.payments.stripe.connect-webhook-secret:}") String webhookSecret,
            @Value("${app.payments.stripe.connect-refresh-url}") String refreshUrl,
            @Value("${app.payments.stripe.connect-return-url}") String returnUrl) {
        this.pandits = pandits;
        this.stripeSecret = stripeSecret;
        this.webhookSecret = webhookSecret;
        this.refreshUrl = refreshUrl;
        this.returnUrl = returnUrl;
    }

    @Transactional
    public OnboardingResponse createOnboardingLink(Long userId) {
        requireStripe();
        Pandit pandit = pandits.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Practitioner profile not found"));
        if (!pandit.isApproved()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Practitioner approval is required before payout onboarding");
        try {
            Stripe.apiKey = stripeSecret;
            String accountId = pandit.getStripeAccountId();
            if (accountId == null) {
                AccountCreateParams params = AccountCreateParams.builder()
                        .setType(AccountCreateParams.Type.EXPRESS)
                        .setCountry(countryCode(pandit.getCountry()))
                        .putMetadata("onlinepanditPanditId", pandit.getId().toString())
                        .putMetadata("onlinepanditUserId", userId.toString())
                        .build();
                Account account = Account.create(params);
                accountId = account.getId();
                pandit.setStripeAccountId(accountId);
                pandits.save(pandit);
            }
            AccountLink link = AccountLink.create(AccountLinkCreateParams.builder()
                    .setAccount(accountId)
                    .setRefreshUrl(refreshUrl)
                    .setReturnUrl(returnUrl)
                    .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                    .build());
            return new OnboardingResponse(accountId, link.getUrl(), pandit.isStripePayoutsEnabled());
        } catch (StripeException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not start payout onboarding", exception);
        }
    }

    @Transactional
    public PayoutStatus getStatus(Long userId) {
        requireStripe();
        Pandit pandit = pandits.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Practitioner profile not found"));
        if (pandit.getStripeAccountId() == null) return new PayoutStatus(false, false, false);
        try {
            Stripe.apiKey = stripeSecret;
            Account account = Account.retrieve(pandit.getStripeAccountId());
            updateAccount(account.getId(), Boolean.TRUE.equals(account.getPayoutsEnabled()));
            return new PayoutStatus(true, Boolean.TRUE.equals(account.getDetailsSubmitted()), Boolean.TRUE.equals(account.getPayoutsEnabled()));
        } catch (StripeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not retrieve payout status", exception);
        }
    }

    @Transactional
    public void processWebhook(String rawBody, String signature) {
        if (webhookSecret.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Connect webhook is not configured");
        Event event;
        try {
            event = Webhook.constructEvent(rawBody, signature, webhookSecret);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Connect webhook signature");
        }
        if (!"account.updated".equals(event.getType())) return;
        Account account = (Account) event.getDataObjectDeserializer().getObject()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid account event payload"));
        updateAccount(account.getId(), Boolean.TRUE.equals(account.getPayoutsEnabled()));
    }

    private void updateAccount(String accountId, boolean payoutsEnabled) {
        Pandit pandit = pandits.findByStripeAccountId(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Practitioner payout account not found"));
        pandit.setStripePayoutsEnabled(payoutsEnabled);
        pandits.save(pandit);
    }

    private void requireStripe() {
        if (stripeSecret.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe Connect is not configured");
    }

    private static String countryCode(String country) {
        String normalized = country.trim();
        if (normalized.matches("(?i)^[a-z]{2}$")) return normalized.toUpperCase(Locale.ROOT);
        if (normalized.equalsIgnoreCase("United Kingdom")) return "GB";
        for (String code : Locale.getISOCountries()) {
            Locale locale = new Locale.Builder().setRegion(code).build();
            if (locale.getDisplayCountry(Locale.ENGLISH).equalsIgnoreCase(normalized)) return code;
        }
        throw new IllegalArgumentException("Use a supported Stripe country in the practitioner profile");
    }

    public record OnboardingResponse(String stripeAccountId, String onboardingUrl, boolean payoutsEnabled) { }
    public record PayoutStatus(boolean accountCreated, boolean detailsSubmitted, boolean payoutsEnabled) { }
}

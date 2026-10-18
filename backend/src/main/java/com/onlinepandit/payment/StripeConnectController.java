package com.onlinepandit.payment;

import com.onlinepandit.security.JwtAuthenticationFilter.UserPrincipal;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pandits/me/payments")
public class StripeConnectController {
    private final StripeConnectService connect;
    public StripeConnectController(StripeConnectService connect) { this.connect = connect; }

    @PostMapping("/onboarding")
    public StripeConnectService.OnboardingResponse onboarding(Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        if (!"PANDIT".equals(user.role())) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN);
        return connect.createOnboardingLink(user.id());
    }

    @GetMapping("/status")
    public StripeConnectService.PayoutStatus status(Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        if (!"PANDIT".equals(user.role())) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN);
        return connect.getStatus(user.id());
    }

    @PostMapping("/webhook")
    @ResponseStatus(HttpStatus.OK)
    public void webhook(@RequestBody String body, @RequestHeader("Stripe-Signature") @NotBlank String signature) {
        connect.processWebhook(body, signature);
    }
}

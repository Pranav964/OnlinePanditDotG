package com.onlinepandit.marketplace;

import com.onlinepandit.identity.UserAccount;
import com.onlinepandit.identity.UserAccountRepository;
import com.onlinepandit.notification.NotificationService;
import com.onlinepandit.security.JwtAuthenticationFilter.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class MarketplaceController {
    private final ActivityRepository activities;
    private final PanditRepository pandits;
    private final UserAccountRepository users;
    private final NotificationService notifications;

    public MarketplaceController(ActivityRepository activities, PanditRepository pandits, UserAccountRepository users,
                                 NotificationService notifications) {
        this.activities = activities;
        this.pandits = pandits;
        this.users = users;
        this.notifications = notifications;
    }

    @GetMapping("/activities")
    public List<ActivityResponse> activities() {
        return activities.findAllByActiveTrueOrderByNameAsc().stream()
                .map(activity -> new ActivityResponse(activity.getId(), activity.getName(), activity.getCategory(),
                        activity.getDuration(), activity.getBasePrice(), activity.getReligion())).toList();
    }

    @GetMapping("/pandits")
    public List<PanditResponse> pandits(@RequestParam(required = false) String city,
                                         @RequestParam(required = false) String country,
                                         @RequestParam(required = false) String religion,
                                         @RequestParam(required = false) Long activityId) {
        String activityName = activityId == null ? null : activities.findById(activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found")).getName();
        return pandits.search(clean(city), clean(country), clean(religion), activityName).stream()
                .map(pandit -> new PanditResponse(pandit.getId(), pandit.getFullName(), pandit.getYearsExperience(),
                        pandit.getCity(), pandit.getRegion(), pandit.getCountry(), pandit.getReligion(),
                        pandit.getLanguages(), split(pandit.getSpecialties()), pandit.getImageUrl(),
                        pandit.getRating(), pandit.getReviewCount())).toList();
    }

    @PostMapping("/pandits/apply")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public PanditResponse applyAsPandit(@Valid @RequestBody PanditApplication request, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        if ("ADMIN".equals(principal.role()) || pandits.findByUserId(principal.id()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This account already has a practitioner profile");
        }
        UserAccount user = users.findById(principal.id()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Pandit pandit = pandits.save(new Pandit(user.getFullName(), request.yearsExperience(), request.dateOfBirth(),
                request.city().trim(), request.region(), request.country().trim(), request.religion().trim(),
                request.languages(), request.specialties(), user.getId()));
        return new PanditResponse(pandit.getId(), pandit.getFullName(), pandit.getYearsExperience(), pandit.getCity(),
                pandit.getRegion(), pandit.getCountry(), pandit.getReligion(), pandit.getLanguages(),
                split(pandit.getSpecialties()), pandit.getImageUrl(), pandit.getRating(), pandit.getReviewCount());
    }

    @PostMapping("/admin/activities")
    @ResponseStatus(HttpStatus.CREATED)
    public ActivityResponse createActivity(@Valid @RequestBody ActivityCreateRequest request) {
        Activity activity = activities.save(new Activity(request.name().trim(), request.category().trim(),
            request.duration().trim(), request.basePrice(), request.religion().trim()));
        return new ActivityResponse(activity.getId(), activity.getName(), activity.getCategory(), activity.getDuration(),
            activity.getBasePrice(), activity.getReligion());
    }

        @GetMapping("/admin/pandits/pending")
        public List<PanditResponse> pendingPandits() {
        return pandits.findByApprovedFalseOrderByIdAsc().stream()
            .map(pandit -> new PanditResponse(pandit.getId(), pandit.getFullName(), pandit.getYearsExperience(),
                pandit.getCity(), pandit.getRegion(), pandit.getCountry(), pandit.getReligion(),
                pandit.getLanguages(), split(pandit.getSpecialties()), pandit.getImageUrl(),
                pandit.getRating(), pandit.getReviewCount())).toList();
        }

        @PatchMapping("/admin/pandits/{id}/approval")
        @Transactional
        public PanditResponse approvePandit(@PathVariable Long id, @RequestParam boolean approved) {
        Pandit pandit = pandits.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        pandit.setApproved(approved);
        pandit = pandits.save(pandit);
        if (pandit.getUserId() != null) users.findById(pandit.getUserId()).ifPresent(user -> {
            user.setRole(approved ? UserAccount.Role.PANDIT : UserAccount.Role.USER);
            users.save(user);
            notifications.create(user.getId(), "ACCOUNT", approved ? "Practitioner profile approved" : "Practitioner application declined",
                approved ? "Your profile is approved. Sign in again to access your practitioner workspace."
                    : "Your practitioner application was not approved.");
        });
        return new PanditResponse(pandit.getId(), pandit.getFullName(), pandit.getYearsExperience(), pandit.getCity(),
            pandit.getRegion(), pandit.getCountry(), pandit.getReligion(), pandit.getLanguages(),
            split(pandit.getSpecialties()), pandit.getImageUrl(), pandit.getRating(), pandit.getReviewCount());
        }

    private static String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static List<String> split(String value) { return value == null ? List.of() : java.util.Arrays.stream(value.split(",")).map(String::trim).toList(); }

    public record ActivityResponse(Long id, String name, String category, String duration, BigDecimal price, String religion) { }
    public record PanditResponse(Long id, String fullName, int yearsExperience, String city, String region,
                                 String country, String religion, String languages, List<String> specialties,
                                 String imageUrl, BigDecimal rating, int reviewCount) { }
    public record ActivityCreateRequest(@NotBlank String name, @NotBlank String category, @NotBlank String duration,
                                        @DecimalMin("0.01") BigDecimal basePrice, @NotBlank String religion) { }
    public record PanditApplication(@Min(0) @Max(80) int yearsExperience, java.time.LocalDate dateOfBirth,
                                    @NotBlank String city, String region, @NotBlank String country,
                                    @NotBlank String religion, String languages, String specialties) { }
}

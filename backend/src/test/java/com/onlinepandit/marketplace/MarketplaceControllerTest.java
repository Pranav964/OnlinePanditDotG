package com.onlinepandit.marketplace;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.onlinepandit.identity.UserAccount;
import com.onlinepandit.identity.UserAccountRepository;
import com.onlinepandit.notification.NotificationService;
import com.onlinepandit.security.JwtAuthenticationFilter.UserPrincipal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class MarketplaceControllerTest {
    private static final Long USER_ID = 17L;

    @Mock
    private ActivityRepository activities;
    @Mock
    private PanditRepository pandits;
    @Mock
    private UserAccountRepository users;
    @Mock
    private NotificationService notifications;
    @InjectMocks
    private MarketplaceController controller;

    @Test
    void pendingApplicationDoesNotGrantPractitionerRole() {
        UserAccount account = mock(UserAccount.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new UserPrincipal(USER_ID, "practitioner@example.com", "USER"));
        when(pandits.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(users.findById(USER_ID)).thenReturn(Optional.of(account));
        when(account.getId()).thenReturn(USER_ID);
        when(account.getFullName()).thenReturn("Asha Mehta");
        when(pandits.save(any(Pandit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        controller.applyAsPandit(new MarketplaceController.PanditApplication(8, LocalDate.of(1988, 4, 12),
                "London", "Greater London", "United Kingdom", "Hindu", "Hindi, English", "Puja"), authentication);

        verify(account, never()).setRole(any(UserAccount.Role.class));
        verify(users, never()).save(any(UserAccount.class));
    }

    @Test
    void adminApprovalGrantsPractitionerRoleAndSendsNotification() {
        Pandit pandit = mock(Pandit.class);
        UserAccount account = mock(UserAccount.class);
        when(pandits.findById(4L)).thenReturn(Optional.of(pandit));
        when(pandits.save(pandit)).thenReturn(pandit);
        when(pandit.getUserId()).thenReturn(USER_ID);
        when(pandit.getId()).thenReturn(4L);
        when(pandit.getFullName()).thenReturn("Asha Mehta");
        when(pandit.getCity()).thenReturn("London");
        when(pandit.getRegion()).thenReturn("Greater London");
        when(pandit.getCountry()).thenReturn("United Kingdom");
        when(pandit.getReligion()).thenReturn("Hindu");
        when(users.findById(USER_ID)).thenReturn(Optional.of(account));
        when(account.getId()).thenReturn(USER_ID);

        controller.approvePandit(4L, true);

        verify(account).setRole(UserAccount.Role.PANDIT);
        verify(pandit).setApproved(true);
        verify(users).save(account);
        verify(notifications).create(USER_ID, "ACCOUNT", "Practitioner profile approved",
                "Your profile is approved. Sign in again to access your practitioner workspace.");
    }
}
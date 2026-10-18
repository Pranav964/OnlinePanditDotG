package com.onlinepandit.identity;

import com.onlinepandit.security.JwtService;
import com.onlinepandit.security.JwtAuthenticationFilter.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserAccountRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmail(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        UserAccount account = users.save(new UserAccount(request.fullName().trim(), email,
            passwordEncoder.encode(request.password()), request.phone(), request.city(), request.country(),
            request.dateOfBirth(), UserAccount.Role.USER));
        return response(account);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        UserAccount account = users.findByEmail(request.email().trim().toLowerCase())
                .filter(user -> passwordEncoder.matches(request.password(), user.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return response(account);
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return users.findById(principal.id()).map(AuthController::userResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private AuthResponse response(UserAccount user) {
        return new AuthResponse(jwtService.issue(user), "Bearer", userResponse(user));
    }

    private static UserResponse userResponse(UserAccount user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(), user.getCity(),
            user.getCountry(), user.getDateOfBirth(), user.getRole().name());
    }

    public record RegisterRequest(@NotBlank @Size(max = 120) String fullName, @Email @NotBlank String email,
                                  @Size(min = 10, max = 72) String password, String phone, String city, String country,
                                  java.time.LocalDate dateOfBirth) { }
    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) { }
    public record AuthResponse(String accessToken, String tokenType, UserResponse user) { }
    public record UserResponse(Long id, String fullName, String email, String phone, String city, String country,
                               java.time.LocalDate dateOfBirth, String role) { }
}

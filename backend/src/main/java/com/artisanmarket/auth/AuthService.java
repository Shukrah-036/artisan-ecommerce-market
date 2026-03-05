package com.artisanmarket.auth;

import com.artisanmarket.shared.exception.InvalidCredentialsException;
import com.artisanmarket.shared.exception.UserAlreadyExistsException;
import com.artisanmarket.shared.security.JwtService;
import com.artisanmarket.user.*;
import com.google.rpc.context.AttributeContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;


    @Transactional
    public AuthResponse registerUser (AuthRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("An account with that email already exists.");
        }

        Role role = "ARTISAN".equalsIgnoreCase(request.role()) ? Role.ARTISAN : Role.USER;
        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName());
        user.setRole(role);
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    public AuthResponse loginUser (AuthRequest request) {

        authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email()).orElseThrow();

        return buildAuthResponse(user);
    }

    public AuthResponse refresh (String refreshToken) {
        String email = jwtService.extractEmail(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new IllegalArgumentException("Refresh token is invalid or expired. Please log in again.");
        }

        User user = userRepository.findByEmail(email).orElseThrow();
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().name()
        );
    }
}

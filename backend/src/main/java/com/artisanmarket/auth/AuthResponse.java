package com.artisanmarket.auth;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String email,
        String displayName,
        String role
) {}

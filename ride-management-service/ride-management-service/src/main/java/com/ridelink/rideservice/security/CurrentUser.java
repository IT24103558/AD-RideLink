package com.ridelink.rideservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** The authenticated caller, read from the SecurityContext populated by {@link JwtAuthFilter}. */
public record CurrentUser(String userId, String role, String token) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public static CurrentUser current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user");
        }
        String role = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .findFirst().orElse("");
        Object credentials = auth.getCredentials();
        return new CurrentUser(String.valueOf(auth.getPrincipal()), role,
                credentials == null ? null : credentials.toString());
    }
}

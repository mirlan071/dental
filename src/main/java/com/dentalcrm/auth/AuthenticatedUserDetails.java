package com.dentalcrm.auth;

import com.dentalcrm.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class AuthenticatedUserDetails implements UserDetails, CredentialsContainer {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String username;
    private String password;
    private final boolean enabled;
    private final long authVersion;
    private final List<GrantedAuthority> authorities;

    public AuthenticatedUserDetails(User user) {
        username = user.getUsername();
        password = user.getPasswordHash();
        enabled = user.isActive();
        authVersion = user.getAuthVersion();
        authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    public long authVersion() {
        return authVersion;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void eraseCredentials() {
        password = null;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof AuthenticatedUserDetails details
                && username.equalsIgnoreCase(details.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username.toLowerCase(Locale.ROOT));
    }
}

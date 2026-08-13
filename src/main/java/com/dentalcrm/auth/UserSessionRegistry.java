package com.dentalcrm.auth;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class UserSessionRegistry {
    private final SessionRegistry sessions;

    public UserSessionRegistry(SessionRegistry sessions) {
        this.sessions = sessions;
    }

    public void register(HttpSession session, Authentication authentication) {
        sessions.registerNewSession(session.getId(), authentication.getPrincipal());
    }

    public void revokeAll(String username) {
        sessions.getAllPrincipals().stream()
                .filter(principal -> principal instanceof UserDetails details
                        && details.getUsername().equalsIgnoreCase(username))
                .flatMap(principal -> sessions.getAllSessions(principal, false).stream())
                .forEach(session -> session.expireNow());
    }
}

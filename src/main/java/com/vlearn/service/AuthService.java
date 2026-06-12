package com.vlearn.service;

import com.vlearn.entity.User;
import com.vlearn.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Session-based authentication. No Spring Security. Simple hash for passwords.
 */
@Service
public class AuthService {

    private static final String SESSION_USER_ID = "userId";
    private static final String SESSION_USER_ROLE = "userRole";
    private static final String HASH_SALT = "VLearn-Salt-2024";

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Simple custom hash (SHA-256 of salt + password). Not BCrypt. */
    public String hashPassword(String plainPassword) {
        if (plainPassword == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest((HASH_SALT + plainPassword).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public boolean checkPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) return false;
        return storedHash.equals(hashPassword(plainPassword));
    }

    public Optional<User> login(String username, String password) {
        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isEmpty()) return Optional.empty();
        User u = opt.get();
        if (!checkPassword(password, u.getPassword())) return Optional.empty();
        if (u.getRole() == User.Role.TEACHER && !u.isApproved()) return Optional.empty();
        return Optional.of(u);
    }

    public void setSessionUser(HttpSession session, User user) {
        if (session == null) return;
        session.setAttribute(SESSION_USER_ID, user.getId());
        session.setAttribute(SESSION_USER_ROLE, user.getRole().name());
    }

    public void clearSession(HttpSession session) {
        if (session != null) {
            session.removeAttribute(SESSION_USER_ID);
            session.removeAttribute(SESSION_USER_ROLE);
            session.invalidate();
        }
    }

    public Long getCurrentUserId(HttpSession session) {
        if (session == null) return null;
        Object id = session.getAttribute(SESSION_USER_ID);
        if (id instanceof Long) {
            return (Long) id;
        }
        if (id instanceof Integer) {
            return ((Integer) id).longValue();
        }
        if (id instanceof Number) {
            return ((Number) id).longValue();
        }
        return null;
    }

    public String getCurrentUserRole(HttpSession session) {
        if (session == null) return null;
        Object role = session.getAttribute(SESSION_USER_ROLE);
        return role instanceof String ? (String) role : null;
    }

    public Optional<User> getCurrentUser(HttpSession session) {
        Long id = getCurrentUserId(session);
        if (id == null) return Optional.empty();
        return userRepository.findById(id);
    }

    public boolean isLoggedIn(HttpSession session) {
        Long userId = getCurrentUserId(session);
        if (userId == null) return false;
        return userRepository.existsById(userId);
    }

    public boolean hasRole(HttpSession session, User.Role role) {
        String r = getCurrentUserRole(session);
        return r != null && r.equals(role.name());
    }
}

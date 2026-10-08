package com.vlearn.config;

import com.vlearn.entity.User;
import com.vlearn.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * Interceptor for role-based access. No Spring Security.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Set<String> PUBLIC_PATHS = Set.of(
        "/", "/login", "/register", "/logout", "/error",
        "/css/", "/js/", "/images/", "/videos/"
    );

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    private static boolean isPublic(String path) {
        if (path == null) return false;
        // Exact public routes
        if (path.equals("/") || path.equals("/login") || path.equals("/register")
                || path.equals("/logout") || path.startsWith("/error")) {
            return true;
        }
        // Public static assets
        return path.startsWith("/css/") || path.startsWith("/js/")
                || path.startsWith("/images/") || path.startsWith("/favicon.ico");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        // Lockdown H2 Console completely from unauthorized/external access
        if (path.startsWith("/h2-console")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access to database console is disabled for security.");
            return false;
        }

        // Allow public pages and assets
        if (isPublic(path)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null || !authService.isLoggedIn(session)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        String role = authService.getCurrentUserRole(session);

        // Strict role validation
        if (path.startsWith("/admin") && !User.Role.ADMIN.name().equals(role)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied: Administrator role required.");
            return false;
        }

        if (path.startsWith("/teacher")
                && !(User.Role.TEACHER.name().equals(role) || User.Role.ADMIN.name().equals(role))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied: Teacher role required.");
            return false;
        }

        if ((path.startsWith("/student") || path.startsWith("/test"))
                && !User.Role.STUDENT.name().equals(role)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied: Student role required.");
            return false;
        }

        if (path.startsWith("/video/watch") && !User.Role.STUDENT.name().equals(role)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied: Student role required.");
            return false;
        }

        return true;
    }
}

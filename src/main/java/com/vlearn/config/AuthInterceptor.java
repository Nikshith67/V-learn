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
        if (path == null) return true;
        for (String prefix : PUBLIC_PATHS) {
            if (path.startsWith(prefix)) return true;
        }
        return false;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (isPublic(request.getRequestURI())) return true;

        HttpSession session = request.getSession(false);
        if (session == null || !authService.isLoggedIn(session)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        String role = authService.getCurrentUserRole(session);
        String path = request.getRequestURI();

        if (path.startsWith("/admin") && !User.Role.ADMIN.name().equals(role)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin only");
            return false;
        }
        if (path.startsWith("/teacher")
            && !(User.Role.TEACHER.name().equals(role) || User.Role.ADMIN.name().equals(role))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Teacher only");
            return false;
        }
        if (path.startsWith("/student") && !User.Role.STUDENT.name().equals(role)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Student only");
            return false;
        }

        return true;
    }
}

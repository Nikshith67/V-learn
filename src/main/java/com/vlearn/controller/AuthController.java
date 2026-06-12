package com.vlearn.controller;

import com.vlearn.entity.User;
import com.vlearn.service.AuthService;
import com.vlearn.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @GetMapping("/")
    public String home(HttpSession session) {
        if (authService.isLoggedIn(session)) {
            String role = authService.getCurrentUserRole(session);
            if (User.Role.ADMIN.name().equals(role)) return "redirect:/admin";
            if (User.Role.TEACHER.name().equals(role)) return "redirect:/teacher";
            if (User.Role.STUDENT.name().equals(role)) return "redirect:/student";
        }
        return "auth/login";
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (authService.isLoggedIn(session)) return "redirect:/";
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        RedirectAttributes ra) {
        var user = authService.login(username, password);
        if (user.isEmpty()) {
            ra.addFlashAttribute("error", "Invalid username or password, or teacher not approved.");
            return "redirect:/login";
        }
        authService.setSessionUser(session, user.get());
        return "redirect:/";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        authService.clearSession(session);
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new User());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(User user,
                           @RequestParam String password,
                           @RequestParam String roleStr,
                           RedirectAttributes ra) {
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            ra.addFlashAttribute("error", "Username is required.");
            return "redirect:/register";
        }
        if (userService.usernameExists(user.getUsername())) {
            ra.addFlashAttribute("error", "Username already exists.");
            return "redirect:/register";
        }
        if (user.getEmail() != null && !user.getEmail().isBlank() && userService.emailExists(user.getEmail())) {
            ra.addFlashAttribute("error", "Email already registered.");
            return "redirect:/register";
        }
        User.Role role;
        try {
            role = User.Role.valueOf(roleStr);
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Invalid role.");
            return "redirect:/register";
        }
        if (role == User.Role.ADMIN) {
            ra.addFlashAttribute("error", "Cannot self-register as Admin.");
            return "redirect:/register";
        }
        if (role == User.Role.TEACHER) {
            if (user.getQualification() == null || user.getQualification().isBlank()) {
                ra.addFlashAttribute("error", "Qualification is required for teacher registration.");
                return "redirect:/register";
            }
            if (user.getCoreConcepts() == null || user.getCoreConcepts().isBlank()) {
                ra.addFlashAttribute("error", "Core concepts (subjects known) are required for teacher registration.");
                return "redirect:/register";
            }
            if (user.getExperienceYears() == null) {
                ra.addFlashAttribute("error", "Experience (years) is required for teacher registration.");
                return "redirect:/register";
            }
            if (user.getExperienceYears() < 0 || user.getExperienceYears() > 80) {
                ra.addFlashAttribute("error", "Experience must be between 0 and 80 years.");
                return "redirect:/register";
            }
        } else {
            user.setQualification(null);
            user.setCoreConcepts(null);
            user.setExperienceYears(null);
        }
        user.setRole(role);
        user.setFullName(user.getFullName() != null ? user.getFullName() : user.getUsername());
        userService.register(user, password);
        ra.addFlashAttribute("message", "Registration successful. " +
            (role == User.Role.TEACHER ? "Please wait for admin approval before logging in." : "You can log in now."));
        return "redirect:/";
    }
}

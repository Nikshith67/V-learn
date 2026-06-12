package com.vlearn.controller;

import com.vlearn.entity.TestResult;
import com.vlearn.service.AuthService;
import com.vlearn.service.TestResultService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/result")
public class ResultController {

    private final AuthService authService;
    private final TestResultService testResultService;

    public ResultController(AuthService authService, TestResultService testResultService) {
        this.authService = authService;
        this.testResultService = testResultService;
    }

    @GetMapping("/{id}")
    public String resultSheet(@PathVariable Long id, HttpSession session, Model model) {
        if (!authService.isLoggedIn(session)) {
            return "redirect:/login";
        }
        TestResult result = testResultService.findById(id).orElseThrow();
        if (!result.getStudent().getId().equals(authService.getCurrentUserId(session))) {
            return "redirect:/student";
        }
        double pct = result.getPercentage();
        String motivation = pct >= 80 ? "Excellent work!" : pct >= 50 ? "Good job! Keep it up!" : "Keep practicing!";
        model.addAttribute("user", authService.getCurrentUser(session).orElse(null));
        model.addAttribute("result", result);
        model.addAttribute("studentName", result.getStudent().getFullName());
        model.addAttribute("videoTitle", result.getVideo().getTitle());
        model.addAttribute("motivation", motivation);
        return "student/result";
    }
}

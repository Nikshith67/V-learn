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
        var currentOpt = authService.getCurrentUser(session);
        if (currentOpt.isEmpty()) {
            return "redirect:/login";
        }
        var current = currentOpt.get();

        var resultOpt = testResultService.findById(id);
        if (resultOpt.isEmpty()) {
            return "redirect:/student";
        }
        TestResult result = resultOpt.get();

        // Data isolation: Student only sees their own; Teacher only sees their video; Admin sees all
        boolean isOwnerStudent = current.getRole() == com.vlearn.entity.User.Role.STUDENT
                && result.getStudent() != null && current.getId().equals(result.getStudent().getId());
        boolean isAdmin = current.getRole() == com.vlearn.entity.User.Role.ADMIN;
        boolean isVideoTeacher = current.getRole() == com.vlearn.entity.User.Role.TEACHER
                && result.getVideo() != null && result.getVideo().getUploadedBy() != null
                && current.getId().equals(result.getVideo().getUploadedBy().getId());

        if (!isOwnerStudent && !isAdmin && !isVideoTeacher) {
            return "redirect:/student";
        }

        double pct = result.getPercentage();
        String motivation = pct >= 80 ? "Excellent work!" : pct >= 50 ? "Good job! Keep it up!" : "Keep practicing!";
        model.addAttribute("user", current);
        model.addAttribute("result", result);
        model.addAttribute("studentName", result.getStudent() != null ? result.getStudent().getFullName() : "Student");
        model.addAttribute("videoTitle", result.getVideo() != null ? result.getVideo().getTitle() : "Video Lesson");
        model.addAttribute("motivation", motivation);
        return "student/result";
    }
}

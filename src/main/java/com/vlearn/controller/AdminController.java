package com.vlearn.controller;

import com.vlearn.entity.User;
import com.vlearn.service.AuthService;
import com.vlearn.service.UserService;
import com.vlearn.service.TestResultService;
import com.vlearn.service.VideoService;
import com.vlearn.service.AdminDashboardService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AuthService authService;
    private final UserService userService;
    private final VideoService videoService;
    private final TestResultService testResultService;
    private final AdminDashboardService adminDashboardService;

    public AdminController(AuthService authService, UserService userService, VideoService videoService,
                           TestResultService testResultService, AdminDashboardService adminDashboardService) {
        this.authService = authService;
        this.userService = userService;
        this.videoService = videoService;
        this.testResultService = testResultService;
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping
    public String dashboard(HttpSession session, Model model) {
        User admin = authService.getCurrentUser(session).orElse(null);
        if (admin == null || admin.getRole() != User.Role.ADMIN) {
            return "redirect:/login";
        }
        List<User> pendingTeachers = userService.findTeachersPendingApproval();
        var videos = videoService.findAll();

        model.addAttribute("user", admin);
        model.addAttribute("pendingTeachers", pendingTeachers);
        model.addAttribute("videos", videos);
        model.addAttribute("stats", adminDashboardService.getDashboardStats());
        return "admin/dashboard";
    }

    @PostMapping("/approve-teacher")
    public String approveTeacher(@RequestParam Long teacherId, HttpSession session, RedirectAttributes ra) {
        User admin = authService.getCurrentUser(session).orElse(null);
        if (admin == null || admin.getRole() != User.Role.ADMIN) {
            return "redirect:/login";
        }
        boolean ok = userService.approveTeacher(teacherId);
        ra.addFlashAttribute("message", ok ? "Teacher approved." : "Could not approve this request. It may have been removed or already processed.");
        return "redirect:/admin";
    }

    @PostMapping("/reject-teacher")
    public String rejectTeacher(@RequestParam Long teacherId, HttpSession session, RedirectAttributes ra) {
        User admin = authService.getCurrentUser(session).orElse(null);
        if (admin == null || admin.getRole() != User.Role.ADMIN) {
            return "redirect:/login";
        }
        boolean ok = userService.rejectTeacher(teacherId);
        ra.addFlashAttribute("message", ok ? "Teacher registration rejected." : "Could not reject this request. It may have been removed or already processed.");
        return "redirect:/admin";
    }

    @PostMapping("/video/delete")
    public String deleteVideo(@RequestParam Long videoId, HttpSession session, RedirectAttributes ra) {
        User admin = authService.getCurrentUser(session).orElse(null);
        if (admin == null || admin.getRole() != User.Role.ADMIN) {
            return "redirect:/login";
        }
        boolean ok = videoService.deleteVideo(videoId, admin);
        ra.addFlashAttribute("message", ok ? "Video deleted." : "Failed to delete video.");
        return "redirect:/admin";
    }

    @GetMapping("/results")
    public String studentResults(HttpSession session, Model model) {
        User admin = authService.getCurrentUser(session).orElse(null);
        if (admin == null || admin.getRole() != User.Role.ADMIN) {
            return "redirect:/login";
        }
        var results = testResultService.findAll();
        model.addAttribute("user", admin);
        model.addAttribute("results", results);
        return "admin/results";
    }
}

package com.vlearn.controller;

import com.vlearn.entity.User;
import com.vlearn.service.AuthService;
import com.vlearn.service.TestResultService;
import com.vlearn.service.VideoProgressService;
import com.vlearn.service.VideoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final AuthService authService;
    private final VideoService videoService;
    private final TestResultService testResultService;
    private final VideoProgressService videoProgressService;

    public StudentController(AuthService authService, VideoService videoService,
                             TestResultService testResultService,
                             VideoProgressService videoProgressService) {
        this.authService = authService;
        this.videoService = videoService;
        this.testResultService = testResultService;
        this.videoProgressService = videoProgressService;
    }

    @GetMapping
    public String home(@RequestParam(required = false) String subject, HttpSession session, Model model) {
        User student = authService.getCurrentUser(session).orElseThrow();
        var videos = subject != null && !subject.isBlank()
            ? videoService.findBySubject(subject)
            : videoService.findAll();
        var subjects = videoService.findAllSubjects();
        model.addAttribute("user", student);
        model.addAttribute("videos", videos);
        model.addAttribute("subjects", subjects);
        return "student/home";
    }

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        User student = authService.getCurrentUser(session).orElseThrow();
        var results = testResultService.findByStudent(student);
        var completedVideos = videoProgressService.findCompletedVideosForProfile(student);
        var continueLearning = videoProgressService.findContinueLearningForProfile(student);
        model.addAttribute("user", student);
        model.addAttribute("results", results);
        model.addAttribute("completedVideos", completedVideos);
        model.addAttribute("continueLearning", continueLearning);
        return "student/profile";
    }
}

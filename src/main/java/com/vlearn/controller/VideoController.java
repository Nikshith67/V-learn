package com.vlearn.controller;

import com.vlearn.entity.User;
import com.vlearn.entity.Video;
import com.vlearn.service.AuthService;
import com.vlearn.service.VideoProgressService;
import com.vlearn.service.VideoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/video")
public class VideoController {

    private final AuthService authService;
    private final VideoService videoService;
    private final VideoProgressService videoProgressService;

    public VideoController(AuthService authService, VideoService videoService, VideoProgressService videoProgressService) {
        this.authService = authService;
        this.videoService = videoService;
        this.videoProgressService = videoProgressService;
    }

    @GetMapping("/watch/{id}")
    public String watch(@PathVariable Long id, HttpSession session, Model model) {
        User student = authService.getCurrentUser(session).orElse(null);
        if (student == null || student.getRole() != User.Role.STUDENT) {
            return "redirect:/login";
        }
        var videoOpt = videoService.findByIdWithUploader(id);
        if (videoOpt.isEmpty()) {
            return "redirect:/student";
        }
        Video video = videoOpt.get();
        boolean canTakeTest = videoProgressService.canTakeTest(student, video);
        double resumePercent = videoProgressService.getResumePercent(student, video);
        User teacher = video.getUploadedBy();
        String teacherDisplayName = teacher == null ? "—"
            : (teacher.getFullName() != null && !teacher.getFullName().isBlank()
                ? teacher.getFullName() : teacher.getUsername());
        model.addAttribute("user", student);
        model.addAttribute("video", video);
        model.addAttribute("canTakeTest", canTakeTest);
        model.addAttribute("resumePercent", resumePercent);
        model.addAttribute("teacherDisplayName", teacherDisplayName);
        return "student/video-watch";
    }

    /**
     * Called by JavaScript when video progress reaches 80% to persist and enable "Take Test".
     */
    @PostMapping("/progress")
    @ResponseBody
    public ResponseEntity<Void> updateProgress(@RequestParam Long videoId,
                                                @RequestParam double progressPercent,
                                                HttpSession session) {
        User student = authService.getCurrentUser(session).orElse(null);
        if (student == null || student.getRole() != User.Role.STUDENT) {
            return ResponseEntity.status(403).build();
        }
        Video video = videoService.findById(videoId).orElse(null);
        if (video == null) return ResponseEntity.notFound().build();
        double cleanProgress = Math.max(0.0, Math.min(100.0, progressPercent));
        videoProgressService.updateProgress(student, video, cleanProgress);
        return ResponseEntity.ok().build();
    }
}

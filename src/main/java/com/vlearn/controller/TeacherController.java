package com.vlearn.controller;

import com.vlearn.entity.Question;
import com.vlearn.entity.User;
import com.vlearn.entity.Video;
import com.vlearn.service.AuthService;
import com.vlearn.service.QuestionService;
import com.vlearn.service.VideoService;
import com.vlearn.service.TestResultService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/teacher")
public class TeacherController {

    private final AuthService authService;
    private final VideoService videoService;
    private final QuestionService questionService;
    private final TestResultService testResultService;

    public TeacherController(AuthService authService, VideoService videoService,
                             QuestionService questionService, TestResultService testResultService) {
        this.authService = authService;
        this.videoService = videoService;
        this.questionService = questionService;
        this.testResultService = testResultService;
    }

    @GetMapping
    public String dashboard(HttpSession session, Model model) {
        User teacher = authService.getCurrentUser(session).orElseThrow();
        List<Video> myVideos = videoService.findByUploadedBy(teacher);
        model.addAttribute("user", teacher);
        model.addAttribute("videos", myVideos);
        return "teacher/dashboard";
    }

    @GetMapping("/video/upload")
    public String uploadForm(HttpSession session, Model model) {
        model.addAttribute("user", authService.getCurrentUser(session).orElseThrow());
        model.addAttribute("video", new Video());
        return "teacher/video-upload";
    }

    @PostMapping("/video/upload")
    public String uploadVideo(@ModelAttribute Video video,
                              @RequestParam("file") MultipartFile file,
                              HttpSession session,
                              RedirectAttributes ra) {
        User teacher = authService.getCurrentUser(session).orElseThrow();
        if (file.isEmpty()) {
            ra.addFlashAttribute("error", "Please select a video file.");
            return "redirect:/teacher/video/upload";
        }
        try {
            videoService.save(video, teacher, file);
            ra.addFlashAttribute("message", "Video uploaded successfully.");
        } catch (IllegalArgumentException | SecurityException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/teacher/video/upload";
        } catch (IOException e) {
            ra.addFlashAttribute("error", "Failed to save file: " + e.getMessage());
        }
        return "redirect:/teacher";
    }

    @GetMapping("/video/{id}/questions")
    public String questionsList(@PathVariable Long id, HttpSession session, Model model) {
        User teacher = authService.getCurrentUser(session).orElseThrow();
        Video video = videoService.findById(id).orElseThrow();
        if (!video.getUploadedBy().getId().equals(teacher.getId())) {
            return "redirect:/teacher";
        }
        List<Question> questions = questionService.findByVideoId(id);
        model.addAttribute("user", teacher);
        model.addAttribute("video", video);
        model.addAttribute("questions", questions);
        return "teacher/questions";
    }

    @GetMapping("/video/{id}/questions/add")
    public String addQuestionForm(@PathVariable Long id, HttpSession session, Model model) {
        User teacher = authService.getCurrentUser(session).orElseThrow();
        Video video = videoService.findById(id).orElseThrow();
        if (!video.getUploadedBy().getId().equals(teacher.getId())) {
            return "redirect:/teacher";
        }
        model.addAttribute("user", teacher);
        model.addAttribute("video", video);
        model.addAttribute("question", new Question());
        return "teacher/question-form";
    }

    @PostMapping("/video/{id}/questions/add")
    public String addQuestion(@PathVariable Long id,
                              @RequestParam String questionText,
                              @RequestParam String option1,
                              @RequestParam String option2,
                              @RequestParam String option3,
                              @RequestParam String option4,
                              @RequestParam int correctOption,
                              HttpSession session,
                              RedirectAttributes ra) {
        User teacher = authService.getCurrentUser(session).orElseThrow();
        Video video = videoService.findById(id).orElseThrow();
        if (!video.getUploadedBy().getId().equals(teacher.getId())) {
            return "redirect:/teacher";
        }
        Question q = new Question();
        q.setVideo(video);
        q.setQuestionText(questionText);
        q.setOption1(option1);
        q.setOption2(option2);
        q.setOption3(option3);
        q.setOption4(option4);
        q.setCorrectOption(Math.max(1, Math.min(4, correctOption)));
        questionService.save(q);
        ra.addFlashAttribute("message", "Question added.");
        return "redirect:/teacher/video/" + id + "/questions";
    }

    @PostMapping("/video/{id}/delete")
    public String deleteVideo(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        User teacher = authService.getCurrentUser(session).orElseThrow();
        boolean ok = videoService.deleteVideo(id, teacher);
        if (ok) {
            ra.addFlashAttribute("message", "Video deleted.");
        } else {
            ra.addFlashAttribute("error", "You are not allowed to delete this video.");
        }
        return "redirect:/teacher";
    }

    @GetMapping("/video/{id}/edit")
    public String editVideoForm(@PathVariable Long id, HttpSession session, Model model) {
        User teacher = authService.getCurrentUser(session).orElseThrow();
        Video video = videoService.findById(id).orElseThrow();
        if (!video.getUploadedBy().getId().equals(teacher.getId())) {
            return "redirect:/teacher";
        }
        model.addAttribute("user", teacher);
        model.addAttribute("video", video);
        return "teacher/video-edit";
    }

    @PostMapping("/video/{id}/edit")
    public String editVideo(@PathVariable Long id,
                            @RequestParam String title,
                            @RequestParam String subject,
                            @RequestParam(required = false) String description,
                            @RequestParam(value = "file", required = false) MultipartFile file,
                            HttpSession session,
                            RedirectAttributes ra) {
        User teacher = authService.getCurrentUser(session).orElseThrow();
        try {
            boolean ok = videoService.updateVideo(id, teacher, title, subject,
                description != null ? description : "", file);
            if (ok) {
                ra.addFlashAttribute("message", "Video updated.");
            } else {
                ra.addFlashAttribute("error", "You cannot edit this video.");
            }
        } catch (IllegalArgumentException | SecurityException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/teacher/video/" + id + "/edit";
        } catch (IOException e) {
            ra.addFlashAttribute("error", "Failed to update file: " + e.getMessage());
        }
        return "redirect:/teacher";
    }

    @PostMapping("/video/{videoId}/questions/{questionId}/delete")
    public String deleteQuestion(@PathVariable Long videoId,
                                 @PathVariable Long questionId,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        User teacher = authService.getCurrentUser(session).orElseThrow();
        Question q = questionService.findById(questionId).orElse(null);
        if (q == null || q.getVideo() == null || !q.getVideo().getId().equals(videoId)
            || !q.getVideo().getUploadedBy().getId().equals(teacher.getId())) {
            ra.addFlashAttribute("error", "You cannot delete this question.");
            return "redirect:/teacher";
        }
        questionService.delete(questionId);
        ra.addFlashAttribute("message", "Question deleted.");
        return "redirect:/teacher/video/" + videoId + "/questions";
    }

    @GetMapping("/video/{videoId}/questions/{questionId}/edit")
    public String editQuestionForm(@PathVariable Long videoId,
                                   @PathVariable Long questionId,
                                   HttpSession session,
                                   Model model) {
        User teacher = authService.getCurrentUser(session).orElseThrow();
        Question q = questionService.findById(questionId).orElseThrow();
        if (!q.getVideo().getId().equals(videoId)
            || !q.getVideo().getUploadedBy().getId().equals(teacher.getId())) {
            return "redirect:/teacher";
        }
        model.addAttribute("user", teacher);
        model.addAttribute("video", q.getVideo());
        model.addAttribute("question", q);
        return "teacher/question-edit";
    }

    @PostMapping("/video/{videoId}/questions/{questionId}/edit")
    public String editQuestion(@PathVariable Long videoId,
                               @PathVariable Long questionId,
                               @RequestParam String questionText,
                               @RequestParam String option1,
                               @RequestParam String option2,
                               @RequestParam String option3,
                               @RequestParam String option4,
                               @RequestParam int correctOption,
                               HttpSession session,
                               RedirectAttributes ra) {
        User teacher = authService.getCurrentUser(session).orElseThrow();
        Question q = questionService.findById(questionId).orElse(null);
        if (q == null || !q.getVideo().getId().equals(videoId)
            || !q.getVideo().getUploadedBy().getId().equals(teacher.getId())) {
            ra.addFlashAttribute("error", "You cannot edit this question.");
            return "redirect:/teacher";
        }
        q.setQuestionText(questionText);
        q.setOption1(option1);
        q.setOption2(option2);
        q.setOption3(option3);
        q.setOption4(option4);
        q.setCorrectOption(Math.max(1, Math.min(4, correctOption)));
        questionService.save(q);
        ra.addFlashAttribute("message", "Question updated.");
        return "redirect:/teacher/video/" + videoId + "/questions";
    }

    @GetMapping("/results")
    public String results(HttpSession session, Model model) {
        User teacher = authService.getCurrentUser(session).orElseThrow();
        var myVideos = videoService.findByUploadedBy(teacher);
        var allResults = testResultService.findAll();
        var results = allResults.stream()
            .filter(r -> myVideos.stream().anyMatch(v -> v.getId().equals(r.getVideo().getId())))
            .sorted((a, b) -> b.getAttemptedAt().compareTo(a.getAttemptedAt()))
            .toList();
        model.addAttribute("user", teacher);
        model.addAttribute("results", results);
        return "teacher/results";
    }
}

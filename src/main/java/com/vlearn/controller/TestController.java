package com.vlearn.controller;

import com.vlearn.entity.Question;
import com.vlearn.entity.TestResult;
import com.vlearn.entity.User;
import com.vlearn.entity.Video;
import com.vlearn.service.AuthService;
import com.vlearn.service.QuestionService;
import com.vlearn.service.TestResultService;
import com.vlearn.service.VideoProgressService;
import com.vlearn.service.VideoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/test")
public class TestController {

    private static final int SECONDS_PER_QUESTION = 25;

    private final AuthService authService;
    private final VideoService videoService;
    private final QuestionService questionService;
    private final TestResultService testResultService;
    private final VideoProgressService videoProgressService;

    public TestController(AuthService authService, VideoService videoService,
                          QuestionService questionService, TestResultService testResultService,
                          VideoProgressService videoProgressService) {
        this.authService = authService;
        this.videoService = videoService;
        this.questionService = questionService;
        this.testResultService = testResultService;
        this.videoProgressService = videoProgressService;
    }

    @GetMapping("/{videoId}")
    public String takeTest(@PathVariable Long videoId, HttpSession session, Model model) {
        User student = authService.getCurrentUser(session).orElseThrow();
        Video video = videoService.findById(videoId).orElseThrow();
        if (!videoProgressService.canTakeTest(student, video)) {
            return "redirect:/video/watch/" + videoId + "?error=Watch 80% of the video first";
        }
        List<Question> questions = questionService.findByVideoId(videoId);
        if (questions.isEmpty()) {
            return "redirect:/video/watch/" + videoId + "?error=No questions for this video yet";
        }
        model.addAttribute("user", student);
        model.addAttribute("video", video);
        model.addAttribute("questions", questions);
        model.addAttribute("secondsPerQuestion", SECONDS_PER_QUESTION);
        return "student/test";
    }

    @PostMapping("/submit/{videoId}")
    public String submitTest(@PathVariable Long videoId,
                             @RequestParam Map<String, String> allParams,
                             HttpSession session,
                             RedirectAttributes ra) {
        User student = authService.getCurrentUser(session).orElseThrow();
        Video video = videoService.findById(videoId).orElseThrow();
        List<Question> questions = questionService.findByVideoId(videoId);
        int correct = 0;
        for (Question q : questions) {
            String key = "q_" + q.getId();
            String submitted = allParams.get(key);
            int submittedOption = 0;
            if (submitted != null && !submitted.isBlank()) {
                try {
                    submittedOption = Integer.parseInt(submitted.trim());
                } catch (NumberFormatException ignored) {}
            }
            if (submittedOption == q.getCorrectOption()) correct++;
        }
        int wrong = questions.size() - correct;
        TestResult result = new TestResult();
        result.setStudent(student);
        result.setVideo(video);
        result.setTotalQuestions(questions.size());
        result.setCorrectAnswers(correct);
        result.setWrongAnswers(wrong);
        TestResult saved = testResultService.save(result);
        return "redirect:/result/" + saved.getId();
    }
}

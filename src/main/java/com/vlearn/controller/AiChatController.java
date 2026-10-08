package com.vlearn.controller;

import com.vlearn.dto.ChatMessage;
import com.vlearn.entity.User;
import com.vlearn.entity.Video;
import com.vlearn.service.AiService;
import com.vlearn.service.AuthService;
import com.vlearn.service.VideoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for the AI assistant feature.
 * Provides both the Thymeleaf page and REST endpoints for chat/summarize.
 */
@Controller
@RequestMapping("/student/ai")
public class AiChatController {

    private final AuthService authService;
    private final VideoService videoService;
    private final AiService aiService;
    private final com.vlearn.service.RateLimiterService rateLimiterService;

    public AiChatController(AuthService authService, VideoService videoService, AiService aiService,
                            com.vlearn.service.RateLimiterService rateLimiterService) {
        this.authService = authService;
        this.videoService = videoService;
        this.aiService = aiService;
        this.rateLimiterService = rateLimiterService;
    }

    /**
     * Render the AI assistant page.
     * Optional videoId query param to pre-load video context.
     */
    @GetMapping
    public String aiAssistantPage(@RequestParam(required = false) Long videoId,
                                  HttpSession session, Model model) {
        User student = authService.getCurrentUser(session).orElseThrow();
        if (student.getRole() != User.Role.STUDENT) {
            return "redirect:/";
        }
        model.addAttribute("user", student);

        // Load all videos for the dropdown picker
        var allVideos = videoService.findAll();
        model.addAttribute("allVideos", allVideos);

        // If a specific video was requested, load it
        if (videoId != null) {
            videoService.findByIdWithUploader(videoId).ifPresent(video -> {
                model.addAttribute("selectedVideo", video);
            });
        }
        model.addAttribute("selectedVideoId", videoId);

        return "student/ai-assistant";
    }

    /**
     * Generate a summary for a specific video.
     */
    @PostMapping("/summarize")
    @ResponseBody
    public ResponseEntity<Map<String, String>> summarize(@RequestBody Map<String, Long> body,
                                                          HttpSession session) {
        User student = authService.getCurrentUser(session).orElse(null);
        if (student == null || student.getRole() != User.Role.STUDENT) {
            return ResponseEntity.status(403).body(Map.of("error", "Unauthorized"));
        }

        // Per-user rate limiting / cooldown check
        String limitMsg = rateLimiterService.checkAiRateLimit(student.getId());
        if (limitMsg != null) {
            return ResponseEntity.ok(Map.of("response", limitMsg));
        }

        Long videoId = body.get("videoId");
        if (videoId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "videoId is required"));
        }

        Video video = videoService.findByIdWithUploader(videoId).orElse(null);
        if (video == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Video not found"));
        }

        String summary = aiService.generateVideoSummary(video);
        return ResponseEntity.ok(Map.of("response", summary));
    }

    /**
     * Handle a chat message from the student.
     * Accepts: { message: String, videoId: Long (optional), history: [{role, content}] }
     */
    @PostMapping("/chat")
    @ResponseBody
    public ResponseEntity<Map<String, String>> chat(@RequestBody ChatRequest request,
                                                     HttpSession session) {
        User student = authService.getCurrentUser(session).orElse(null);
        if (student == null || student.getRole() != User.Role.STUDENT) {
            return ResponseEntity.status(403).body(Map.of("error", "Unauthorized"));
        }

        // Per-user rate limiting / cooldown check
        String limitMsg = rateLimiterService.checkAiRateLimit(student.getId());
        if (limitMsg != null) {
            return ResponseEntity.ok(Map.of("response", limitMsg));
        }

        if (request.message == null || request.message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty"));
        }

        // Sanitize length: max 1200 characters per user prompt to avoid lagging API
        String cleanMessage = request.message.trim();
        if (cleanMessage.length() > 1200) {
            cleanMessage = cleanMessage.substring(0, 1200);
        }

        Video video = null;
        if (request.videoId != null) {
            video = videoService.findByIdWithUploader(request.videoId).orElse(null);
        }

        List<ChatMessage> history = request.history != null ? request.history : List.of();
        String response = aiService.chat(cleanMessage, video, history);
        return ResponseEntity.ok(Map.of("response", response));
    }

    /**
     * Request DTO for the chat endpoint.
     */
    public static class ChatRequest {
        public String message;
        public Long videoId;
        public List<ChatMessage> history;

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Long getVideoId() { return videoId; }
        public void setVideoId(Long videoId) { this.videoId = videoId; }
        public List<ChatMessage> getHistory() { return history; }
        public void setHistory(List<ChatMessage> history) { this.history = history; }
    }
}

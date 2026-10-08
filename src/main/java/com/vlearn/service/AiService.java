package com.vlearn.service;

import com.vlearn.dto.ChatMessage;
import com.vlearn.entity.Question;
import com.vlearn.entity.Video;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;

/**
 * Service that communicates with the NVIDIA API (OpenAI-compatible)
 * to provide video summaries and an AI chatbot for students.
 */
@Service
public class AiService {

    private final WebClient webClient;
    private final String apiKey;
    private final String model;
    private final String fallbackModel;
    private final String endpoint;
    private final int maxTokens;

    public AiService(@Value("${vlearn.ai.api-key:${vlearn.nvidia.api-key:}}") String apiKey,
                     @Value("${vlearn.ai.model:${vlearn.nvidia.model:gemini-3.5-flash}}") String model,
                     @Value("${vlearn.ai.fallback-model:gemini-3.5-flash-lite}") String fallbackModel,
                     @Value("${vlearn.ai.endpoint:https://generativelanguage.googleapis.com/v1beta/openai/chat/completions}") String endpoint,
                     @Value("${vlearn.ai.max-tokens:${vlearn.nvidia.max-tokens:4096}}") int maxTokens) {
        String cleaned = apiKey != null ? apiKey.trim() : "";
        while ((cleaned.startsWith("\"") && cleaned.endsWith("\"")) || (cleaned.startsWith("'") && cleaned.endsWith("'"))) {
            if (cleaned.length() <= 1) break;
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }
        if (cleaned.toLowerCase().startsWith("bearer ")) {
            cleaned = cleaned.substring(7).trim();
        }
        this.apiKey = cleaned;
        this.model = model;
        this.fallbackModel = fallbackModel;
        this.endpoint = endpoint;
        this.maxTokens = maxTokens;
        this.webClient = WebClient.builder()
            .defaultHeader("Authorization", "Bearer " + this.apiKey)
            .build();
    }

    /**
     * Build a context string from a Video entity (title, subject, description, questions).
     */
    private String buildVideoContext(Video video) {
        StringBuilder ctx = new StringBuilder();
        ctx.append("Video Title: ").append(video.getTitle()).append("\n");
        ctx.append("Subject: ").append(video.getSubject()).append("\n");
        if (video.getDescription() != null && !video.getDescription().isBlank()) {
            ctx.append("Description: ").append(video.getDescription()).append("\n");
        }
        List<Question> questions = video.getQuestions();
        if (questions != null && !questions.isEmpty()) {
            ctx.append("\nAssociated MCQ Questions:\n");
            for (int i = 0; i < questions.size(); i++) {
                Question q = questions.get(i);
                ctx.append("  Q").append(i + 1).append(": ").append(q.getQuestionText()).append("\n");
                ctx.append("    A) ").append(q.getOption1()).append("\n");
                ctx.append("    B) ").append(q.getOption2()).append("\n");
                ctx.append("    C) ").append(q.getOption3()).append("\n");
                ctx.append("    D) ").append(q.getOption4()).append("\n");
            }
        }
        return ctx.toString();
    }

    /**
     * Generate a concise summary of the video content.
     */
    public String generateVideoSummary(Video video) {
        String systemPrompt = "You are an educational AI assistant for V-Learn, an e-learning platform. "
            + "Generate a clear, well-structured summary of the following video lesson. "
            + "Include the key topics covered, important concepts, and what students will learn. "
            + "Keep it concise (3-5 paragraphs). Use markdown formatting for readability.";
        String userMessage = "Please summarize this video lesson:\n\n" + buildVideoContext(video);
        return callAi(systemPrompt, userMessage, List.of());
    }

    /**
     * Chat with the AI using optional video context and conversation history.
     */
    public String chat(String userMessage, Video video, List<ChatMessage> history) {
        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("You are an intelligent educational AI assistant for V-Learn, an e-learning platform. ");
        systemPrompt.append("You help students understand video lessons, answer their questions about the content, ");
        systemPrompt.append("and assist with general subject-related doubts. ");
        systemPrompt.append("Be encouraging, clear, and thorough in your explanations. ");
        systemPrompt.append("Use markdown formatting (bold, lists, headings) for readability. ");
        systemPrompt.append("If a student asks about something not related to education, ");
        systemPrompt.append("politely redirect them to educational topics.\n\n");

        if (video != null) {
            systemPrompt.append("The student is currently studying the following video lesson. ");
            systemPrompt.append("Use this context to provide relevant and specific answers:\n\n");
            systemPrompt.append(buildVideoContext(video));
        }

        return callAi(systemPrompt.toString(), userMessage, history);
    }

    /**
     * Call the OpenAI-compatible AI API endpoint (Google Gemini, Groq, NVIDIA NIM, OpenAI).
     */
    private String callAi(String systemInstruction, String userMessage, List<ChatMessage> history) {
        if (apiKey == null || apiKey.isBlank()) {
            return "⚠️ AI features are not configured. Please add a valid API key in application-local.properties or set the AI_API_KEY environment variable.";
        }

        try {
            // Build messages array (OpenAI format)
            List<Map<String, String>> messages = new ArrayList<>();

            // System message
            messages.add(Map.of("role", "system", "content", systemInstruction));

            // Conversation history
            if (history != null) {
                for (ChatMessage msg : history) {
                    String role = "user".equals(msg.getRole()) ? "user" : "assistant";
                    messages.add(Map.of("role", role, "content", msg.getContent()));
                }
            }

            // Current user message
            messages.add(Map.of("role", "user", "content", userMessage));

            // Build request body
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", 0.7);
            requestBody.put("stream", false);

            // Make the API call with automatic fallback if the model is busy
            Map<?, ?> response = executeWithFallback(requestBody);

            // Parse the response
            return extractTextFromResponse(response);

        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                return "⏳ The AI is currently busy. Please try again in a moment (rate limit reached).";
            } else if (e.getStatusCode().value() == 503) {
                return "⏳ The AI service is currently experiencing high demand. Please try again in a few seconds.";
            } else if (e.getStatusCode().value() == 401) {
                return "🔒 The API key is invalid. Please check your AI API key in configuration.";
            } else if (e.getStatusCode().value() == 400) {
                return "⚠️ There was an issue with the request (" + e.getResponseBodyAsString() + ").";
            }
            return "❌ AI service error (" + e.getStatusCode().value() + "): " + e.getResponseBodyAsString();
        } catch (Exception e) {
            return "❌ Could not reach the AI service. Please check your internet connection and try again.";
        }
    }

    /**
     * Executes the chat completion request, automatically switching to the fallback model
     * if the primary model encounters high demand (503) or rate limits (429).
     */
    private Map<?, ?> executeWithFallback(Map<String, Object> requestBody) {
        try {
            return executePost(requestBody);
        } catch (WebClientResponseException e) {
            if ((e.getStatusCode().value() == 503 || e.getStatusCode().value() == 429)
                    && fallbackModel != null && !fallbackModel.isBlank()
                    && !fallbackModel.equalsIgnoreCase((String) requestBody.get("model"))) {
                Map<String, Object> fallbackBody = new LinkedHashMap<>(requestBody);
                fallbackBody.put("model", fallbackModel);
                return executePost(fallbackBody);
            }
            throw e;
        }
    }

    private Map<?, ?> executePost(Map<String, Object> body) {
        return webClient.post()
            .uri(endpoint)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(Map.class)
            .block();
    }

    /**
     * Extract the text content from an OpenAI-compatible API response.
     */
    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<?, ?> response) {
        if (response == null) {
            return "No response received from AI.";
        }
        try {
            List<?> choices = (List<?>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                return "The AI could not generate a response. Please try again.";
            }
            Map<?, ?> choice = (Map<?, ?>) choices.get(0);
            Map<?, ?> message = (Map<?, ?>) choice.get("message");
            String content = (String) message.get("content");
            return content != null ? content : "The AI returned an empty response.";
        } catch (Exception e) {
            return "Could not parse AI response.";
        }
    }
}

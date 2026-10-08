package com.vlearn.dto;

/**
 * Simple DTO for a chat message exchanged between user and AI assistant.
 */
public class ChatMessage {

    private String role;    // "user" or "assistant"
    private String content; // The message text

    public ChatMessage() {}

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}

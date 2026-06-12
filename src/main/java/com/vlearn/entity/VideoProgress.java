package com.vlearn.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Tracks how much of a video a student has watched.
 * Used to enable "Take Test" when progress >= 80%.
 */
@Entity
@Table(name = "video_progress", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"student_id", "video_id"})
})
public class VideoProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    /**
     * Maximum fraction of the video ever reached (0–100). Used to unlock the test at 80%.
     */
    @Column(name = "progress_percent", nullable = false)
    private double progressPercent = 0;

    /**
     * Last playback position as a percentage (0–100). Used to resume where the student left off.
     */
    @Column(name = "last_watched_percent", nullable = false)
    private double lastWatchedPercent = 0;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public VideoProgress() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }
    public Video getVideo() { return video; }
    public void setVideo(Video video) { this.video = video; }
    public double getProgressPercent() { return progressPercent; }
    public void setProgressPercent(double progressPercent) { this.progressPercent = progressPercent; }
    public double getLastWatchedPercent() { return lastWatchedPercent; }
    public void setLastWatchedPercent(double lastWatchedPercent) { this.lastWatchedPercent = lastWatchedPercent; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

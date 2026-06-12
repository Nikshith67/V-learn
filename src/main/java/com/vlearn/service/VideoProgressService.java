package com.vlearn.service;

import com.vlearn.entity.User;
import com.vlearn.entity.Video;
import com.vlearn.entity.VideoProgress;
import com.vlearn.repository.VideoProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VideoProgressService {

    private static final double MIN_PROGRESS_TO_TAKE_TEST = 80.0;

    private final VideoProgressRepository videoProgressRepository;

    public VideoProgressService(VideoProgressRepository videoProgressRepository) {
        this.videoProgressRepository = videoProgressRepository;
    }

    /**
     * Persists playback position: {@code currentPercent} is the latest position (resume);
     * {@code progressPercent} stores the maximum reached (test unlock at 80%).
     */
    @Transactional
    public VideoProgress updateProgress(User student, Video video, double currentPercent) {
        Optional<VideoProgress> opt = videoProgressRepository.findByStudentAndVideo(student, video);
        VideoProgress vp = opt.orElseGet(() -> {
            VideoProgress newVp = new VideoProgress();
            newVp.setStudent(student);
            newVp.setVideo(video);
            return newVp;
        });
        double clamped = Math.max(0, Math.min(100, currentPercent));
        vp.setLastWatchedPercent(clamped);
        if (clamped > vp.getProgressPercent()) {
            vp.setProgressPercent(clamped);
        }
        vp.setUpdatedAt(java.time.LocalDateTime.now());
        return videoProgressRepository.save(vp);
    }

    public Optional<VideoProgress> getProgress(User student, Video video) {
        return videoProgressRepository.findByStudentAndVideo(student, video);
    }

    public boolean canTakeTest(User student, Video video) {
        return videoProgressRepository.findByStudentAndVideo(student, video)
            .map(vp -> vp.getProgressPercent() >= MIN_PROGRESS_TO_TAKE_TEST)
            .orElse(false);
    }

    /** Percentage (0–100) to seek to when reopening the video. */
    public double getResumePercent(User student, Video video) {
        return videoProgressRepository.findByStudentAndVideo(student, video)
            .map(VideoProgress::getLastWatchedPercent)
            .orElse(0.0);
    }

    /** Videos where the student reached at least 80% watch (test unlocked). */
    @Transactional(readOnly = true)
    public List<VideoProgress> findCompletedVideosForProfile(User student) {
        return videoProgressRepository.findAllByStudentIdWithVideo(student.getId()).stream()
            .filter(vp -> vp.getProgressPercent() >= MIN_PROGRESS_TO_TAKE_TEST)
            .collect(Collectors.toList());
    }

    /**
     * Videos the student started but has not yet reached 80% maximum watch.
     * Named for the UI: “Continue learning”.
     */
    @Transactional(readOnly = true)
    public List<VideoProgress> findContinueLearningForProfile(User student) {
        return videoProgressRepository.findAllByStudentIdWithVideo(student.getId()).stream()
            .filter(vp -> vp.getProgressPercent() < MIN_PROGRESS_TO_TAKE_TEST
                && (vp.getLastWatchedPercent() > 0 || vp.getProgressPercent() > 0))
            .collect(Collectors.toList());
    }
}

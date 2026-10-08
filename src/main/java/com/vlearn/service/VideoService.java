package com.vlearn.service;

import com.vlearn.entity.User;
import com.vlearn.entity.Video;
import com.vlearn.repository.QuestionRepository;
import com.vlearn.repository.TestResultRepository;
import com.vlearn.repository.VideoProgressRepository;
import com.vlearn.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class VideoService {

    private final VideoRepository videoRepository;
    private final QuestionRepository questionRepository;
    private final TestResultRepository testResultRepository;
    private final VideoProgressRepository videoProgressRepository;
    private final Path uploadRoot;

    public VideoService(VideoRepository videoRepository,
                        QuestionRepository questionRepository,
                        TestResultRepository testResultRepository,
                        VideoProgressRepository videoProgressRepository,
                        @Value("${vlearn.videos.upload-dir:./vlearn-uploads/videos}") String uploadDir) {
        this.videoRepository = videoRepository;
        this.questionRepository = questionRepository;
        this.testResultRepository = testResultRepository;
        this.videoProgressRepository = videoProgressRepository;
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    private static final java.util.Set<String> ALLOWED_EXTENSIONS = java.util.Set.of(".mp4", ".webm", ".mkv", ".mov");

    private String validateAndGetExtension(MultipartFile file) {
        String orig = file.getOriginalFilename();
        if (orig == null || !orig.contains(".")) {
            throw new IllegalArgumentException("File must have a valid video extension (.mp4, .webm, .mkv, .mov)");
        }
        String ext = orig.substring(orig.lastIndexOf('.')).toLowerCase().trim();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Invalid file type: " + ext + ". Only video files (.mp4, .webm, .mkv, .mov) are allowed.");
        }
        return ext;
    }

    @Transactional
    public Video save(Video video, User uploadedBy, MultipartFile file) throws IOException {
        video.setUploadedBy(uploadedBy);
        if (file != null && !file.isEmpty()) {
            Files.createDirectories(uploadRoot);
            String ext = validateAndGetExtension(file);
            String filename = UUID.randomUUID() + ext;
            Path target = uploadRoot.resolve(filename).normalize();
            if (!target.startsWith(uploadRoot)) {
                throw new SecurityException("Invalid file path detected");
            }
            file.transferTo(target.toFile());
            video.setFilePath(filename);
        }
        return videoRepository.save(video);
    }

    public Optional<Video> findById(Long id) {
        return videoRepository.findById(id);
    }

    /**
     * Loads a video and touches {@code uploadedBy} inside a transaction so Thymeleaf can read teacher fields.
     */
    @Transactional(readOnly = true)
    public Optional<Video> findByIdWithUploader(Long id) {
        return videoRepository.findById(id).map(v -> {
            User u = v.getUploadedBy();
            if (u != null) {
                u.getUsername();
                u.getFullName();
            }
            return v;
        });
    }

    @Transactional
    public boolean updateVideo(Long videoId, User teacher, String title, String subject, String description,
                               MultipartFile file) throws IOException {
        Video video = videoRepository.findById(videoId).orElse(null);
        if (video == null || video.getUploadedBy() == null
            || !video.getUploadedBy().getId().equals(teacher.getId())) {
            return false;
        }
        video.setTitle(title);
        video.setSubject(subject);
        video.setDescription(description);
        if (file != null && !file.isEmpty()) {
            Files.createDirectories(uploadRoot);
            String ext = validateAndGetExtension(file);
            String filename = UUID.randomUUID() + ext;
            Path target = uploadRoot.resolve(filename).normalize();
            if (!target.startsWith(uploadRoot)) {
                throw new SecurityException("Invalid file path detected");
            }
            file.transferTo(target.toFile());
            String oldPath = video.getFilePath();
            video.setFilePath(filename);
            if (oldPath != null && !oldPath.isBlank()) {
                Files.deleteIfExists(uploadRoot.resolve(oldPath).normalize());
            }
        }
        videoRepository.save(video);
        return true;
    }

    public List<Video> findAll() {
        return videoRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Video> findBySubject(String subject) {
        return videoRepository.findBySubjectOrderByCreatedAtDesc(subject);
    }

    public List<String> findAllSubjects() {
        return videoRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(Video::getSubject)
            .distinct()
            .sorted()
            .toList();
    }

    public List<Video> findByUploadedBy(User teacher) {
        return videoRepository.findByUploadedBy(teacher);
    }

    /**
     * Deletes a video and its related data + file.
     * - Admin can delete any video
     * - Teacher can delete only their uploaded videos
     */
    @Transactional
    public boolean deleteVideo(Long videoId, User requester) {
        if (requester == null) return false;
        Video video = videoRepository.findById(videoId).orElse(null);
        if (video == null) return false;

        boolean isAdmin = requester.getRole() == User.Role.ADMIN;
        boolean isOwnerTeacher = requester.getRole() == User.Role.TEACHER
            && video.getUploadedBy() != null
            && video.getUploadedBy().getId() != null
            && video.getUploadedBy().getId().equals(requester.getId());

        if (!isAdmin && !isOwnerTeacher) return false;

        // Delete dependent rows first to avoid FK issues
        try { videoProgressRepository.deleteByVideo(video); } catch (Exception ignored) {}
        try { testResultRepository.deleteByVideo(video); } catch (Exception ignored) {}
        try { questionRepository.deleteByVideo(video); } catch (Exception ignored) {}

        // Delete the DB record
        videoRepository.delete(video);

        // Best-effort delete the uploaded file from disk
        try {
            if (video.getFilePath() != null && !video.getFilePath().isBlank()) {
                Files.deleteIfExists(uploadRoot.resolve(video.getFilePath()));
            }
        } catch (Exception ignored) {}

        return true;
    }

    public Path getUploadRoot() {
        return uploadRoot;
    }
}

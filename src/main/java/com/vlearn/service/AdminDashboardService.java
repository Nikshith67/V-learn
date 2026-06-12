package com.vlearn.service;

import com.vlearn.dto.DashboardStatsDto;
import com.vlearn.entity.User;
import com.vlearn.entity.Video;
import com.vlearn.repository.UserRepository;
import com.vlearn.repository.VideoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final VideoRepository videoRepository;

    public AdminDashboardService(UserRepository userRepository, VideoRepository videoRepository) {
        this.userRepository = userRepository;
        this.videoRepository = videoRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {
        List<User> students = userRepository.findByRole(User.Role.STUDENT);
        List<User> teachers = userRepository.findByRole(User.Role.TEACHER);
        List<Video> videos = videoRepository.findAll();

        long totalStudents = students.size();
        long totalTeachers = teachers.size();
        long totalVideos = videos.size();

        List<String> studentNames = students.stream()
                .map(u -> u.getFullName() + " (" + u.getUsername() + ")")
                .toList();
        List<String> teacherNames = teachers.stream()
                .map(u -> u.getFullName() + " (" + u.getUsername() + ")")
                .toList();
        List<String> videoTitles = videos.stream()
                .map(v -> v.getTitle() + (v.getUploadedBy() != null ? " (by " + v.getUploadedBy().getFullName() + ")" : ""))
                .toList();

        return new DashboardStatsDto(totalStudents, totalTeachers, totalVideos, studentNames, teacherNames, videoTitles);
    }
}

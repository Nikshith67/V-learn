package com.vlearn.dto;

import java.util.List;

public class DashboardStatsDto {

    private final long totalStudents;
    private final long totalTeachers;
    private final long totalVideos;
    private final List<String> studentNames;
    private final List<String> teacherNames;
    private final List<String> videoTitles;

    public DashboardStatsDto(long totalStudents, long totalTeachers, long totalVideos,
                             List<String> studentNames, List<String> teacherNames, List<String> videoTitles) {
        this.totalStudents = totalStudents;
        this.totalTeachers = totalTeachers;
        this.totalVideos = totalVideos;
        this.studentNames = studentNames;
        this.teacherNames = teacherNames;
        this.videoTitles = videoTitles;
    }

    public long getTotalStudents() {
        return totalStudents;
    }

    public long getTotalTeachers() {
        return totalTeachers;
    }

    public long getTotalVideos() {
        return totalVideos;
    }

    public List<String> getStudentNames() {
        return studentNames;
    }

    public List<String> getTeacherNames() {
        return teacherNames;
    }

    public List<String> getVideoTitles() {
        return videoTitles;
    }
}

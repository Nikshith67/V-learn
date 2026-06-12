package com.vlearn.repository;

import com.vlearn.entity.TestResult;
import com.vlearn.entity.User;
import com.vlearn.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TestResultRepository extends JpaRepository<TestResult, Long> {

    List<TestResult> findByStudentOrderByAttemptedAtDesc(User student);

    List<TestResult> findByStudentIdOrderByAttemptedAtDesc(Long studentId);

    Optional<TestResult> findFirstByStudentAndVideoOrderByAttemptedAtDesc(User student, Video video);

    List<TestResult> findByVideo(Video video);

    void deleteByVideo(Video video);
}

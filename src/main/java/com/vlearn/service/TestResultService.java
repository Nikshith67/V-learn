package com.vlearn.service;

import com.vlearn.entity.TestResult;
import com.vlearn.entity.User;
import com.vlearn.repository.TestResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TestResultService {

    private final TestResultRepository testResultRepository;

    public TestResultService(TestResultRepository testResultRepository) {
        this.testResultRepository = testResultRepository;
    }

    @Transactional
    public TestResult save(TestResult result) {
        return testResultRepository.save(result);
    }

    public List<TestResult> findByStudent(User student) {
        return testResultRepository.findByStudentOrderByAttemptedAtDesc(student);
    }

    public List<TestResult> findByStudentId(Long studentId) {
        return testResultRepository.findByStudentIdOrderByAttemptedAtDesc(studentId);
    }

    public Optional<TestResult> findById(Long id) {
        return testResultRepository.findById(id);
    }

    public List<TestResult> findAll() {
        return testResultRepository.findAll();
    }
}

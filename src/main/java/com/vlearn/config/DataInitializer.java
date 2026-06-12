package com.vlearn.config;

import com.vlearn.entity.User;
import com.vlearn.repository.UserRepository;
import com.vlearn.service.AuthService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AuthService authService;

    public DataInitializer(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(authService.hashPassword("admin123"));
            admin.setFullName("Administrator");
            admin.setEmail("admin@vlearn.local");
            admin.setRole(User.Role.ADMIN);
            admin.setApproved(true);
            userRepository.save(admin);
        }

        if (userRepository.findByUsername("student1").isEmpty()) {
            User student = new User();
            student.setUsername("student1");
            student.setPassword(authService.hashPassword("student123"));
            student.setFullName("Student One");
            student.setEmail("student1@vlearn.local");
            student.setRole(User.Role.STUDENT);
            student.setApproved(true);
            userRepository.save(student);
        }

        if (userRepository.findByUsername("teacher1").isEmpty()) {
            User teacher = new User();
            teacher.setUsername("teacher1");
            teacher.setPassword(authService.hashPassword("teacher123"));
            teacher.setFullName("Teacher One");
            teacher.setEmail("teacher1@vlearn.local");
            teacher.setRole(User.Role.TEACHER);
            teacher.setApproved(true); // demo teacher can login immediately
            teacher.setQualification("M.Ed., Mathematics");
            teacher.setExperienceYears(5);
            teacher.setCoreConcepts("Algebra, Geometry, Calculus fundamentals");
            userRepository.save(teacher);
        }
    }
}

package com.vlearn.service;

import com.vlearn.entity.User;
import com.vlearn.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuthService authService;

    public UserService(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Transactional
    public User register(User user, String plainPassword) {
        user.setPassword(authService.hashPassword(plainPassword));
        if (user.getRole() == User.Role.TEACHER) {
            user.setApproved(false);
        }
        return userRepository.save(user);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public List<User> findTeachersPendingApproval() {
        return userRepository.findByRoleAndApproved(User.Role.TEACHER, false);
    }

    /**
     * Approves a pending teacher. Returns false if the user does not exist or is not a pending teacher.
     */
    @Transactional
    public boolean approveTeacher(Long teacherId) {
        Optional<User> opt = userRepository.findById(teacherId);
        if (opt.isEmpty()) {
            return false;
        }
        User t = opt.get();
        if (t.getRole() != User.Role.TEACHER || t.isApproved()) {
            return false;
        }
        t.setApproved(true);
        userRepository.save(t);
        return true;
    }

    /**
     * Reject a teacher registration request (removes the pending account).
     * Only intended for TEACHER users that are not yet approved.
     */
    @Transactional
    public boolean rejectTeacher(Long teacherId) {
        Optional<User> opt = userRepository.findById(teacherId);
        if (opt.isEmpty()) {
            return false;
        }
        User t = opt.get();
        if (t.getRole() != User.Role.TEACHER || t.isApproved()) {
            return false;
        }
        userRepository.delete(t);
        return true;
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    public long countByRole(User.Role role) {
        return userRepository.countByRole(role);
    }

    public long countByRoleNot(User.Role role) {
        return userRepository.countByRoleNot(role);
    }

    public long countStudents() {
        return userRepository.countByRole(User.Role.STUDENT);
    }

    public long countTeachers() {
        return userRepository.countByRole(User.Role.TEACHER);
    }
}

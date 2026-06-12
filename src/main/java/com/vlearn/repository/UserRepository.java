package com.vlearn.repository;

import com.vlearn.entity.User;
import com.vlearn.entity.User.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByRole(User.Role role);

    List<User> findByRoleAndApproved(User.Role role, boolean approved);

    long countByRoleAndApproved(User.Role role, boolean approved);

    long countByRole(Role role);

    long countByRoleNot(User.Role role);
}

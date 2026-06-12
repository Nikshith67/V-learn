package com.vlearn.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /**
     * For TEACHER role: must be true before they can log in and upload content.
     */
    @Column(nullable = false)
    private boolean approved = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Required for TEACHER registrations; profile / subjects expertise. */
    @Column(length = 500)
    private String qualification;

    /** Years of teaching experience; required for TEACHER. */
    @Column(name = "experience_years")
    private Integer experienceYears;

    /** Subjects / core concepts the teacher covers; required for TEACHER. */
    @Column(name = "core_concepts", length = 2000)
    private String coreConcepts;

    public enum Role {
        ADMIN, TEACHER, STUDENT
    }

    public User() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }

    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }

    public String getCoreConcepts() { return coreConcepts; }
    public void setCoreConcepts(String coreConcepts) { this.coreConcepts = coreConcepts; }
}

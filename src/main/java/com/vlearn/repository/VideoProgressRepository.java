package com.vlearn.repository;

import com.vlearn.entity.VideoProgress;
import com.vlearn.entity.User;
import com.vlearn.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VideoProgressRepository extends JpaRepository<VideoProgress, Long> {

    Optional<VideoProgress> findByStudentAndVideo(User student, Video video);

    void deleteByVideo(Video video);

    @Query("SELECT vp FROM VideoProgress vp JOIN FETCH vp.video WHERE vp.student.id = :studentId ORDER BY vp.updatedAt DESC")
    List<VideoProgress> findAllByStudentIdWithVideo(@Param("studentId") Long studentId);
}

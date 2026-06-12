package com.vlearn.repository;

import com.vlearn.entity.Video;
import com.vlearn.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VideoRepository extends JpaRepository<Video, Long> {

    List<Video> findBySubjectOrderByCreatedAtDesc(String subject);

    List<Video> findAllByOrderByCreatedAtDesc();

    List<Video> findByUploadedBy(User teacher);
}

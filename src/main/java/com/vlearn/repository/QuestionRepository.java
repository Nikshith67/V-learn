package com.vlearn.repository;

import com.vlearn.entity.Question;
import com.vlearn.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByVideoOrderById(Video video);

    List<Question> findByVideoIdOrderById(Long videoId);

    void deleteByVideo(Video video);
}

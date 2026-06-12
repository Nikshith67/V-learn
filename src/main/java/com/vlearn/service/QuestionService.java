package com.vlearn.service;

import com.vlearn.entity.Question;
import com.vlearn.entity.Video;
import com.vlearn.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;

    public QuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Transactional
    public Question save(Question question) {
        return questionRepository.save(question);
    }

    public List<Question> findByVideoId(Long videoId) {
        return questionRepository.findByVideoIdOrderById(videoId);
    }

    public List<Question> findByVideo(Video video) {
        return questionRepository.findByVideoOrderById(video);
    }

    public Optional<Question> findById(Long id) {
        return questionRepository.findById(id);
    }

    public void delete(Long id) {
        questionRepository.deleteById(id);
    }
}

package com.vlearn.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(nullable = false)
    private String option1;

    @Column(nullable = false)
    private String option2;

    @Column(nullable = false)
    private String option3;

    @Column(nullable = false)
    private String option4;

    /**
     * 1-based index of correct option (1, 2, 3, or 4).
     */
    @Column(name = "correct_option", nullable = false)
    private int correctOption;

    public Question() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Video getVideo() { return video; }
    public void setVideo(Video video) { this.video = video; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getOption1() { return option1; }
    public void setOption1(String option1) { this.option1 = option1; }
    public String getOption2() { return option2; }
    public void setOption2(String option2) { this.option2 = option2; }
    public String getOption3() { return option3; }
    public void setOption3(String option3) { this.option3 = option3; }
    public String getOption4() { return option4; }
    public void setOption4(String option4) { this.option4 = option4; }
    public int getCorrectOption() { return correctOption; }
    public void setCorrectOption(int correctOption) { this.correctOption = correctOption; }

    public String getOption(int index) {
        return switch (index) {
            case 1 -> option1;
            case 2 -> option2;
            case 3 -> option3;
            case 4 -> option4;
            default -> null;
        };
    }
}

package com.example.demo01.model;


import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_question_record")
public class AiQuestionRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private LocalDateTime createAt;

    @Column(length = 5000)
    private String transcriptionContent;  // 转写内容

    @Column(length = 1000)
    private String customRemark;  // 自定义备注

    @Column(length = 5000)
    private String generatedQuestion;  // AI 生成的题目

    public AiQuestionRecord(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getCreateAt() {
        return createAt;
    }

    public void setCreateAt(LocalDateTime createAt) {
        this.createAt = createAt;
    }

    public String getTranscriptionContent() {
        return transcriptionContent;
    }

    public void setTranscriptionContent(String transcriptionContent) {
        this.transcriptionContent = transcriptionContent;
    }

    public String getCustomRemark() {
        return customRemark;
    }

    public void setCustomRemark(String customRemark) {
        this.customRemark = customRemark;
    }

    public String getGeneratedQuestion() {
        return generatedQuestion;
    }

    public void setGeneratedQuestion(String generatedQuestion) {
        this.generatedQuestion = generatedQuestion;
    }

    @Override
    public String toString() {
        return "AiQuestionRecord{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", createAt=" + createAt +
                ", transcriptionContent='" + transcriptionContent + '\'' +
                ", customRemark='" + customRemark + '\'' +
                ", generatedQuestion='" + generatedQuestion + '\'' +
                '}';
    }
}

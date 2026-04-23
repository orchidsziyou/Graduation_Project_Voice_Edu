package com.example.demo01.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "question_assignments")
public class QuestionAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    @Column(nullable = false)
    private Long questionId;                 // 题目 ID
    
    @Column(nullable = false)
    private Long classId;                    // 班级 ID
    
    @Column(nullable = false)
    private Long teacherId;                  // 教师 ID
    
    @Column(nullable = true)
    private String title;                    // 推送标题
    
    @Column(nullable = true)
    private String requirement;              // 推送要求
    
    @Column(nullable = false)
    private Integer status;                  // 状态：0=未发布，1=已发布，2=已截止
    
    @Column(nullable = true)
    private LocalDateTime publishTime;       // 发布时间
    
    @Column(nullable = true)
    private LocalDateTime deadline;          // 截止时间
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;         // 创建时间
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getQuestionId() {
        return questionId;
    }
    
    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }
    
    public Long getClassId() {
        return classId;
    }
    
    public void setClassId(Long classId) {
        this.classId = classId;
    }
    
    public Long getTeacherId() {
        return teacherId;
    }
    
    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getRequirement() {
        return requirement;
    }
    
    public void setRequirement(String requirement) {
        this.requirement = requirement;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public LocalDateTime getPublishTime() {
        return publishTime;
    }
    
    public void setPublishTime(LocalDateTime publishTime) {
        this.publishTime = publishTime;
    }
    
    public LocalDateTime getDeadline() {
        return deadline;
    }
    
    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    @Override
    public String toString() {
        return "QuestionAssignment{" +
                "id=" + id +
                ", questionId=" + questionId +
                ", classId=" + classId +
                ", teacherId=" + teacherId +
                ", title='" + title + '\'' +
                ", requirement='" + requirement + '\'' +
                ", status=" + status +
                ", publishTime=" + publishTime +
                ", deadline=" + deadline +
                ", createdAt=" + createdAt +
                '}';
    }
    
    public QuestionAssignment() {
    }
    
    public QuestionAssignment(Long questionId, Long classId, Long teacherId, String title, Integer status) {
        this.questionId = questionId;
        this.classId = classId;
        this.teacherId = teacherId;
        this.title = title;
        this.status = status;
    }
}

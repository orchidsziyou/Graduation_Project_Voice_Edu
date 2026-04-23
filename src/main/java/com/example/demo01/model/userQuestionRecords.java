package com.example.demo01.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "userQuestionRecords")
public class userQuestionRecords {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private Long userid;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserid() {
        return userid;
    }

    public void setUserid(Long userid) {
        this.userid = userid;
    }

    public Long getQuestionid() {
        return questionid;
    }

    public void setQuestionid(Long questionid) {
        this.questionid = questionid;
    }

    public String getQuestion_answer() {
        return question_answer;
    }

    public void setQuestion_answer(String question_answer) {
        this.question_answer = question_answer;
    }

    public Integer getQuestion_type() {
        return question_type;
    }

    public void setQuestion_type(Integer question_type) {
        this.question_type = question_type;
    }
    
    // Getters and Setters for new fields
    public Long getAssignmentId() {
        return assignmentId;
    }
    
    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }
    
    public Long getClassId() {
        return classId;
    }
    
    public void setClassId(Long classId) {
        this.classId = classId;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public Boolean getIsCorrect() {
        return isCorrect;
    }
    
    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }
    
    public LocalDateTime getAnswerTime() {
        return answerTime;
    }
    
    public void setAnswerTime(LocalDateTime answerTime) {
        this.answerTime = answerTime;
    }

    @Column(nullable = false)
    private Long questionid;

    @Column(nullable = false)
    private String question_answer;

    @Column(nullable = false)
    private Integer question_type;

    @Column(nullable = true)
    private Long assignmentId;               // 推送 ID
    
    @Column(nullable = true)
    private Long classId;                    // 班级 ID
    
    @Column(nullable = false)
    private Integer status;                  // 状态：0=未作答，1=已作答
    
    @Column(nullable = true)
    private Boolean isCorrect;               // 是否正确
    
    @Column(nullable = true)
    private LocalDateTime answerTime;        // 答题提交时间
    
    @Column(nullable = true, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "userQuestionRecords{" +
                "id=" + id +
                ", userid=" + userid +
                ", questionid=" + questionid +
                ", question_answer='" + question_answer + '\'' +
                ", question_type=" + question_type +
                ", assignmentId=" + assignmentId +
                ", classId=" + classId +
                ", status=" + status +
                ", isCorrect=" + isCorrect +
                ", answerTime=" + answerTime +
                ", createdAt=" + createdAt +
                '}';
    }

    public userQuestionRecords() {
    }
    
    public userQuestionRecords(Long userid, Long questionid, String question_answer, Integer question_type) {
        this.userid = userid;
        this.questionid = questionid;
        this.question_answer = question_answer;
        this.question_type = question_type;
        this.status = 0;  // 默认未作答
    }
    
    public userQuestionRecords(Long userid, Long questionid, String question_answer, Integer question_type, Long assignmentId, Long classId) {
        this.userid = userid;
        this.questionid = questionid;
        this.question_answer = question_answer;
        this.question_type = question_type;
        this.assignmentId = assignmentId;
        this.classId = classId;
        this.status = 0;  // 默认未作答
    }

}

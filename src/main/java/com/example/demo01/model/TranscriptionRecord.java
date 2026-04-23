package com.example.demo01.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transcription_records")
public class TranscriptionRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "order_id")
    private String orderId;
    
    @Column(name = "transcription_text", columnDefinition = "TEXT")
    private String transcriptionText;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "file_name")
    private String fileName;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    // 构造函数
    public TranscriptionRecord() {}
    
    // 云端转写构造函数（有 order_id）
    public TranscriptionRecord(Long userId, String orderId, String fileName, Long fileSize) {
        this.userId = userId;
        this.orderId = orderId;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.createdAt = LocalDateTime.now();
    }
    
    // 本地转写构造函数（有 transcriptionText）
    public TranscriptionRecord(Long userId, String fileName, Long fileSize, String transcriptionText) {
        this.userId = userId;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.transcriptionText = transcriptionText;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getter和Setter方法
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public String getTranscriptionText() {
        return transcriptionText;
    }
    
    public void setTranscriptionText(String transcriptionText) {
        this.transcriptionText = transcriptionText;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    @Override
    public String toString() {
        return "TranscriptionRecord{" +
                "id=" + id +
                ", userId=" + userId +
                ", orderId='" + orderId + '\'' +
                ", transcriptionText='" + (transcriptionText != null ? transcriptionText.substring(0, Math.min(50, transcriptionText.length())) + "..." : "null") + '\'' +
                ", createdAt=" + createdAt +
                ", fileName='" + fileName + '\'' +
                ", fileSize=" + fileSize +
                '}';
    }
}
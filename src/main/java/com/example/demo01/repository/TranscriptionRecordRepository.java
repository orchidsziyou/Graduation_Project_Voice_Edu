package com.example.demo01.repository;

import com.example.demo01.model.TranscriptionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TranscriptionRecordRepository extends JpaRepository<TranscriptionRecord, Long> {
    
    /**
     * 根据用户ID查找该用户的所有转写记录
     */
    List<TranscriptionRecord> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * 根据订单ID查找转写记录（云端转写）
     */
    TranscriptionRecord findByOrderId(String orderId);
    
    /**
     * 根据用户ID和订单ID查找特定记录（云端转写）
     */
    TranscriptionRecord findByUserIdAndOrderId(Long userId, String orderId);
    
    /**
     * 查找某个用户的所有本地转写记录
     */
    List<TranscriptionRecord> findByUserIdAndOrderIdIsNullOrderByCreatedAtDesc(Long userId);
    
    /**
     * 统计某个用户的转写记录数量
     */
    long countByUserId(Long userId);
}
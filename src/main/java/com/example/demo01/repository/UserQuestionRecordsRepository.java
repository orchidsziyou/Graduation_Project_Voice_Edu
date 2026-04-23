package com.example.demo01.repository;

import com.example.demo01.model.userQuestionRecords;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserQuestionRecordsRepository extends JpaRepository<userQuestionRecords, Long> {
    
    /**
     * 根据用户 ID 查询答题记录（分页）
     */
    Page<userQuestionRecords> findByUseridOrderByCreatedAtDesc(Long userid, Pageable pageable);
    
    /**
     * 根据用户 ID 查询答题记录（不分页）
     */
    List<userQuestionRecords> findByUseridOrderByCreatedAtDesc(Long userid);
    
    /**
     * 根据用户 ID 和状态查询答题记录
     */
    List<userQuestionRecords> findByUseridAndStatusOrderByCreatedAtDesc(Long userid, Integer status);
    
    /**
     * 根据推送 ID 查询答题记录
     */
    List<userQuestionRecords> findByAssignmentIdOrderByCreatedAtDesc(Long assignmentId);
    
    /**
     * 根据班级 ID 和状态查询答题记录
     */
    List<userQuestionRecords> findByClassIdAndStatusOrderByCreatedAtDesc(Long classId, Integer status);
    
    /**
     * 根据推送 ID 和学生 ID 查询答题记录
     */
    userQuestionRecords findByAssignmentIdAndUserid(Long assignmentId, Long userid);
}

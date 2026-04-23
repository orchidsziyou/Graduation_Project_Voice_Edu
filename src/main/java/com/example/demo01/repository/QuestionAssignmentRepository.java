package com.example.demo01.repository;

import com.example.demo01.model.QuestionAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionAssignmentRepository extends JpaRepository<QuestionAssignment, Long> {
    
    /**
     * 根据班级 ID 查询推送
     */
    Page<QuestionAssignment> findByClassIdOrderByCreatedAtDesc(Long classId, Pageable pageable);
    
    /**
     * 根据教师 ID 查询推送
     */
    Page<QuestionAssignment> findByTeacherIdOrderByCreatedAtDesc(Long teacherId, Pageable pageable);
    
    /**
     * 根据班级 ID 和状态查询推送
     */
    List<QuestionAssignment> findByClassIdAndStatusOrderByCreatedAtDesc(Long classId, Integer status);
    
    /**
     * 根据教师 ID 和状态查询推送
     */
    List<QuestionAssignment> findByTeacherIdAndStatusOrderByCreatedAtDesc(Long teacherId, Integer status);


    /**
     * 根据班级 ID 查询所有推送记录
     */

    List<QuestionAssignment>findByClassIdOrderByCreatedAtDesc(Long classId);
}

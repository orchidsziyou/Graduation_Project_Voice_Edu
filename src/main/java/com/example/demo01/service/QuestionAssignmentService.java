package com.example.demo01.service;

import com.example.demo01.model.QuestionAssignment;
import com.example.demo01.model.ClassMembers;
import com.example.demo01.model.userQuestionRecords;
import com.example.demo01.model.question_items;
import com.example.demo01.repository.QuestionAssignmentRepository;
import com.example.demo01.repository.UserQuestionRecordsRepository;
import com.example.demo01.repository.QuestionItemsRepository;
import com.example.demo01.repository.ClassMembersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@Service
public class QuestionAssignmentService {
    
    @Autowired
    private QuestionAssignmentRepository questionAssignmentRepository;
    
    @Autowired
    private UserQuestionRecordsRepository userQuestionRecordsRepository;
    
    @Autowired
    private QuestionItemsRepository questionItemsRepository;
    
    @Autowired
    private ClassMembersRepository classMembersRepository;
    
    /**
     * 创建题目推送（自动为班级所有学生创建答题记录）
     */
    @Transactional
    public QuestionAssignment createAssignment(Long questionId, Long classId, Long teacherId, 
                                               String title, String requirement, 
                                               LocalDateTime deadline) {
        // 创建推送任务
        QuestionAssignment assignment = new QuestionAssignment();
        assignment.setQuestionId(questionId);
        assignment.setClassId(classId);
        assignment.setTeacherId(teacherId);
        assignment.setTitle(title);
        assignment.setRequirement(requirement);
        assignment.setStatus(1);  // 1=已发布
        assignment.setPublishTime(LocalDateTime.now());
        assignment.setDeadline(deadline);
        
        assignment = questionAssignmentRepository.save(assignment);
        
        // 获取班级所有成员（只获取学生，userrole=1）
        List<ClassMembers> allMembers = classMembersRepository.findByClassidOrderByJoinAtDesc(classId);
        List<ClassMembers> members = new ArrayList<>();
        for (ClassMembers member : allMembers) {
            if (member.getUserrole() == 1) {  // 1=学生
                members.add(member);
            }
        }
        
        // 获取题目信息
        question_items question = questionItemsRepository.findById(questionId).orElse(null);
        if (question == null) {
            throw new RuntimeException("题目不存在");
        }
        
        // 为每个学生创建答题记录
        for (ClassMembers member : members) {
            userQuestionRecords record = new userQuestionRecords(
                member.getUserid(),           // 学生 ID
                questionId,                   // 题目 ID
                "",                           // 空答案，表示未作答
                question.getQuestion_type(),  // 题型
                assignment.getId(),           // 推送 ID
                classId                       // 班级 ID
            );
            record.setStatus(0);  // 0=未作答
            userQuestionRecordsRepository.save(record);
        }
        
        return assignment;
    }
    
    /**
     * 分页查询教师的推送记录
     */
    public Page<QuestionAssignment> getTeacherAssignments(Long teacherId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return questionAssignmentRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId, pageable);
    }
    
    /**
     * 分页查询班级的推送记录
     */
    public Page<QuestionAssignment> getClassAssignments(Long classId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return questionAssignmentRepository.findByClassIdOrderByCreatedAtDesc(classId, pageable);
    }
    
    /**
     * 获取推送详情
     */
    public QuestionAssignment getAssignment(Long assignmentId) {
        return questionAssignmentRepository.findById(assignmentId).orElse(null);
    }
    
    /**
     * 更新推送状态
     */
    @Transactional
    public QuestionAssignment updateStatus(Long assignmentId, Integer status) {
        QuestionAssignment assignment = questionAssignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new RuntimeException("推送不存在"));
        
        assignment.setStatus(status);
        if (status == 2) {  // 已截止
            assignment.setDeadline(LocalDateTime.now());
        }
        return questionAssignmentRepository.save(assignment);
    }
    
    /**
     * 获取某次推送的答题统计
     */
    public AssignmentStatistics getAssignmentStatistics(Long assignmentId) {
        List<userQuestionRecords> records = userQuestionRecordsRepository.findByAssignmentIdOrderByCreatedAtDesc(assignmentId);
        
        long total = records.size();
        long answered = records.stream().filter(r -> r.getStatus() == 1).count();
        long correct = records.stream().filter(r -> Boolean.TRUE.equals(r.getIsCorrect())).count();
        
        AssignmentStatistics stats = new AssignmentStatistics();
        stats.setTotal(total);
        stats.setAnswered(answered);
        stats.setUnanswered(total - answered);
        stats.setCorrect(correct);
        
        if (answered > 0) {
            stats.setCorrectRate(correct * 100.0 / answered);
        } else {
            stats.setCorrectRate(0.0);
        }
        
        return stats;
    }
    
    /**
     * 获取学生在某次推送中的答题记录
     */
    public userQuestionRecords getStudentAnswerRecord(Long assignmentId, Long studentId) {
        return userQuestionRecordsRepository.findByAssignmentIdAndUserid(assignmentId, studentId);
    }
    
    /**
     * 获取学生的待完成任务（所有未作答的推送题目）
     */
    public List<Map<String, Object>> getStudentPendingAssignments(Long studentId) {
//        System.out.println("=== 获取学生待完成任务 ===");
//        System.out.println("学生 ID: " + studentId);
        
        // 查找该学生所有未作答的记录（status=0 且有 assignmentId）
        List<userQuestionRecords> records = userQuestionRecordsRepository.findByUseridAndStatusOrderByCreatedAtDesc(studentId, 0);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (userQuestionRecords record : records) {
            if (record.getAssignmentId() != null) {
                // 获取推送信息
                QuestionAssignment assignment = questionAssignmentRepository.findById(record.getAssignmentId()).orElse(null);
                if (assignment != null) {
                    // 获取题目信息
                    question_items question = questionItemsRepository.findById(record.getQuestionid()).orElse(null);
                    
                    Map<String, Object> item = new HashMap<>();
                    item.put("assignmentId", assignment.getId());
                    item.put("assignmentTitle", assignment.getTitle());
                    item.put("requirement", assignment.getRequirement());
                    item.put("deadline", assignment.getDeadline());
                    item.put("recordId", record.getId());
                    item.put("status", record.getStatus()); // 0=未完成, 1=已完成
                    
                    if (question != null) {
                        item.put("questionId", question.getId());
                        item.put("questionBody", question.getQuestion_body());
                        item.put("questionType", question.getQuestion_type());
                        item.put("choosingAnswer", question.getChoosing_answer());
                    }
                    
                    result.add(item);
                }
            }
        }
        
        System.out.println("找到 " + result.size() + " 个待完成任务");
        return result;
    }
    
    /**
     * 获取学生的所有推送题目（包括已完成和未完成）
     */
    public List<Map<String, Object>> getStudentAllAssignments(Long studentId) {
        System.out.println("=== 获取学生所有推送题目 ===");
        System.out.println("学生 ID: " + studentId);
        
        // 查找该学生所有有 assignmentId 的记录（不管 status）
        List<userQuestionRecords> allRecords = userQuestionRecordsRepository.findByUseridOrderByCreatedAtDesc(studentId);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (userQuestionRecords record : allRecords) {
            if (record.getAssignmentId() != null) {
                // 获取推送信息
                QuestionAssignment assignment = questionAssignmentRepository.findById(record.getAssignmentId()).orElse(null);
                if (assignment != null) {
                    // 获取题目信息
                    question_items question = questionItemsRepository.findById(record.getQuestionid()).orElse(null);
                    
                    Map<String, Object> item = new HashMap<>();
                    item.put("assignmentId", assignment.getId());
                    item.put("assignmentTitle", assignment.getTitle());
                    item.put("requirement", assignment.getRequirement());
                    item.put("deadline", assignment.getDeadline());
                    item.put("recordId", record.getId());
                    item.put("status", record.getStatus()); // 0=未完成, 1=已完成
                    
                    if (question != null) {
                        item.put("questionId", question.getId());
                        item.put("questionBody", question.getQuestion_body());
                        item.put("questionType", question.getQuestion_type());
                        item.put("choosingAnswer", question.getChoosing_answer());
                    }
                    
                    result.add(item);
                }
            }
        }
        
        System.out.println("找到 " + result.size() + " 个推送题目");
        return result;
    }
    
    /**
     * 学生提交答案
     */
    @Transactional
    public userQuestionRecords submitAnswer(Long recordId, String answer) {
        userQuestionRecords record = userQuestionRecordsRepository.findById(recordId)
            .orElseThrow(() -> new RuntimeException("记录不存在"));
        
        if (record.getStatus() == 1) {
            throw new RuntimeException("已经作答，不能重复提交");
        }
        
        // 更新答案
        record.setQuestion_answer(answer);
        record.setStatus(1);  // 已作答
        record.setAnswerTime(LocalDateTime.now());
        
        // 自动批改（如果是选择题）
        if (record.getQuestion_type() == 0) {
            question_items question = questionItemsRepository.findById(record.getQuestionid())
                .orElse(null);
            
            if (question != null && question.getChoosing_answer() != null) {
                // 假设 choosing_answer 存储的是正确答案，格式如 "A"
                String correctAnswer = question.getChoosing_answer().trim();
                record.setIsCorrect(correctAnswer.equalsIgnoreCase(answer.trim()));
            }
        }
        
        return userQuestionRecordsRepository.save(record);
    }
    
    /**
     * 统计类（用于返回统计数据）
     */
    public static class AssignmentStatistics {
        private long total;          // 总人数
        private long answered;       // 已答人数
        private long unanswered;     // 未答人数
        private long correct;        // 正确人数
        private double correctRate;  // 正确率
        
        // Getters and Setters
        public long getTotal() {
            return total;
        }
        
        public void setTotal(long total) {
            this.total = total;
        }
        
        public long getAnswered() {
            return answered;
        }
        
        public void setAnswered(long answered) {
            this.answered = answered;
        }
        
        public long getUnanswered() {
            return unanswered;
        }
        
        public void setUnanswered(long unanswered) {
            this.unanswered = unanswered;
        }
        
        public long getCorrect() {
            return correct;
        }
        
        public void setCorrect(long correct) {
            this.correct = correct;
        }
        
        public double getCorrectRate() {
            return correctRate;
        }
        
        public void setCorrectRate(double correctRate) {
            this.correctRate = correctRate;
        }
    }
}

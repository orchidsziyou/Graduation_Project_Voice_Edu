package com.example.demo01.controller;

import com.example.demo01.model.QuestionAssignment;
import com.example.demo01.model.User;
import com.example.demo01.model.question_items;
import com.example.demo01.model.userQuestionRecords;
import com.example.demo01.repository.QuestionAssignmentRepository;
import com.example.demo01.repository.QuestionItemsRepository;
import com.example.demo01.repository.UserRepository;
import com.example.demo01.service.QuestionAssignmentService;
import com.example.demo01.repository.UserQuestionRecordsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assignment")
public class QuestionAssignmentController {
    
    @Autowired
    private QuestionAssignmentService questionAssignmentService;
    
    @Autowired
    private UserQuestionRecordsRepository userQuestionRecordsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuestionItemsRepository questionItemsRepository;

    @Autowired
    private QuestionAssignmentRepository questionAssignmentRepository;
    
    /**
     * 创建题目推送
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createAssignment(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long questionId = Long.parseLong(request.get("questionId").toString());
            Long classId = Long.parseLong(request.get("classId").toString());
            Long teacherId = Long.parseLong(request.get("teacherId").toString());
            String title = (String) request.get("title");
            String requirement = (String) request.get("requirement");
            
            LocalDateTime deadline = null;
            if (request.get("deadline") != null) {
                deadline = LocalDateTime.parse(request.get("deadline").toString());
            }
            
            // 参数验证
            if (questionId == null || questionId <= 0) {
                response.put("success", false);
                response.put("message", "题目 ID 无效");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (classId == null || classId <= 0) {
                response.put("success", false);
                response.put("message", "班级 ID 无效");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (teacherId == null || teacherId <= 0) {
                response.put("success", false);
                response.put("message", "教师 ID 无效");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 创建推送
            QuestionAssignment assignment = questionAssignmentService.createAssignment(
                questionId, classId, teacherId, title, requirement, deadline
            );
            response.put("success", true);
            response.put("message", "推送成功");
            response.put("data", assignment);
            response.put("assignmentId", assignment.getId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 查询教师的推送记录
     */
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<Map<String, Object>> getTeacherAssignments(
            @PathVariable Long teacherId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Page<QuestionAssignment> assignmentsPage = questionAssignmentService.getTeacherAssignments(teacherId, page, size);
            
            response.put("success", true);
            response.put("data", assignmentsPage.getContent());
            response.put("total", assignmentsPage.getTotalElements());
            response.put("totalPages", assignmentsPage.getTotalPages());
            response.put("currentPage", page);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 查询班级的推送记录
     */
    @GetMapping("/class/{classId}")
    public ResponseEntity<Map<String, Object>> getClassAssignments(
            @PathVariable Long classId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Page<QuestionAssignment> assignmentsPage = questionAssignmentService.getClassAssignments(classId, page, size);
            
            response.put("success", true);
            response.put("data", assignmentsPage.getContent());
            response.put("total", assignmentsPage.getTotalElements());
            response.put("totalPages", assignmentsPage.getTotalPages());
            response.put("currentPage", page);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取推送详情
     */
    @GetMapping("/{assignmentId}")
    public ResponseEntity<Map<String, Object>> getAssignment(@PathVariable Long assignmentId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            QuestionAssignment assignment = questionAssignmentService.getAssignment(assignmentId);
            
            if (assignment == null) {
                response.put("success", false);
                response.put("message", "推送不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            response.put("success", true);
            response.put("data", assignment);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 更新推送状态
     */
    @PutMapping("/{assignmentId}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long assignmentId,
            @RequestBody Map<String, Integer> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Integer status = request.get("status");
            
            if (status == null || status < 0 || status > 2) {
                response.put("success", false);
                response.put("message", "状态无效");
                return ResponseEntity.badRequest().body(response);
            }
            
            QuestionAssignment assignment = questionAssignmentService.updateStatus(assignmentId, status);
            
            response.put("success", true);
            response.put("message", "状态更新成功");
            response.put("data", assignment);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取推送统计
     */
    @GetMapping("/{assignmentId}/statistics")
    public ResponseEntity<Map<String, Object>> getAssignmentStatistics(@PathVariable Long assignmentId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            QuestionAssignmentService.AssignmentStatistics stats = 
                questionAssignmentService.getAssignmentStatistics(assignmentId);
            
            response.put("success", true);
            response.put("data", stats);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取学生在某次推送中的答题记录
     */
    @GetMapping("/{assignmentId}/student/{studentId}")
    public ResponseEntity<Map<String, Object>> getStudentAnswerRecord(
            @PathVariable Long assignmentId,
            @PathVariable Long studentId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            userQuestionRecords record = questionAssignmentService.getStudentAnswerRecord(assignmentId, studentId);
            
            if (record == null) {
                response.put("success", false);
                response.put("message", "未找到答题记录");
                return ResponseEntity.badRequest().body(response);
            }
            
            response.put("success", true);
            response.put("data", record);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 学生提交答案
     */
    @PostMapping("/submit-answer")
    public ResponseEntity<Map<String, Object>> submitAnswer(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long recordId = Long.parseLong(request.get("recordId").toString());
            String answer = (String) request.get("answer");
            
            if (recordId == null || recordId <= 0) {
                response.put("success", false);
                response.put("message", "记录 ID 无效");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (answer == null || answer.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "答案不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            userQuestionRecords record = questionAssignmentService.submitAnswer(recordId, answer);
            
            response.put("success", true);
            response.put("message", "提交成功");
            response.put("data", record);
            response.put("isCorrect", record.getIsCorrect());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取学生的待完成任务
     */
    @GetMapping("/pending/{studentId}")
    public ResponseEntity<Map<String, Object>> getPendingAssignments(@PathVariable Long studentId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("=== 查询待完成任务 ===");
            System.out.println("学生 ID: " + studentId);
            
            List<Map<String, Object>> assignments = questionAssignmentService.getStudentPendingAssignments(studentId);
            
            response.put("success", true);
            response.put("data", assignments);
            response.put("total", assignments.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取学生的所有推送题目（包括已完成和未完成）
     */
    @GetMapping("/all/{studentId}")
    public ResponseEntity<Map<String, Object>> getAllAssignments(@PathVariable Long studentId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("=== 查询所有推送题目 ===");
            System.out.println("学生 ID: " + studentId);
            
            List<Map<String, Object>> assignments = questionAssignmentService.getStudentAllAssignments(studentId);
            
            response.put("success", true);
            response.put("data", assignments);
            response.put("total", assignments.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取某次推送的所有答题记录
     */
    @GetMapping("/{assignmentId}/records")
    public ResponseEntity<Map<String, Object>> getAssignmentRecords(@PathVariable Long assignmentId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<userQuestionRecords> records = userQuestionRecordsRepository.findByAssignmentIdOrderByCreatedAtDesc(assignmentId);
            
            response.put("success", true);
            response.put("data", records);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    /**
     * 获取班级的所有推送记录（带统计信息）
     */
    @GetMapping("/class/{classId}/with-stats")
    public ResponseEntity<Map<String, Object>>  getClassAssignmentsWithStats(@PathVariable Long classId) {
        Map<String, Object> response = new HashMap<>();

        try{
            // 获取该班级的所有推送
            List<QuestionAssignment> assignments = questionAssignmentRepository
                    .findByClassIdOrderByCreatedAtDesc(classId);
            List<Map<String, Object>> result = new ArrayList<>();
            for (QuestionAssignment assignment : assignments) {
                // 获取该推送的统计数据
                QuestionAssignmentService.AssignmentStatistics stats = questionAssignmentService.getAssignmentStatistics(assignment.getId());

                // 获取题目信息
                question_items question = questionItemsRepository.findById(assignment.getQuestionId()).orElse(null);
                
                System.out.println("=== 题目信息调试 ===");
                System.out.println("推送 ID: " + assignment.getId());
                System.out.println("题目 ID: " + assignment.getQuestionId());
                System.out.println("题目对象: " + question);
                System.out.println("题目类型 question_type: " + (question != null ? question.getQuestion_type() : "null"));

                Map<String, Object> item = new HashMap<>();
                item.put("id", assignment.getId());
                item.put("title", assignment.getTitle());
                item.put("requirement", assignment.getRequirement());
                item.put("deadline", assignment.getDeadline());
                item.put("questionType", question != null ? question.getQuestion_type() : null);
                item.put("answeredCount", stats.getAnswered());
                item.put("unansweredCount", stats.getUnanswered());
                item.put("total", stats.getTotal());
                item.put("correctRate", stats.getCorrectRate());

                result.add(item);
            }

            response.put("success", true);
            response.put("data", result);
            response.put("total", result.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    /**
     * 获取某次推送的详细作答记录（包含学生信息）
     */
    @GetMapping("/{assignmentId}/answer-details")
    public ResponseEntity<Map<String, Object>> getAssignmentAnswerDetails(@PathVariable Long assignmentId) {
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("=== 查询推送作答详情 ===");
            System.out.println("推送 ID: " + assignmentId);

            // 获取推送信息
            QuestionAssignment assignment = questionAssignmentRepository.findById(assignmentId).orElse(null);
            if (assignment == null) {
                response.put("success", false);
                response.put("message", "推送不存在");
                return ResponseEntity.badRequest().body(response);
            }

            // 获取题目信息
            question_items question = questionItemsRepository.findById(assignment.getQuestionId()).orElse(null);
            
            System.out.println("=== 题目信息调试 ===");
            System.out.println("推送 ID: " + assignmentId);
            System.out.println("题目 ID: " + assignment.getQuestionId());
            System.out.println("题目类型 question_type: " + (question != null ? question.getQuestion_type() : "null"));

            // 获取所有作答记录
            List<userQuestionRecords> records = userQuestionRecordsRepository
                    .findByAssignmentIdOrderByCreatedAtDesc(assignmentId);

            List<Map<String, Object>> result = new ArrayList<>();
            for (userQuestionRecords record : records) {
                // 获取学生信息
                User student = userRepository.findById(record.getUserid()).orElse(null);

                Map<String, Object> item = new HashMap<>();
                item.put("id", record.getId());
                item.put("userId", record.getUserid());
                item.put("studentName", student != null ? student.getUsername() : "未知");
                item.put("answer", record.getQuestion_answer());
                item.put("status", record.getStatus());
                item.put("isCorrect", record.getIsCorrect());
                item.put("answerTime", record.getAnswerTime());

                result.add(item);
            }

            response.put("success", true);
            response.put("data", result);
            // 添加题目类型信息
            response.put("questionType", question != null ? question.getQuestion_type() : null);
            response.put("questionId", assignment.getQuestionId());
            response.put("assignmentTitle", assignment.getTitle());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

}

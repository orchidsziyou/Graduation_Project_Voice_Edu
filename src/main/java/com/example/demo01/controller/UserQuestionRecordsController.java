package com.example.demo01.controller;

import com.example.demo01.model.question_items;
import com.example.demo01.model.userQuestionRecords;
import com.example.demo01.service.UserQuestionRecordsService;
import com.example.demo01.repository.UserQuestionRecordsRepository;
import com.example.demo01.repository.QuestionItemsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/answer-question")
public class UserQuestionRecordsController {
    
    @Autowired
    private UserQuestionRecordsService userQuestionRecordsService;
    
    @Autowired
    private UserQuestionRecordsRepository userQuestionRecordsRepository;
    
    @Autowired
    private QuestionItemsRepository questionItemsRepository;
    
    /**
     * 随机获取一道题目
     */
    @GetMapping("/random")
    public ResponseEntity<Map<String, Object>> getRandomQuestion() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            question_items question = userQuestionRecordsService.getRandomQuestion();
            
            if (question == null) {
                response.put("success", false);
                response.put("message", "题库中没有题目");
                return ResponseEntity.ok(response);
            }
            
            response.put("success", true);
            response.put("data", question);
            response.put("message", "获取题目成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 提交答案
     */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitAnswer(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long userid = Long.parseLong(request.get("userid").toString());
            Long questionid = Long.parseLong(request.get("questionid").toString());
            String answer = (String) request.get("answer");
            Integer questionType = Integer.parseInt(request.get("questionType").toString());
            
            // 参数验证
            if (userid == null || userid <= 0) {
                response.put("success", false);
                response.put("message", "用户 ID 无效");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (questionid == null || questionid <= 0) {
                response.put("success", false);
                response.put("message", "题目 ID 无效");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (answer == null || answer.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "答案不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 保存答题记录（设置为已作答状态）
            userQuestionRecords record = new userQuestionRecords(userid, questionid, answer, questionType);
            record.setStatus(1);  // 1=已作答
            record.setAnswerTime(java.time.LocalDateTime.now());
            
            // 自动批改（如果是选择题）
            if (questionType == 0) {
                question_items question = questionItemsRepository.findById(questionid).orElse(null);
                if (question != null && question.getChoosing_answer() != null) {
                    String correctAnswer = question.getChoosing_answer().trim();
                    record.setIsCorrect(correctAnswer.equalsIgnoreCase(answer.trim()));
                }
            }
            
            record = userQuestionRecordsRepository.save(record);
            
            response.put("success", true);
            response.put("message", "答题成功");
            response.put("data", record);
            response.put("recordId", record.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 查询用户的答题记录
     */
    @GetMapping("/records/{userid}")
    public ResponseEntity<Map<String, Object>> getUserRecords(
            @PathVariable Long userid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Page<userQuestionRecords> recordsPage = userQuestionRecordsService.getUserRecords(userid, page, size);
            
            response.put("success", true);
            response.put("data", recordsPage);
            response.put("total", recordsPage.getTotalElements());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

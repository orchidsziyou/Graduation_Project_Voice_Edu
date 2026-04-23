package com.example.demo01.controller;

import com.example.demo01.model.question_items;
import com.example.demo01.service.QuestionItemsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/question-items")
public class QuestionItemsController {
    
    @Autowired
    private QuestionItemsService questionItemsService;
    
    /**
     * 保存题目
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveQuestion(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String questionBody = (String) request.get("questionBody");
            int questionType = Integer.parseInt(request.get("questionType").toString());
            String questionAnswer = (String) request.get("questionAnswer");
            String choosingAnswer = (String) request.get("choosingAnswer");
            Long userid = null;
            
            // 获取用户 ID（如果前端传递了）
            if (request.containsKey("userid")) {
                userid = Long.parseLong(request.get("userid").toString());
            }
            
            // 参数验证
            if (questionBody == null || questionBody.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "题干不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (questionType != 0 && questionType != 1) {
                response.put("success", false);
                response.put("message", "题目类型无效（0=选择题，1=主观题）");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 保存题目
            question_items savedItem = questionItemsService.saveQuestion(
                questionBody, questionType, questionAnswer, choosingAnswer, userid
            );
            
            response.put("success", true);
            response.put("message", "题目保存成功");
            response.put("data", savedItem);
            response.put("id", savedItem.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 分页查询题目列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getQuestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer type) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("=== 查询题目列表 ===");
            System.out.println("页码：" + page);
            System.out.println("每页数量：" + size);
            System.out.println("关键词：" + (keyword != null ? keyword : "无"));
            System.out.println("题型：" + (type != null ? type : "全部"));
            
            Page<question_items> questionsPage = questionItemsService.findAll(page, size, keyword);
            
            // 如果指定了题型，进行过滤
            java.util.List<question_items> filteredList = questionsPage.getContent();
            if (type != null) {
                final Integer finalType = type;
                filteredList = questionsPage.getContent().stream()
                    .filter(q -> q.getQuestion_type() == finalType)
                    .collect(java.util.stream.Collectors.toList());
            }
            
            response.put("success", true);
            response.put("data", filteredList);
            response.put("total", type != null ? filteredList.size() : questionsPage.getTotalElements());
            response.put("currentPage", page);
            response.put("pageSize", size);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

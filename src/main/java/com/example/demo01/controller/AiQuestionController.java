package com.example.demo01.controller;

import com.example.demo01.model.AiQuestionRecord;
import com.example.demo01.model.User;
import com.example.demo01.service.AiQuestionService;
import com.example.demo01.util.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai-question")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AiQuestionController {

    private final AiQuestionService aiQuestionService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    /**
     * 保存 AI 出题记录
     * POST /api/ai-question/save
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveAiQuestionRecord(
            @RequestBody Map<String, String> requestData,
            HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 从请求中获取数据
            String transcriptionContent = requestData.get("transcriptionContent");
            String customRemark = requestData.get("customRemark");
            String generatedQuestion = requestData.get("generatedQuestion");

            // 验证不能为空的字段
            if (transcriptionContent == null || transcriptionContent.isEmpty()) {
                response.put("success", false);
                response.put("message", "转写内容不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            if (generatedQuestion == null || generatedQuestion.isEmpty()) {
                response.put("success", false);
                response.put("message", "AI 生成的题目不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            // 获取当前用户
            String username = getCurrentUsername(request);

            if (username == null) {
                response.put("success", false);
                response.put("message", "请先登录");
                return ResponseEntity.badRequest().body(response);
            }

            System.out.println("保存 AI 出题记录，用户：" + username);

            // 保存到数据库
            AiQuestionRecord record = aiQuestionService.saveQuestionRecord(
                    username,
                    transcriptionContent,
                    customRemark,
                    generatedQuestion
            );

            response.put("success", true);
            response.put("message", "保存成功");
            response.put("recordId", record.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("保存 AI 出题记录失败：" + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "保存失败：" + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取当前用户的出题记录列表
     * GET /api/ai-question/records
     */
    @GetMapping("/records")
    public ResponseEntity<Map<String, Object>> getUserRecords(HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 获取当前用户
            String username = getCurrentUsername(request);

            if (username == null) {
                response.put("success", false);
                response.put("message", "请先登录");
                return ResponseEntity.badRequest().body(response);
            }

            System.out.println("查询用户 " + username + " 的 AI 出题记录");

            // 查询记录
            List<AiQuestionRecord> records = aiQuestionService.getUserRecords(username);
            long totalCount = aiQuestionService.countUserRecords(username);

            response.put("success", true);
            response.put("records", records);
            response.put("totalCount", totalCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取 AI 出题记录失败：" + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "获取记录失败：" + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 删除单条记录
     * DELETE /api/ai-question/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteRecord(
            @PathVariable Long id,
            HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 获取当前用户
            String username = getCurrentUsername(request);

            if (username == null) {
                response.put("success", false);
                response.put("message", "请先登录");
                return ResponseEntity.badRequest().body(response);
            }

            // 验证记录是否属于当前用户
            AiQuestionRecord record = aiQuestionService.getRecordById(id);
            if (record == null) {
                response.put("success", false);
                response.put("message", "记录不存在");
                return ResponseEntity.badRequest().body(response);
            }

            if (!record.getUsername().equals(username)) {
                response.put("success", false);
                response.put("message", "无权删除他人的记录");
                return ResponseEntity.badRequest().body(response);
            }

            // 删除记录
            aiQuestionService.deleteRecord(id);

            response.put("success", true);
            response.put("message", "删除成功");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("删除 AI 出题记录失败：" + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "删除失败：" + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 从请求中获取当前用户名（优先从 Token，其次从 Session）
     */
    private String getCurrentUsername(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        // 尝试从 Token 中获取用户名
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                if (jwtTokenUtil.validateToken(token)) {
                    return jwtTokenUtil.getUsernameFromToken(token);
                }
            } catch (Exception e) {
                System.err.println("Token 解析失败：" + e.getMessage());
            }
        }

        // 尝试从 Session 中获取
        User currentUser = (User) request.getSession().getAttribute("currentUser");
        if (currentUser != null) {
            return currentUser.getUsername();
        }

        return null;
    }
}

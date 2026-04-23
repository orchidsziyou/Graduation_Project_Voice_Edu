package com.example.demo01.controller;

import com.example.demo01.model.TranscriptionRecord;
import com.example.demo01.model.User;
import com.example.demo01.service.FileTranscriptionService;
import com.example.demo01.util.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transcription")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class TranscriptionController {
    
    private final FileTranscriptionService fileTranscriptionService;
    
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    
    /**
     * 获取当前用户的转写记录列表
     */
    @GetMapping("/records")
    public ResponseEntity<Map<String, Object>> getUserTranscriptionRecords(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long userId = getUserIdFromToken(authHeader);
            
            if (userId == null) {
                response.put("success", false);
                response.put("message", "请先登录");
                return ResponseEntity.badRequest().body(response);
            }
            
            List<TranscriptionRecord> records = fileTranscriptionService.getUserTranscriptionRecords(userId);
            long totalCount = fileTranscriptionService.countUserTranscriptionRecords(userId);
            
            response.put("success", true);
            response.put("records", records);
            response.put("totalCount", totalCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取转写记录失败：" + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取单个转写记录详情
     */
    @GetMapping("/records/{id}")
    public ResponseEntity<Map<String, Object>> getTranscriptionRecordDetail(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long userId = getUserIdFromToken(authHeader);
            
            if (userId == null) {
                response.put("success", false);
                response.put("message", "请先登录");
                return ResponseEntity.badRequest().body(response);
            }
            
            response.put("success", true);
            response.put("record", new HashMap<>());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取记录详情失败：" + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 保存本地转写记录（用于 VoskTest 页面手动保存）
     */
    @PostMapping("/save-local")
    public ResponseEntity<Map<String, Object>> saveLocalTranscriptionRecord(
            @RequestBody Map<String, Object> requestData,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long userId = getUserIdFromToken(authHeader);
            
            if (userId == null) {
                response.put("success", false);
                response.put("message", "请先登录");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取请求数据
            String transcriptionText = (String) requestData.get("transcriptionText");
            String fileName = (String) requestData.get("fileName");
            Long fileSize = ((Number) requestData.get("fileSize")).longValue();
            
            // 验证数据
            if (transcriptionText == null || transcriptionText.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "转写内容不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 保存转写记录
            fileTranscriptionService.saveLocalTranscriptionRecord(userId, transcriptionText, fileName, fileSize);
            
            response.put("success", true);
            response.put("message", "保存成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "保存失败：" + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(response);
        }
    }

        /**
     * 根据订单号获取转写结果
     */
    @PostMapping("/get-result-by-orderid")
    public ResponseEntity<Map<String, Object>> getResultByOrderId(
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "请先登录");
                return ResponseEntity.badRequest().body(response);
            }

            String orderId = request.get("orderId");
            if (orderId == null || orderId.isEmpty()) {
                response.put("success", false);
                response.put("message", "订单号不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            // 调用服务层获取结果（这里会自动处理轮询逻辑）
            String result = fileTranscriptionService.getTranscriptionResult(orderId);

            response.put("success", true);
            response.put("result", result);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取结果失败：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    
    /**
     * 从 JWT token 中获取用户 ID
     */
    private Long getUserIdFromToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                System.out.println("开始解析 Token...");
                System.out.println("Token: " + token.substring(0, Math.min(50, token.length())) + "...");
                
                if (jwtTokenUtil.validateToken(token)) {
                    Long userId = jwtTokenUtil.getUserIdFromToken(token);
                    String username = jwtTokenUtil.getUsernameFromToken(token);
                    System.out.println("Token 解析成功！用户名：" + username + ", 用户 ID: " + userId);
                    return userId;
                } else {
                    System.err.println("Token 验证失败！");
                }
            } catch (Exception e) {
                System.err.println("Token 解析异常：" + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("Authorization header 不存在或格式不正确：" + authHeader);
        }
        return null;
    }
}
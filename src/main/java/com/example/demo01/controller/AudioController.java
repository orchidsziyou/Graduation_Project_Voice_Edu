package com.example.demo01.controller;

import com.example.demo01.service.FileTranscriptionService;
import com.example.demo01.model.User;
import com.example.demo01.util.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/audio")
@RequiredArgsConstructor
public class AudioController {
    
    private final FileTranscriptionService fileTranscriptionService;
    
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    
    @Value("${file.upload.path:D:/Homework/Final_Project/uploads}")
    private String uploadPath;
    
    /**
     * 上传音频文件进行转写
     */
    @PostMapping("/transcribe")
    public ResponseEntity<?> transcribeAudio(
            @RequestParam("file") MultipartFile file, 
            @RequestParam(value = "duration", required = false) Long duration,
            HttpSession session,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // 验证文件
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("文件不能为空");
            }
                
            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !isValidAudioType(contentType)) {
                return ResponseEntity.badRequest().body("不支持的音频格式");
            }
                
            // 获取当前登录用户 ID（优先从 token 中获取）
            Long userId = getUserIdFromToken(authHeader);
                            
            // 如果 token 解析失败，尝试从 session 中获取
            if (userId == null) {
                User currentUser = (User) session.getAttribute("currentUser");
                if (currentUser != null) {
                    userId = currentUser.getId();
                    log.info("从 Session 中获取到用户：{} (ID: {})", currentUser.getUsername(), userId);
                    System.out.println("从 Session 中获取到用户：" + currentUser.getUsername() + " (ID: " + userId + ")");
                }
            }
                
            // 立即读取文件内容，避免临时文件问题
            byte[] fileContent;
            try (java.io.InputStream inputStream = file.getInputStream()) {
                fileContent = inputStream.readAllBytes();
            }
            
            // 保存文件到磁盘
            String savedFilePath = saveUploadedFile(file, fileContent);
            if (savedFilePath != null) {
                log.info("文件已保存到: {}", savedFilePath);
                System.out.println("文件已保存到: " + savedFilePath);
            }
                
            String result;
            if (userId != null) {
                // 用户已登录，调用带用户 ID 的转写服务
                result = fileTranscriptionService.transcribeAudioContentWithUser(
                    file.getOriginalFilename(), 
                    file.getSize(), 
                    fileContent,
                    userId,
                    duration  // 传递前端提供的时长
                );
            } else {
                // 用户未登录，调用普通转写服务
                result = fileTranscriptionService.transcribeAudioContent(
                    file.getOriginalFilename(), 
                    file.getSize(), 
                    fileContent,
                    duration  // 传递前端提供的时长
                );
            }
                
            // 返回 JSON 格式的响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("result", result);
            response.put("fileName", file.getOriginalFilename());
            response.put("fileSize", file.getSize());
            if (duration != null) {
                response.put("duration", duration);
                System.out.println("使用前端提供的时长：" + duration + " ms");
            }
            if (userId != null) {
                response.put("userId", userId);
            }
                            
            return ResponseEntity.ok(response);
                
        } catch (Exception e) {
            log.error("音频转写失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("转写失败：" + e.getMessage());
        }
    }
    
    /**
     * 查询转写结果
     */
    @GetMapping("/result/{orderId}")
    public ResponseEntity<?> getTranscriptionResult(@PathVariable String orderId) {
        try {
            String result = fileTranscriptionService.getTranscriptionResult(orderId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("获取转写结果失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取结果失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证音频文件类型
     */
    private boolean isValidAudioType(String contentType) {
        return contentType.startsWith("audio/") || 
               contentType.equals("application/octet-stream");
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
    
    /**
     * 保存上传的文件到磁盘
     */
    private String saveUploadedFile(MultipartFile file, byte[] fileContent) {
        try {
            // 确保上传目录存在
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                System.out.println("创建上传目录: " + uploadPath);
            }
            
            // 生成唯一文件名 (避免重名覆盖)
            String originalFileName = file.getOriginalFilename();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
            String extension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String newFileName = timestamp + "_" + originalFileName;
            
            // 构建完整路径
            Path filePath = uploadDir.resolve(newFileName);
            
            // 保存文件
            Files.write(filePath, fileContent);
            
            System.out.println("   文件保存成功:");
            System.out.println("   原始文件名: " + originalFileName);
            System.out.println("   保存文件名: " + newFileName);
            System.out.println("   文件大小: " + fileContent.length + " bytes");
            System.out.println("   保存路径: " + filePath.toAbsolutePath());
            
            return filePath.toAbsolutePath().toString();
            
        } catch (IOException e) {
            System.err.println("  文件保存失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
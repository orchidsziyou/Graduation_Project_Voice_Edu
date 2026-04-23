package com.example.demo01.controller;

import com.example.demo01.config.Utf8ConfigLoader;
import com.example.demo01.service.AiService;
import com.example.demo01.service.FileTranscriptionService;
import com.example.demo01.service.VoskSpeechService;
import com.example.demo01.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private AiService aiService;
    
    @Autowired
    private FileTranscriptionService fileTranscriptionService;
    
    @Autowired
    private VoskSpeechService voskSpeechService;
    
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    
    @Value("${file.upload.path:D:/Homework/Final_Project/uploads}")
    private String uploadPath;
    
    // 语音识别提示器配置
    // 从配置文件当中读取
    //private String speechRecognitionPromptPrefix = "你是一个评论员，你需要从后面我发送的内容里面，猜测我当时是什么心情，并且返回结果：";
//    @Component
//    @ConfigurationProperties(prefix = "speech.recognition")
//    @Data
//    public static class SpeechRecognitionConfig {
//        private String promptPrefix;
//    }
//
//    @Autowired
//    private SpeechRecognitionConfig speechRecognitionPromptPrefix;
    // 使用UTF-8配置加载器获取语音识别提示词
    private String speechRecognitionPromptPrefix;

    /**
     * 获取对话历史
     */
    @GetMapping("/chat/history")
    public ResponseEntity<Map<String, Object>> getChatHistory(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            String sessionId = session.getId();
            List<AiService.ChatMessage> history = aiService.getSessionHistory(sessionId);
            response.put("success", true);
            response.put("history", history);
            response.put("sessionId", sessionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取历史失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 发送聊天消息
     */
    @PostMapping("/chat/send")
    public ResponseEntity<Map<String, Object>> sendChatMessage(
            @RequestBody Map<String, String> requestBody,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String userMessage = requestBody.get("message");
            if (userMessage == null || userMessage.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "消息内容不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            String sessionId = session.getId();
            String aiResponse = aiService.continuousChat(sessionId, userMessage.trim());
            
            response.put("success", true);
            response.put("response", aiResponse);
            response.put("userMessage", userMessage);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "发送失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 清除对话历史
     */
    @PostMapping("/chat/clear")
    public ResponseEntity<Map<String, Object>> clearChatHistory(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            String sessionId = session.getId();
            aiService.clearSession(sessionId);
            response.put("success", true);
            response.put("message", "对话历史已清除");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "清除失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取语音识别配置
     */
    @GetMapping("/config/speech-recognition")
    public ResponseEntity<Map<String, Object>> getSpeechRecognitionConfig() {
        Map<String, Object> response = new HashMap<>();
        speechRecognitionPromptPrefix = Utf8ConfigLoader.getSpeechPromptPrefix();
        try {
            response.put("success", true);
//            System.out.println("语音识别提示词: " + speechRecognitionPromptPrefix);
            response.put("promptPrefix", speechRecognitionPromptPrefix);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取配置失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 语音识别接口
     * 直接返回语音识别结果文本，不包含任何 HTML 或页面元素
     * 云端识别的接口
     */
    @PostMapping("/speech/transcribe")
    public ResponseEntity<Map<String, Object>> transcribeSpeech(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "duration", required = false) Long duration,
            @RequestParam(value = "userId", required = false) Long userId,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 检查文件是否为空
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "请选择音频文件");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 检查文件类型是否为音频类型
            String fileName = file.getOriginalFilename().toLowerCase();
            if (!(fileName.endsWith(".mp3") || fileName.endsWith(".wav") || 
                  fileName.endsWith(".m4a") || fileName.endsWith(".flac"))) {
                response.put("success", false);
                response.put("message", "只支持MP3、WAV、M4A、FLAC格式的音频文件");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 读取文件内容
            byte[] fileContent = file.getInputStream().readAllBytes();
            
            // 如果没有通过参数传递userId，则从session中获取
            if (userId == null) {
                Object currentUserObj = session.getAttribute("currentUser");
                if (currentUserObj != null) {
                    userId = ((com.example.demo01.model.User) currentUserObj).getId();
                    System.out.println("从session获取用户ID: " + userId);
                }
            }
            
            String transcriptionResult;//转写结果
            if (userId != null) {
                // 调用带用户 ID 的转写服务
                transcriptionResult = fileTranscriptionService.transcribeAudioContentWithUser(
                    file.getOriginalFilename(), 
                    file.getSize(), 
                    fileContent,
                    userId,
                    duration  // 传递前端提供的时长
                );
                System.out.println("已登录用户转写完成，用户 ID: " + userId);
            } else {
                // 用户未登录，调用普通转写服务
                transcriptionResult = fileTranscriptionService.transcribeAudioContent(
                    file.getOriginalFilename(), 
                    file.getSize(), 
                    fileContent,
                    duration  // 传递前端提供的时长
                );
                System.out.println("匿名用户转写完成");
            }
            
            // 返回结果
            response.put("success", true);
            response.put("result", transcriptionResult);
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
            System.err.println("语音转写API异常: " + e.getMessage());
            e.printStackTrace();
            
            response.put("success", false);
            response.put("message", "转写失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 本地 Vosk 语音识别接口
     * 使用离线模型进行语音转文字，无需网络连接
     */
    @PostMapping("/speech/transcribe-local")
    public ResponseEntity<Map<String, Object>> transcribeSpeechLocal(
            @RequestParam("file") MultipartFile file,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 检查文件是否为空
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "请选择音频文件");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 检查文件类型是否为wav类型，前端也已经过滤过一次了
            String fileName = file.getOriginalFilename().toLowerCase();
            if (!fileName.endsWith(".wav")) {
                response.put("success", false);
                response.put("message", "本地识别仅支持 WAV 格式\n\n 提示:\n- 请使用 Windows 录音机录制 WAV 文件\n- 或使用在线工具将 MP3 转换为 WAV\n- 推荐采样率: 16kHz, 单声道, 16-bit");
                return ResponseEntity.badRequest().body(response);
            }
            
            System.out.println("开始本地 Vosk 识别: " + fileName);
            
            // 读取文件内容
            byte[] audioData = file.getInputStream().readAllBytes();
            
            // 保存文件到磁盘
            String savedFilePath = saveUploadedFile(file, audioData);
            if (savedFilePath != null) {
                System.out.println("文件已保存到: " + savedFilePath);
            }
            // 调用 Vosk 服务进行识别
            String result = voskSpeechService.recognize(audioData);
            System.out.println("本地识别完成，结果长度: " + result.length());
            
            // 注意：不再在后端自动保存转写记录
            // 前端会在转写成功后手动调用 /api/transcription/save-local 接口保存
            
            response.put("success", true);
            response.put("result", result);
            response.put("fileName", fileName);
            response.put("fileSize", file.getSize());
            response.put("mode", "local-vosk");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("本地语音识别异常: " + e.getMessage());
            e.printStackTrace();
            
            response.put("success", false);
            response.put("message", "本地识别失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
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
            
            // 生成唯一文件名，为了避免重复，用时间+原始文件名作为上传的文件名
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
            System.err.println("文件保存失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
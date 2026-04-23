package com.example.demo01.controller;

import com.example.demo01.service.KeywordExtractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 关键词提取控制器
 */
@RestController
@RequestMapping("/api/keyword")
@CrossOrigin(origins = "http://localhost:5173")
public class KeywordExtractionController {

    @Autowired
    private KeywordExtractionService keywordExtractionService;

    /**
     * 提取关键词
     * 
     * @param requestBody 包含 text 和 topN 的请求体
     * @return 关键词列表
     */
    @PostMapping("/extract")
    public ResponseEntity<Map<String, Object>> extractKeywords(@RequestBody Map<String, Object> requestBody) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String text = (String) requestBody.get("text");
            Integer topN = requestBody.get("topN") != null ? 
                ((Number) requestBody.get("topN")).intValue() : 10;
            
            if (text == null || text.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "文本内容不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 限制 topN 范围
            if (topN <= 0 || topN > 50) {
                topN = 10;
            }
            
            List<Map<String, Object>> keywords = keywordExtractionService.extractKeywords(text, topN);
            
            response.put("success", true);
            response.put("keywords", keywords);
            response.put("count", keywords.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "关键词提取失败：" + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 提取关键短语
     * 
     * @param requestBody 包含 text 和 topN 的请求体
     * @return 关键短语列表
     */
    @PostMapping("/extract-phrases")
    public ResponseEntity<Map<String, Object>> extractKeyPhrases(@RequestBody Map<String, Object> requestBody) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String text = (String) requestBody.get("text");
            Integer topN = requestBody.get("topN") != null ? 
                ((Number) requestBody.get("topN")).intValue() : 5;
            
            if (text == null || text.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "文本内容不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            List<String> phrases = keywordExtractionService.extractKeyPhrases(text, topN);
            
            response.put("success", true);
            response.put("phrases", phrases);
            response.put("count", phrases.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "关键短语提取失败：" + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 文本分词
     * 
     * @param requestBody 包含 text 的请求体
     * @return 分词结果
     */
    @PostMapping("/segment")
    public ResponseEntity<Map<String, Object>> segmentText(@RequestBody Map<String, Object> requestBody) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String text = (String) requestBody.get("text");
            
            if (text == null || text.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "文本内容不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            List<Map<String, String>> segments = keywordExtractionService.segmentText(text);
            
            response.put("success", true);
            response.put("segments", segments);
            response.put("count", segments.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "分词失败：" + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }
}

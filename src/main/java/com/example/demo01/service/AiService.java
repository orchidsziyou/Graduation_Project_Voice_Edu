package com.example.demo01.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.model.output.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiService {

    @Value("${langchain4j.default-model-provider:dashscope}")
    private String defaultModelProvider;

    @Value("${langchain4j.ollama.chat-model.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${langchain4j.ollama.chat-model.model-name:llama2}")
    private String ollamaModelName;

    @Value("${langchain4j.open-ai.chat-model.api-key:}")
    private String openAiApiKey;

    @Value("${langchain4j.open-ai.chat-model.model-name:gpt-3.5-turbo}")
    private String openAiModelName;

    @Value("${langchain4j.dashscope.chat-model.api-key:}")
    private String dashscopeApiKey;

    @Value("${langchain4j.dashscope.chat-model.model-name:qwen-plus}")
    private String dashscopeModelName;

    private ChatLanguageModel chatModel;
    
    // 会话存储
    private final Map<String, List<ChatMessage>> sessionContexts = new ConcurrentHashMap<>();
    
    // 聊天消息类
    public static class ChatMessage {
        private String role; // "user" 或 "assistant"
        private String content;
        private long timestamp;
        
        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
        }
        
        // getter和setter方法
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        @Override
        public String toString() {
            return role + ": " + content;
        }
    }


    //初始化
    @PostConstruct
    public void init() {
        //当前默认使用通义千问
        if ("openai".equalsIgnoreCase(defaultModelProvider) && !openAiApiKey.isEmpty()) {
            // 使用OpenAI
            chatModel = OpenAiChatModel.builder()
                    .apiKey(openAiApiKey)
                    .modelName(openAiModelName)
                    .temperature(0.7)
                    .build();
        } else if ("dashscope".equalsIgnoreCase(defaultModelProvider) && !dashscopeApiKey.isEmpty()) {
            // 使用千问(通义千问)
            chatModel = QwenChatModel.builder()
                    .apiKey(dashscopeApiKey)
                    .modelName(dashscopeModelName)
                    .temperature(0.7f)
                    .build();
        } else {
            // 默认使用Ollama本地模型
            chatModel = OllamaChatModel.builder()
                    .baseUrl(ollamaBaseUrl)
                    .modelName(ollamaModelName)
                    .temperature(0.7)
                    .build();
        }
    }

    /**
     * 简单的聊天对话
     */
    public String chat(String message) {
        try {
            return chatModel.generate(message);
        } catch (Exception e) {
            return "AI服务暂时不可用: " + e.getMessage();
        }
    }
    
    /**·
     * 持续对话 - 带会话ID的上下文对话
     * @param sessionId 会话ID
     * @param userMessage 用户消息
     * @return AI回复
     */
    public String continuousChat(String sessionId, String userMessage) {
        try {
            // 获取或创建会话上下文
            List<ChatMessage> context = sessionContexts.computeIfAbsent(sessionId, k -> new ArrayList<>());
            // 添加用户消息到上下文
            context.add(new ChatMessage("user", userMessage));
            // 构建带上下文的提示词
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("你是一个智能AI助手，请根据之前的对话历史来回答用户的问题。\n\n");
            promptBuilder.append("对话历史:\n");
            
            // 限制上下文长度，只保留最近20条消息
            int startIndex = Math.max(0, context.size() - 20);
            for (int i = startIndex; i < context.size(); i++) {
                ChatMessage msg = context.get(i);
                promptBuilder.append(msg.toString()).append("\n");
            }
            
            promptBuilder.append("\n用户最新问题: ").append(userMessage);
            promptBuilder.append("\n\n请回答:");
            
            String prompt = promptBuilder.toString();
            // 调用AI模型
            String response = chatModel.generate(prompt);
            // 添加AI回复到上下文
            context.add(new ChatMessage("assistant", response));
            // 限制上下文大小，防止内存溢出
            if (context.size() > 50) {
                context.subList(0, 20).clear(); // 删除前20条消息
            }
            
            return response;
            
        } catch (Exception e) {
            return "AI服务暂时不可用: " + e.getMessage();
        }
    }
    
    /**
     * 获取会话历史
     * @param sessionId 会话ID
     * @return 对话历史列表
     */
    public List<ChatMessage> getSessionHistory(String sessionId) {
        return sessionContexts.getOrDefault(sessionId, new ArrayList<>());
    }
    
    /**
     * 清除会话历史
     * @param sessionId 会话ID
     */
    public void clearSession(String sessionId) {
        sessionContexts.remove(sessionId);
    }
    
    /**
     * 获取所有活跃会话数量
     * @return 会话数量
     */
    public int getActiveSessionsCount() {
        return sessionContexts.size();
    }

    /**
     * 带上下文的对话
     * 类似Python中的: response = llm.invoke(messages=[{"role": "user", "content": "你好"}])
     */
    public String chatWithContext(String userMessage, String context) {
        String prompt = context + "\n\n用户问题: " + userMessage;
        return chat(prompt);
    }

    /**
     * 根据提供的内容智能化生成题目
     */
    public String generateQuestionFromContent(String context){
//        System.out.println("=== AI题目生成开始 ===");
//        System.out.println("原始输入内容: " + context);
//        System.out.println("输入内容长度: " + (context != null ? context.length() : 0));
        
        String PromptWords = "";
        String prompt = context;
        
//        System.out.println("构造的完整提示词: " + prompt);
//        System.out.println("提示词长度: " + prompt.length());
        
        try {
            String result = chat(prompt);
//            System.out.println("AI返回结果: " + result);
//            System.out.println("AI结果长度: " + (result != null ? result.length() : 0));
//            System.out.println("=== AI题目生成结束 ===");
            return result;
        } catch (Exception e) {
            System.err.println("AI生成题目时发生异常: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
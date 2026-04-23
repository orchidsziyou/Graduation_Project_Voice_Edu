package com.example.demo01.service;

import com.example.demo01.util.AudioConverter;
import com.example.demo01.util.JaveAudioConverter;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Vosk 离线语音识别服务
 * 使用 Vosk 引擎进行本地语音转文字
 */
@Service
public class VoskSpeechService {

    private Model model;
    
    @Value("${vosk.model.path}")
    private String modelPathConfig;

    /**
     * 初始化 Vosk 模型
     */
    @PostConstruct
    public void init() {
        try {
//            System.out.println("========== Vosk 初始化诊断 ==========");
//            System.out.println("Java 版本: " + System.getProperty("java.version"));
//            System.out.println("操作系统: " + System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")");
//
            // 检查 Vosk 库是否加载
            try {
                System.out.println("尝试加载 LibVosk...");
                LibVosk.setLogLevel(LogLevel.WARNINGS);
                System.out.println(" LibVosk 加载成功");
            } catch (UnsatisfiedLinkError e) {
                System.err.println(" LibVosk 加载失败: " + e.getMessage());
                System.err.println("原因: 缺少 vosk.dll 或依赖库");
                throw e;
            }
            
            System.out.println("\n正在加载 Vosk 中文模型...");
            
            // 优先从 src 目录加载（开发环境）
            String modelPath = detectModelPath();
            
            if (modelPath == null) {
                throw new IOException("找不到模型目录，请确保已下载中文模型");
            }
            
            System.out.println("模型路径: " + modelPath);
            
            // 验证模型目录是否存在且包含必要文件
            java.io.File modelDir = new java.io.File(modelPath);
            if (!modelDir.exists() || !modelDir.isDirectory()) {
                throw new IOException("模型目录不存在: " + modelPath);
            }
            
            // 检查关键子目录
            String[] requiredDirs = {"am", "conf", "graph", "ivector"};
            for (String dir : requiredDirs) {
                java.io.File subDir = new java.io.File(modelDir, dir);
                if (!subDir.exists()) {
                    System.err.println("警告: 缺少子目录 " + dir);
                }
            }
            
            // 加载模型
//            System.out.println("开始创建 Vosk Model 对象...");
//            System.out.println("当前可用内存: " + String.format("%.2f MB", Runtime.getRuntime().freeMemory() / (1024.0 * 1024.0)));
//            System.out.println("最大可用内存: " + String.format("%.2f MB", Runtime.getRuntime().maxMemory() / (1024.0 * 1024.0)));
//
            // 检查模型文件大小
            long modelSize = 0;
            int fileCount = 0;
            java.io.File modelDirCheck = new java.io.File(modelPath);
            for (java.io.File subDir : modelDirCheck.listFiles()) {
                if (subDir.isDirectory()) {
                    for (java.io.File file : subDir.listFiles()) {
                        modelSize += file.length();
                        fileCount++;
                    }
                }
            }
            System.out.println("模型文件统计: " + fileCount + " 个文件, 总大小: " + String.format("%.2f MB", modelSize / (1024.0 * 1024.0)));
            
            // 检查关键文件是否存在
            java.io.File finalMdl = new java.io.File(modelPath, "am/final.mdl");
            System.out.println("关键文件 am/final.mdl 存在: " + finalMdl.exists());
            System.out.println("关键文件大小: " + String.format("%.2f MB", finalMdl.length() / (1024.0 * 1024.0)));
            
            try {
                model = new Model(modelPath);
                System.out.println("Vosk 中文模型加载成功");
            } catch (Exception e) {
                System.err.println("Model 构造函数抛出异常:");
                System.err.println("  异常类型: " + e.getClass().getName());
                System.err.println("  异常消息: " + e.getMessage());
                System.err.println("  根本原因: " + (e.getCause() != null ? e.getCause().getMessage() : "无"));
                throw e;
            }
        } catch (IOException e) {
            System.err.println("Vosk 模型加载失败: " + e.getMessage());
            System.err.println("请确保已下载中文模型并放置在: src/main/resources/models/vosk-model-cn");
            System.err.println("下载地址: https://alphacephei.com/vosk/models/vosk-model-cn-0.22.zip");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Vosk 初始化异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 检测模型路径（优先使用配置的绝对路径或相对路径）
     */
    private String detectModelPath() {
        // 使用配置文件中的路径
        if (modelPathConfig != null && !modelPathConfig.isEmpty()) {
            java.io.File dir = new java.io.File(modelPathConfig);
            if (!dir.isAbsolute()) {
                // 相对路径，转换为绝对路径
                dir = new java.io.File(System.getProperty("user.dir"), modelPathConfig);
            }
            if (dir.exists() && dir.isDirectory()) {
                System.out.println("从配置路径找到模型: " + dir.getAbsolutePath());
                return dir.getAbsolutePath();
            } else {
                System.err.println("配置的模型路径不存在: " + dir.getAbsolutePath());
            }
        }
        
        //从 classpath 读取（Maven 构建后打包在 JAR 中）
        try {
            java.net.URL modelUrl = getClass().getClassLoader().getResource("models/vosk-model-cn");
            if (modelUrl != null) {
                String path = modelUrl.getPath();
                path = java.net.URLDecoder.decode(path, "UTF-8");
                // Windows 路径处理
                if (path.startsWith("/") && path.length() > 2 && path.charAt(2) == ':') {
                    path = path.substring(1);
                }
                path = new java.io.File(path).getAbsolutePath();
                
                java.io.File dir = new java.io.File(path);
                if (dir.exists() && dir.isDirectory()) {
                    System.out.println("从 classpath 找到模型: " + path);
                    return path;
                }
            }
        } catch (Exception e) {
            System.err.println("从 classpath 加载模型失败: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 从 JAR 包中提取模型到临时目录
     */
    private String extractModelFromJar(java.net.URL modelUrl) throws IOException {
        // 创建临时目录
        java.io.File tempDir = new java.io.File(System.getProperty("java.io.tmpdir"), "vosk-model-cn");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        
        System.out.println("模型将解压到: " + tempDir.getAbsolutePath());
        
        // TODO: 如果需要从 JAR 包中提取，这里需要实现解压逻辑
        // 目前建议在生产环境使用外部模型路径
        throw new IOException("JAR 包模式暂未实现，请使用外部模型路径");
    }

    /**
     * 识别音频数据 (仅支持 WAV 格式，会自动转换为 16kHz 单声道)
     * 
     * @param audioData WAV 音频数据
     * @return 识别结果文本
     */
    public String recognize(byte[] audioData) {
        if (model == null) {
            return "错误: Vosk 模型未加载，请检查模型文件是否存在";
        }
        if (audioData == null || audioData.length == 0) {
            return "错误: 音频数据为空";
        }
        try {
            // 音频预处理 - 尝试使用 Java Sound API 转换
            System.out.println("开始音频预处理...");
            byte[] convertedAudio;
            try {
                convertedAudio = AudioConverter.convertToVoskFormat(audioData);
                System.out.println("音频预处理完成(Java Sound API)，原始大小: " + audioData.length +
                                 " -> 转换后: " + convertedAudio.length);
            } catch (Exception e) {
                // Java Sound API 失败，尝试使用 Jave2 (FFmpeg)
                System.out.println("Java Sound API 转换失败，尝试使用 Jave2 转换...");
                System.out.println("原因: " + e.getMessage());
                convertedAudio = JaveAudioConverter.convertToWav(audioData);
                System.out.println("Jave2 转换完成，原始大小: " + audioData.length +
                                 " -> 转换后: " + convertedAudio.length);
            }
            // 使用 Vosk 进行识别
            return recognizeInternal(convertedAudio);
            
        } catch (Exception e) {
            System.err.println("音频处理失败: " + e.getMessage());
            e.printStackTrace();
            return "识别失败: " + e.getMessage();
        }
    }
    
    /**
     * 内部识别方法（假设音频已经是正确格式）
     */
    private String recognizeInternal(byte[] audioData) {

        Recognizer recognizer = null;
        try {
            // 创建识别器，采样率 16000
            recognizer = new Recognizer(model, 16000);
            // 设置最大备选结果数
            recognizer.setMaxAlternatives(0);
            recognizer.setWords(false);
            recognizer.setPartialWords(true);
            // 识别结果存放处
            StringBuilder fullText = new StringBuilder();
            // 处理音频数据
            int offset = 0;
            int bufferSize = 4096;
            
            while (offset < audioData.length) {
                int remaining = audioData.length - offset;
                int chunkSize = Math.min(bufferSize, remaining);
                
                byte[] chunk = new byte[chunkSize];
                System.arraycopy(audioData, offset, chunk, 0, chunkSize);
                
                if (recognizer.acceptWaveForm(chunk, chunkSize)) {
                    // 有完整识别结果，累积到总结果中
                    String result = recognizer.getResult();
                    JSONObject json = new JSONObject(result);
                    if (json.has("text")) {
                        String text = json.getString("text");
                        if (text != null && !text.trim().isEmpty()) {
                            // 如果已有内容，添加逗号分隔
                            if (fullText.length() > 0) {
                                fullText.append("，");
                            }
                            fullText.append(text);
                        }
                    }
                }
                offset += chunkSize;
            }
            // 获取最终结果
            String finalResult = recognizer.getFinalResult();
            JSONObject json = new JSONObject(finalResult);
            if (json.has("text")) {
                String text = json.getString("text");
                if (text != null && !text.trim().isEmpty()) {
                    // 如果已有内容，添加逗号分隔
                    if (fullText.length() > 0) {
                        fullText.append("，");
                    }
                    fullText.append(text);
                    System.out.println("📝 最终片段: " + text);
                }
            }
            // 合并所有结果
            String completeText = fullText.toString();
            if (completeText != null && !completeText.isEmpty()) {
                // 去除空格
                completeText = completeText.replace(" ", "").trim();
                System.out.println("完整识别结果长度: " + completeText.length() + " 字符");
                return completeText;
            }
            
            return "";
            
        } catch (Exception e) {
            System.err.println("语音识别失败: " + e.getMessage());
            e.printStackTrace();
            return "识别失败: " + e.getMessage();
        } finally {
            if (recognizer != null) {
                recognizer.close();
            }
        }
    }

    /**
     * 释放资源
     */
    @PreDestroy
    public void destroy() {
        if (model != null) {
            model.close();
            System.out.println("Vosk 模型已释放");
        }
    }
}


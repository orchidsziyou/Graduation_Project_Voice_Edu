package com.example.demo01.config;

import org.springframework.stereotype.Component;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * UTF-8配置文件加载器
 * 专门处理中文配置的编码问题
 */
@Component
public class Utf8ConfigLoader {
    
    private static String speechPromptPrefix;
    
    @EventListener(ContextRefreshedEvent.class)
    public void loadUtf8Properties() {
        try {
            // 手动加载UTF-8编码的配置文件
            Properties props = new Properties();
            InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("application.properties");
            
            if (inputStream != null) {
                // 强制使用UTF-8读取
                InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
                props.load(reader);
                
                // 获取语音识别配置
                //已废弃，提示器写到前端当中了
                speechPromptPrefix = props.getProperty("speech.recognition.prompt-prefix");

                reader.close();
                inputStream.close();
            }
        } catch (Exception e) {
            System.err.println("UTF-8配置加载失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 获取语音识别提示词前缀
     */
    public static String getSpeechPromptPrefix() {
        return speechPromptPrefix;
    }
    
    /**
     * 重新加载配置（可用于运行时刷新）
     */
    public void reloadConfig() {
        loadUtf8Properties();
    }
}
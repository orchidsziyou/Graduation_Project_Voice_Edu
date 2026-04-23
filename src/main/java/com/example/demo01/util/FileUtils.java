package com.example.demo01.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文件处理工具类
 * 用于安全地处理MultipartFile，避免临时文件被清理的问题
 */
public class FileUtils {
    
    /**
     * 安全地读取MultipartFile的内容
     * 一次性读取所有字节，避免多次访问临时文件
     * 
     * @param file MultipartFile对象
     * @return 文件内容的字节数组
     * @throws IOException 读取失败时抛出
     */
    public static byte[] readFileContent(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        
        try (InputStream inputStream = file.getInputStream()) {
            return inputStream.readAllBytes();
        } catch (IOException e) {
            throw new IOException("读取文件内容失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 验证文件类型是否为音频文件
     * 
     * @param filename 文件名
     * @return 是否为音频文件
     */
    public static boolean isAudioFile(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }
        
        String lowerFileName = filename.toLowerCase();
        return lowerFileName.endsWith(".mp3") || 
               lowerFileName.endsWith(".wav") || 
               lowerFileName.endsWith(".m4a") || 
               lowerFileName.endsWith(".flac") ||
               lowerFileName.endsWith(".aac") ||
               lowerFileName.endsWith(".ogg");
    }
    
    /**
     * 格式化文件大小显示
     * 
     * @param sizeInBytes 文件大小（字节）
     * @return 格式化的文件大小字符串
     */
    public static String formatFileSize(long sizeInBytes) {
        if (sizeInBytes < 1024) {
            return sizeInBytes + " B";
        } else if (sizeInBytes < 1024 * 1024) {
            return String.format("%.2f KB", (double) sizeInBytes / 1024);
        } else {
            return String.format("%.2f MB", (double) sizeInBytes / (1024 * 1024));
        }
    }
}
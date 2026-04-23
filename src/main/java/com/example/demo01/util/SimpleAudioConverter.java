package com.example.demo01.util;

import javax.sound.sampled.*;
import java.io.*;

/**
 * 简单的音频转换工具
 * 将MP3转换为WAV格式
 */
public class SimpleAudioConverter {
    
    /**
     * 检查是否为WAV文件
     */
    public static boolean isWavFile(File file) {
        return file.getName().toLowerCase().endsWith(".wav");
    }
    
    /**
     * 简单的音频信息提取
     */
    public static String getAudioInfo(File file) {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
            AudioFormat format = audioInputStream.getFormat();
            long frameLength = audioInputStream.getFrameLength();
            float duration = frameLength / format.getFrameRate();
            
            String info = String.format(
                "格式: %s, 采样率: %.0f Hz, 位深度: %d bit, 声道: %d, 时长: %.2f 秒",
                format.getEncoding(),
                format.getSampleRate(),
                format.getSampleSizeInBits(),
                format.getChannels(),
                duration
            );
            
            audioInputStream.close();
            return info;
        } catch (Exception e) {
            return "无法读取音频信息: " + e.getMessage();
        }
    }
    
    /**
     * 创建一个简短的测试音频文件（用于验证）
     */
    public static File createTestAudio() {
        try {
            // 创建16kHz, 16位, 单声道的测试音频
            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
            byte[] testAudio = new byte[16000 * 2]; // 1秒的音频数据
            
            // 生成简单的测试音调
            for (int i = 0; i < testAudio.length; i += 2) {
                short sample = (short) (Math.sin(2 * Math.PI * 440 * i / 16000) * 10000);
                testAudio[i] = (byte) (sample & 0xFF);
                testAudio[i + 1] = (byte) ((sample >> 8) & 0xFF);
            }
            
            File tempFile = File.createTempFile("test_audio", ".wav");
            try (AudioInputStream ais = new AudioInputStream(
                    new ByteArrayInputStream(testAudio), format, testAudio.length / format.getFrameSize())) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, tempFile);
            }
            
            System.out.println("测试音频文件创建成功: " + tempFile.getAbsolutePath());
            return tempFile;
            
        } catch (Exception e) {
            System.err.println("创建测试音频失败: " + e.getMessage());
            return null;
        }
    }
}
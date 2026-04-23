package com.example.demo01.util;

import ws.schild.jave.*;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 使用 Jave2 进行音频格式转换
 * Jave2 是 FFmpeg 的 Java 封装，通过 Maven 依赖自动管理
 */
public class JaveAudioConverter {

    /**
     * 将任意格式音频转换为 WAV 格式（16kHz, 单声道, 16-bit PCM）
     * 
     * @param inputAudio 输入音频数据
     * @return 转换后的 WAV 数据
     * @throws Exception 转换失败
     */
    public static byte[] convertToWav(byte[] inputAudio) throws Exception {
        if (inputAudio == null || inputAudio.length == 0) {
            throw new IOException("输入音频数据为空");
        }

        // 创建临时文件
        Path tempDir = Files.createTempDirectory("jave_convert_");
        File inputFile = File.createTempFile("input_", ".tmp", tempDir.toFile());
        File outputFile = File.createTempFile("output_", ".wav", tempDir.toFile());
        
        try {
            // 写入输入文件
            Files.write(inputFile.toPath(), inputAudio);
            System.out.println("Jave2 输入文件大小: " + inputAudio.length + " bytes");
            
            // 配置音频属性
            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("pcm_s16le");      // PCM signed 16-bit little-endian
            audio.setBitRate(256000);          // 比特率
            audio.setChannels(1);              // 单声道
            audio.setSamplingRate(16000);      // 采样率 16kHz
            
            // 配置编码参数
            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat("wav");      // 输出格式 WAV
            attrs.setAudioAttributes(audio);
            
            // 执行转换
            Encoder encoder = new Encoder();
            MultimediaObject multimediaObject = new MultimediaObject(inputFile);
            
            System.out.println("开始 Jave2 音频转换...");
            encoder.encode(multimediaObject, outputFile, attrs);
            
            // 读取输出文件
            byte[] wavData = Files.readAllBytes(outputFile.toPath());
            System.out.println("Jave2 转换成功，WAV 大小: " + wavData.length + " bytes");
            
            return wavData;
            
        } catch (Exception e) {
            System.err.println("Jave2 转换失败: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            // 清理临时文件
            try {
                Files.deleteIfExists(inputFile.toPath());
                Files.deleteIfExists(outputFile.toPath());
                Files.deleteIfExists(tempDir);
            } catch (IOException e) {
                System.err.println("清理临时文件失败: " + e.getMessage());
            }
        }
    }
}

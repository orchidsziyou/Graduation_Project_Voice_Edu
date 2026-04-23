package com.example.demo01.util;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 音频转换工具类
 * 将任意格式的音频转换为 Vosk 要求的格式: 16kHz, 单声道, 16-bit PCM
 */
public class AudioConverter {

    /**
     * 将音频数据转换为 Vosk 要求的格式
     * 
     * @param audioData 原始音频数据
     * @return 转换后的 PCM 数据 (16kHz, mono, 16-bit)
     * @throws IOException 转换失败
     */
    public static byte[] convertToVoskFormat(byte[] audioData) throws IOException {
        if (audioData == null || audioData.length == 0) {
            throw new IOException("音频数据为空");
        }

        try {
            // 创建音频输入流
            ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
            AudioInputStream sourceStream = AudioSystem.getAudioInputStream(bais);
            
            AudioFormat sourceFormat = sourceStream.getFormat();
//            System.out.println("   原始音频格式:");
//            System.out.println("   采样率: " + sourceFormat.getSampleRate() + " Hz");
//            System.out.println("   声道数: " + sourceFormat.getChannels());
//            System.out.println("   位深度: " + sourceFormat.getSampleSizeInBits() + " bit");
//            System.out.println("   编码: " + sourceFormat.getEncoding());

            // 目标格式: 16kHz, 单声道, 16-bit, PCM signed, little-endian
            AudioFormat targetFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                16000.0f,  // 采样率 16kHz
                16,        // 16-bit
                1,         // 单声道
                2,         // 帧大小 (16-bit = 2 bytes)
                16000.0f,  // 帧率
                false      // little-endian
            );

            // 如果源格式已经是目标格式，直接返回
            if (isSameFormat(sourceFormat, targetFormat)) {
//                System.out.println(" 音频格式已符合要求，无需转换");
                return audioData;
            }

            // 检查是否支持转换
            if (!AudioSystem.isConversionSupported(targetFormat, sourceFormat)) {
                System.out.println("不支持直接转换，尝试通过中间格式转换...");
                // 尝试先转换为 PCM
                AudioFormat pcmFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    sourceFormat.getSampleRate(),
                    16,
                    sourceFormat.getChannels(),
                    sourceFormat.getChannels() * 2,
                    sourceFormat.getSampleRate(),
                    false
                );
                
                if (AudioSystem.isConversionSupported(pcmFormat, sourceFormat)) {
                    AudioInputStream pcmStream = AudioSystem.getAudioInputStream(pcmFormat, sourceStream);
                    AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, pcmStream);
                    return readAudioData(convertedStream);
                } else {
                    throw new IOException("无法将音频转换为目标格式");
                }
            }

            // 执行转换
            AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, sourceStream);
            byte[] convertedData = readAudioData(convertedStream);
            
            System.out.println(" 音频转换成功，输出大小: " + convertedData.length + " bytes");
            
            convertedStream.close();
            sourceStream.close();
            
            return convertedData;
            
        } catch (UnsupportedAudioFileException e) {
            System.err.println("不支持的音频格式: " + e.getMessage());
            System.err.println("提示: Java Sound API 支持的格式包括 WAV, AIFF, AU");
            System.err.println("已添加 MP3SPI 支持 MP3 格式，请确保依赖已正确加载");
            throw new IOException("不支持的音频格式: " + e.getMessage() + "\n建议使用 WAV 格式以获得最佳兼容性", e);
        } catch (Exception e) {
            System.err.println("音频转换失败: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("音频转换失败: " + e.getMessage(), e);
        }
    }

    /**
     * 读取音频流数据
     */
    private static byte[] readAudioData(AudioInputStream stream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        
        while ((bytesRead = stream.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        
        return baos.toByteArray();
    }

    /**
     * 判断两个音频格式是否相同
     */
    private static boolean isSameFormat(AudioFormat format1, AudioFormat format2) {
        return format1.getSampleRate() == format2.getSampleRate()
            && format1.getChannels() == format2.getChannels()
            && format1.getSampleSizeInBits() == format2.getSampleSizeInBits()
            && format1.getEncoding() == format2.getEncoding();
    }
}

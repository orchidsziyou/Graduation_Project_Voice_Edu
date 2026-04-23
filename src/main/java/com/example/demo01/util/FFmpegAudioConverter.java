package com.example.demo01.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * FFmpeg 音频转换工具
 * 使用项目内置的 FFmpeg 进行音频格式转换
 */
public class FFmpegAudioConverter {

    private static final String FFMPEG_DIR = "ffmpeg";
    private static String ffmpegPath = null;

    /**
     * 获取 FFmpeg 可执行文件路径
     * 如果不存在，从 resources 解压到临时目录
     */
    private static synchronized String getFFmpegPath() throws IOException {
        if (ffmpegPath != null && new File(ffmpegPath).exists()) {
            return ffmpegPath;
        }

        // 检测操作系统
        String os = System.getProperty("os.name").toLowerCase();
        String ffmpegName = os.contains("win") ? "ffmpeg.exe" : "ffmpeg";
        
        // 尝试从项目 resources 目录读取
        String resourcePath = FFMPEG_DIR + "/" + ffmpegName;
        InputStream ffmpegStream = FFmpegAudioConverter.class.getClassLoader()
            .getResourceAsStream(resourcePath);
        
        if (ffmpegStream == null) {
            throw new IOException("未找到 FFmpeg 可执行文件: " + resourcePath);
        }

        // 创建临时目录
        Path tempDir = Files.createTempDirectory("ffmpeg_");
        Path ffmpegFile = tempDir.resolve(ffmpegName);
        
        // 复制 FFmpeg 到临时目录
        Files.copy(ffmpegStream, ffmpegFile, StandardCopyOption.REPLACE_EXISTING);
        ffmpegStream.close();
        
        // 设置可执行权限（Linux/Mac）
        if (!os.contains("win")) {
            ffmpegFile.toFile().setExecutable(true);
        }
        
        ffmpegPath = ffmpegFile.toAbsolutePath().toString();
        System.out.println("FFmpeg 已解压到: " + ffmpegPath);
        
        return ffmpegPath;
    }

    /**
     * 将音频转换为 WAV 格式（16kHz, 单声道, 16-bit PCM）
     * 
     * @param inputAudio 输入音频数据（可以是任意格式）
     * @return 转换后的 WAV 数据
     * @throws IOException 转换失败
     */
    public static byte[] convertToWav(byte[] inputAudio) throws IOException {
        if (inputAudio == null || inputAudio.length == 0) {
            throw new IOException("输入音频数据为空");
        }

        String ffmpegExe = getFFmpegPath();
        
        // 创建临时文件
        Path tempDir = Files.createTempDirectory("ffmpeg_convert_");
        Path inputFile = tempDir.resolve("input.bin");
        Path outputFile = tempDir.resolve("output.wav");
        
        try {
            // 写入输入文件
            Files.write(inputFile, inputAudio);
            
            // 构建 FFmpeg 命令
            // -ar 16000: 采样率 16kHz
            // -ac 1: 单声道
            // -f wav: 输出 WAV 格式
            // -acodec pcm_s16le: PCM signed 16-bit little-endian
            ProcessBuilder processBuilder = new ProcessBuilder(
                ffmpegExe,
                "-y",              // 覆盖输出文件
                "-i", inputFile.toString(),  // 输入文件
                "-ar", "16000",    // 采样率 16kHz
                "-ac", "1",        // 单声道
                "-f", "wav",       // 输出格式 WAV
                "-acodec", "pcm_s16le",  // 编码器
                outputFile.toString()
            );
            
            // 重定向错误流到标准输出
            processBuilder.redirectErrorStream(true);
            
            // 启动进程
            Process process = processBuilder.start();
            
            // 读取 FFmpeg 输出
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            // 等待进程结束
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                System.err.println("FFmpeg 转换失败，退出码: " + exitCode);
                System.err.println("FFmpeg 输出:\n" + output.toString());
                throw new IOException("FFmpeg 转换失败，退出码: " + exitCode);
            }
            
            // 读取输出文件
            byte[] wavData = Files.readAllBytes(outputFile);
            System.out.println("FFmpeg 转换成功，WAV 大小: " + wavData.length + " bytes");
            
            return wavData;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("FFmpeg 转换被中断", e);
        } finally {
            // 清理临时文件
            try {
                Files.deleteIfExists(inputFile);
                Files.deleteIfExists(outputFile);
                Files.deleteIfExists(tempDir);
            } catch (IOException e) {
                System.err.println("清理临时文件失败: " + e.getMessage());
            }
        }
    }
}

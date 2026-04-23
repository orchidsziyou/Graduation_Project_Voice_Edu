package com.example.demo01.service;

import com.example.demo01.config.XfyunConfig;
import com.example.demo01.model.TranscriptionRecord;
import com.example.demo01.repository.TranscriptionRecordRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.sound.sampled.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Base64;
import java.util.stream.Collectors;

@Service
public class FileTranscriptionService {

    @Autowired
    private XfyunConfig xfyunConfig;
    
    @Autowired
    private TranscriptionRecordRepository transcriptionRecordRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String lastBaseString;
    private String lastSignature;

    /**
     * 上传音频文件并获取转写结果
     */
    public String transcribeAudio(byte[] fileContent, String fileName, long fileSize) throws Exception {
        //上传音频文件获取订单 ID
        String orderId = uploadAudioFile(fileContent, fileName, fileSize, null);
            
        //获取转写结果
        return pollForResult(orderId);
    }
        
    /**
     * 上传音频文件并获取转写结果（带时长参数）
     */
    public String transcribeAudio(byte[] fileContent, String fileName, long fileSize, Long duration) throws Exception {
        //上传音频文件获取订单 ID
        String orderId = uploadAudioFile(fileContent, fileName, fileSize, duration);
            
        //获取转写结果
        return pollForResult(orderId);
    }
    
    /**
     * 上传音频文件并获取转写结果（带用户 ID 版本）
     */
    public String transcribeAudioWithUser(byte[] fileContent, String fileName, long fileSize, Long userId) throws Exception {
        //上传音频文件获取订单 ID
        String orderId = uploadAudioFile(fileContent, fileName, fileSize, null);
            
        //保存转写记录到数据库
        saveTranscriptionRecord(userId, orderId, fileName, fileSize);
            
        //轮询获取转写结果
        return pollForResult(orderId);
    }
        
    /**
     * 上传音频文件并获取转写结果（带用户 ID 和时长参数）
     */
    public String transcribeAudioWithUser(byte[] fileContent, String fileName, long fileSize, Long userId, Long duration) throws Exception {
        //上传音频文件获取订单 ID
        String orderId = uploadAudioFile(fileContent, fileName, fileSize, duration);
            
        //保存转写记录到数据库
        saveTranscriptionRecord(userId, orderId, fileName, fileSize);
            
        //轮询获取转写结果
        return pollForResult(orderId);
    }
    
    /**
     * 上传音频文件内容并获取转写结果（直接传入文件内容）
     */
    public String transcribeAudioContent(String fileName, long fileSize, byte[] fileContent) throws Exception {
        return transcribeAudio(fileContent, fileName, fileSize);
    }
        
    /**
     * 上传音频文件内容并获取转写结果（带时长参数）
     */
    public String transcribeAudioContent(String fileName, long fileSize, byte[] fileContent, Long duration) throws Exception {
        return transcribeAudio(fileContent, fileName, fileSize, duration);
    }
        
    /**
     * 上传音频文件内容并获取转写结果（带用户 ID 版本）
     */
    public String transcribeAudioContentWithUser(String fileName, long fileSize, byte[] fileContent, Long userId) throws Exception {
        return transcribeAudioWithUser(fileContent, fileName, fileSize, userId);
    }
        
    /**
     * 上传音频文件内容并获取转写结果（带用户 ID 和时长参数）
     */
    public String transcribeAudioContentWithUser(String fileName, long fileSize, byte[] fileContent, Long userId, Long duration) throws Exception {
        return transcribeAudioWithUser(fileContent, fileName, fileSize, userId, duration);
    }

    /**
     * 上传音频文件到讯飞服务器 - 基于官方 demo 实现
     */
    private String uploadAudioFile(byte[] fileContent, String fileName, long fileSize, Long frontendDuration) throws Exception {

        String dateTime = getCurrentDateTime();
        String signatureRandom = generateRandomString(16);
        
//        System.out.println("=== 上传音频文件 ===");
//        System.out.println("文件名：" + fileName);
//        System.out.println("文件大小：" + fileSize + " 字节");
//        System.out.println("前端提供的时长：" + (frontendDuration != null ? frontendDuration + " ms" : "null"));
//        System.out.println("生成的时间戳：" + dateTime);
//        System.out.println("随机字符串：" + signatureRandom);
        
        // 计算音频时长
        long audioDuration;
        if (frontendDuration != null && frontendDuration > 0) {//查看前段传过来的数据是否包含音频时长
            audioDuration = frontendDuration;
            System.out.println("使用前端提供的时长：" + audioDuration + " ms");
        } else {
            audioDuration = calculateAudioDuration(fileContent);
            System.out.println("前端未提供时长，使用后端计算的时长：" + audioDuration + " ms");
        }

        //构建 URL 参数
        Map<String, String> urlParams = new HashMap<>();
        urlParams.put("appId", xfyunConfig.getAppId());
        urlParams.put("accessKeyId", xfyunConfig.getAccessKeyId());
        urlParams.put("dateTime", dateTime);
        urlParams.put("signatureRandom", signatureRandom);
        urlParams.put("fileSize", String.valueOf(fileSize));
        urlParams.put("fileName", fileName);
        urlParams.put("language", "autodialect");
        urlParams.put("duration", String.valueOf(audioDuration));
        System.out.println("最终发送给讯飞的时长：" + audioDuration + " ms");

        //生成签名，根据官方demo构建签名
        String signature = generateSignature(urlParams);
        if (signature == null || signature.isEmpty()) {
            throw new RuntimeException("签名生成失败，结果为空");
        }

        //构建请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/octet-stream");
        headers.put("signature", signature);

        //构建最终请求 URL
        String uploadUrl = buildRequestUrl(xfyunConfig.getApiBaseUrl() + "/v2/upload", urlParams);
        System.out.println("上传音频文件 URL：" + uploadUrl);

        //发送 POST 请求
        String response = sendPostRequest(uploadUrl, headers, fileContent, false);

        // 解析响应结果
        JsonNode result = objectMapper.readTree(response);
        System.out.println("上传响应：" + result.toPrettyString());

        // 处理 API 业务错误
        if (!"000000".equals(result.get("code").asText())) {
            throw new RuntimeException(String.format("上传失败（API 错误）：\n错误码：%s\n错误描述：%s\n请求 URL：%s\n签名原始串：%s\n签名值：%s", 
                result.get("code").asText(), 
                result.has("descInfo") ? result.get("descInfo").asText() : "未知错误", 
                uploadUrl, 
                lastBaseString, 
                lastSignature));
        }

        // 上传成功，返回订单 ID
        String orderId = result.get("content").get("orderId").asText();
        System.out.println("上传成功！订单 ID：" + orderId);
        return orderId;
    }

    /**
     * 轮询获取转写结果
     */
    private String pollForResult(String orderId) throws Exception {
        int maxRetries = 10; // 最大重试次数
        int retryInterval = 5000; // 重试间隔5秒

        for (int i = 0; i < maxRetries; i++) {
            Thread.sleep(retryInterval);
            String result = getResult(orderId);
            if (result != null && !result.isEmpty()) {
                return result;
            }
            System.out.println("第" + (i + 1) + "次轮询，未获取到结果...");
        }
        throw new RuntimeException("获取转写结果超时");
    }

    /**
     * 获取转写结果
     */
    private String getResult(String orderId) throws Exception {
        // 构建查询参数
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("appId", xfyunConfig.getAppId());
        queryParams.put("accessKeyId", xfyunConfig.getAccessKeyId());
        queryParams.put("dateTime", getCurrentDateTime());
        queryParams.put("orderId", orderId);
        queryParams.put("signatureRandom", generateRandomString(16));

        // 生成查询签名
        String querySignature = generateSignature(queryParams);
        Map<String, String> queryHeaders = new HashMap<>();
        queryHeaders.put("Content-Type", "application/json");
        queryHeaders.put("signature", querySignature);

        // 构建查询 URL
        String queryUrl = buildRequestUrl(xfyunConfig.getApiBaseUrl() + "/v2/getResult", queryParams);
        System.out.println("获取结果 URL：" + queryUrl);

        // 发送查询请求
        String response = sendPostRequest(queryUrl, queryHeaders, "{}".getBytes(StandardCharsets.UTF_8), true);
        JsonNode result = objectMapper.readTree(response);
        System.out.println("获取结果响应：" + result.toPrettyString());

        // 检查响应状态
        if (!"000000".equals(result.get("code").asText())) {
            throw new RuntimeException("查询失败（API 错误）：" + 
                (result.has("descInfo") ? result.get("descInfo").asText() : "未知错误"));
        }

        // 检查转写状态：3=处理中，4=完成
        int processStatus = result.get("content").get("orderInfo").get("status").asInt();
        if (processStatus == 4) {
            System.out.println("转写完成！");
            // 解析并返回转写结果
            return parseTranscriptionResult(result);
        } else if (processStatus != 3) {
            throw new RuntimeException("转写异常：状态码=" + processStatus);
        }

        return null; // 处理中，返回 null 继续轮询
    }

    /**
     * 解析转写结果
     */
    private String parseTranscriptionResult(JsonNode apiResponse) {
        try {
            // 从 API 响应中获取 orderResult 字段
            String orderResultStr = apiResponse.get("content").get("orderResult").asText("{}");
            String cleanedStr = orderResultStr.replace("\\\\", "\\");
            
            // 把 orderResult 字符串转为 JSON 对象
            JsonNode orderResult = objectMapper.readTree(cleanedStr);
            
            // 提取所有 w 字段的值
            List<String> wValues = new ArrayList<>();
            
            // 遍历数组
            if (orderResult.has("lattice")) {
                JsonNode lattice = orderResult.get("lattice");
                for (JsonNode latticeItem : lattice) {
                    if (latticeItem.has("json_1best")) {
                        // 解析 json_1best 字段
                        JsonNode json1best = objectMapper.readTree(latticeItem.get("json_1best").asText());
                        
                        // 处理 st 对象
                        if (json1best.has("st") && json1best.get("st").has("rt")) {
                            JsonNode rtArray = json1best.get("st").get("rt");
                            for (JsonNode rtItem : rtArray) {
                                if (rtItem.has("ws")) {
                                    JsonNode wsArray = rtItem.get("ws");
                                    for (JsonNode wsItem : wsArray) {
                                        if (wsItem.has("cw")) {
                                            JsonNode cwArray = wsItem.get("cw");
                                            for (JsonNode cwItem : cwArray) {
                                                if (cwItem.has("w")) {
                                                    wValues.add(cwItem.get("w").asText());
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // 拼接所有 w 值
            return String.join("", wValues);
            
        } catch (Exception e) {
            System.out.println("处理转写结果过程中出错：" + e.getMessage());
            return "";
        }
    }

    /**
     * 生成签名 ，参考官方提供的demo
     */
    private String generateSignature(Map<String, String> params) throws Exception {

        Map<String, String> signParams = new TreeMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!"signature".equals(entry.getKey()) && entry.getValue() != null && !entry.getValue().trim().isEmpty()) {
                signParams.put(entry.getKey(), entry.getValue());
            }
        }

        // 对 key 和 value 都进行 URL 编码
        List<String> baseParts = new ArrayList<>();
        for (Map.Entry<String, String> entry : signParams.entrySet()) {
            String encodedKey = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name());
            String encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.name());
            baseParts.add(encodedKey + "=" + encodedValue);
        }

        this.lastBaseString = String.join("&", baseParts);

        // HMAC-SHA1 加密 + Base64 编码
        Mac hmac = Mac.getInstance("HmacSHA1");
        SecretKeySpec secretKey = new SecretKeySpec(xfyunConfig.getAccessKeySecret().getBytes(StandardCharsets.UTF_8), "HmacSHA1");
        hmac.init(secretKey);
        byte[] hmacBytes = hmac.doFinal(lastBaseString.getBytes(StandardCharsets.UTF_8));
        this.lastSignature = Base64.getEncoder().encodeToString(hmacBytes);
        
        System.out.println("baseString：" + lastBaseString);
        System.out.println("最终签名：" + lastSignature);
        
        return this.lastSignature;
    }

    /**
     * 构建请求 URL
     */
    private String buildRequestUrl(String baseUrl, Map<String, String> params) throws Exception {
        List<String> encodedParams = new ArrayList<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String encodedKey = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name());
            String encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.name());
            encodedParams.add(encodedKey + "=" + encodedValue);
        }
        return baseUrl + "?" + String.join("&", encodedParams);
    }

    /**
     * 发送 POST 请求
     */
    private String sendPostRequest(String urlStr, Map<String, String> headers, byte[] data, boolean isJson) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);

            // 设置请求头
            for (Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }

            // 发送数据
            try (OutputStream os = connection.getOutputStream()) {
                os.write(data);
                os.flush();
            }

            // 读取响应
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                // 尝试读取错误流
                String errorResponse = readErrorStream(connection);
                throw new RuntimeException("HTTP 请求失败，状态码：" + responseCode + "，错误信息：" + errorResponse);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 读取错误流
     */
    private String readErrorStream(HttpURLConnection connection) {
        try {
            InputStream errorStream = connection.getErrorStream();
            if (errorStream != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream, StandardCharsets.UTF_8))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    return errorResponse.toString();
                }
            }
        } catch (Exception e) {
            // 忽略错误流读取异常
        }
        return "";
    }

    /**
     * 计算音频文件时长（毫秒）
     * 直接使用固定时长避免估算误差
     * 这个代码是前期使用跟前端没有发送过来音频的长度的时候才会进行估算，估算的结果基本都是错误的，发送过去的api也无法正常运行
     */
    private long calculateAudioDuration(byte[] audioData) {
        try {
            // 首先尝试直接从字节数组分析音频格式
            long directDuration = tryDirectDurationExtraction(audioData);
            if (directDuration > 0) {
                System.out.println("直接提取时长成功：" + directDuration + " ms");
                return directDuration;
            }
            
            // 如果直接提取失败，使用固定的合理默认值
            System.out.println("无法准确计算时长，使用固定默认值20秒");
            return 20000; // 固定20秒，这是一个比较合理的默认值
            
        } catch (Exception e) {
            System.out.println("时长计算异常，使用默认值20秒：" + e.getMessage());
            return 20000; // 固定20秒
        }
    }
    
    /**
     * 尝试直接从音频数据中提取时长信息
     * 支持 WAV、MP3 等常见格式的元数据读取
     */
    private long tryDirectDurationExtraction(byte[] audioData) {
        try {
            // 尝试 WAV 格式的直接读取
            long wavDuration = extractWavDuration(audioData);
            if (wavDuration > 0) {
                return wavDuration;
            }
            
            // 尝试 MP3 格式的直接读取
            long mp3Duration = extractMp3Duration(audioData);
            if (mp3Duration > 0) {
                return mp3Duration;
            }
            
            System.out.println("无法直接提取时长信息");
            return -1;
            
        } catch (Exception e) {
            System.out.println("直接提取时长异常：" + e.getMessage());
            return -1;
        }
    }
    
    /**
     * 从 WAV 文件头中直接提取时长
     */
    private long extractWavDuration(byte[] wavData) {
        try {
            // WAV 文件头格式检查
            if (wavData.length < 44) return -1;
            
            // 检查 RIFF 标识
            if (wavData[0] != 'R' || wavData[1] != 'I' || wavData[2] != 'F' || wavData[3] != 'F') {
                return -1;
            }
            
            // 检查 WAVE 标识
            if (wavData[8] != 'W' || wavData[9] != 'A' || wavData[10] != 'V' || wavData[11] != 'E') {
                return -1;
            }
            
            // 读取音频格式信息
            int sampleRate = getIntLE(wavData, 24);
            int bitsPerSample = getShortLE(wavData, 34);
            int channels = getShortLE(wavData, 22);
            int byteRate = getIntLE(wavData, 28);
            
            // 读取数据块大小
            int dataSize = getIntLE(wavData, 40);
            
            System.out.println("WAV 文件头信息：");
            System.out.println("  - 采样率：" + sampleRate + " Hz");
            System.out.println("  - 位深度：" + bitsPerSample + " bits");
            System.out.println("  - 通道数：" + channels);
            System.out.println("  - 数据大小：" + dataSize + " bytes");
            
            // 计算时长（毫秒）
            if (sampleRate > 0 && channels > 0 && bitsPerSample > 0) {
                double durationSeconds = (double) dataSize / (sampleRate * channels * (bitsPerSample / 8));
                long durationMs = Math.round(durationSeconds * 1000);
                
                System.out.println("  - 直接计算时长：" + durationMs + " ms");
                return durationMs;
            }
            
        } catch (Exception e) {
            System.out.println("WAV 时长提取异常：" + e.getMessage());
        }
        
        return -1;
    }
    
    /**
     * 从 MP3 文件中尝试提取时长（简单方法）
     */
    private long extractMp3Duration(byte[] mp3Data) {
        try {
            // 简单的 MP3 检测
            if (mp3Data.length < 10) return -1;
            
            // 检查 MP3 同步字（0xFFE）
            if ((mp3Data[0] & 0xFF) != 0xFF || (mp3Data[1] & 0xE0) != 0xE0) {
                // 不是标准 MP3 文件头，使用文件大小估算
                return estimateMp3Duration(mp3Data.length);
            }
            
            System.out.println("检测到 MP3 格式，使用文件大小估算");
            return estimateMp3Duration(mp3Data.length);
            
        } catch (Exception e) {
            System.out.println("MP3 时长提取异常：" + e.getMessage());
            return -1;
        }
    }
    
    /**
     * 小端序读取 4 字节整数
     */
    private int getIntLE(byte[] data, int offset) {
        return (data[offset] & 0xFF) |
               ((data[offset + 1] & 0xFF) << 8) |
               ((data[offset + 2] & 0xFF) << 16) |
               ((data[offset + 3] & 0xFF) << 24);
    }
    
    /**
     * 小端序读取 2 字节短整数
     */
    private int getShortLE(byte[] data, int offset) {
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
    }

    
    /**
     * MP3 文件时长估算
     * 结果通常都是不准确的，备用而已
     */
    private long estimateMp3Duration(long fileSize) {
        // 128 kbps 是常见的平均值
        double durationSeconds = (double) fileSize / (16 * 1024);
        long durationMs = Math.round(durationSeconds * 1000);
        
        System.out.println("MP3 估算详情：");
        System.out.println("  - 文件大小：" + fileSize + " bytes");
        System.out.println("  - 估算时长：" + durationSeconds + " 秒");
        System.out.println("  - 转换为毫秒：" + durationMs + " ms");
        System.out.println("  - 基于 128kbps 平均比特率");
        
        // MP3 时长范围检查（通常 1 秒到 6 小时）
        long minDuration = 1000;   // 1秒
        long maxDuration = 6 * 60 * 60 * 1000;  // 6小时
        
        if (durationMs < minDuration) {
            System.out.println("MP3 估算时长过短，调整为：" + minDuration + " ms");
            return minDuration;
        } else if (durationMs > maxDuration) {
            System.out.println("MP3 估算时长过长，调整为：" + maxDuration + " ms");
            return maxDuration;
        }
        
        return durationMs;
    }

    /**
     * 获取当前时间戳（符合 API 要求格式）
     * 格式：yyyy-MM-dd'T'HH:mm:ssZ
     * 根据官方 demo，使用 SimpleDateFormat 生成 +0800 格式
     */
    private String getCurrentDateTime() {
        // 使用 SimpleDateFormat 生成正确的格式：yyyy-MM-dd'T'HH:mm:ssZ
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        sdf.setTimeZone(TimeZone.getDefault());

        String dateTime = sdf.format(new Date());
        System.out.println("生成的 dateTime：" + dateTime);
        return dateTime;
    }

    /**
     * 生成随机字符串
     */
    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
    
    /**
     * 保存转写记录到数据库（云端转写 - 有 order_id）
     */
    private void saveTranscriptionRecord(Long userId, String orderId, String fileName, Long fileSize) {
        try {
            if (userId != null) {
                TranscriptionRecord record = new TranscriptionRecord(userId, orderId, fileName, fileSize);
                transcriptionRecordRepository.save(record);
                System.out.println("✅ 云端转写记录已保存：用户 ID=" + userId + ", 订单 ID=" + orderId);
            } else {
                System.out.println("⚠️ 用户未登录，跳过保存转写记录");
            }
        } catch (Exception e) {
            System.err.println("❌ 保存转写记录失败：" + e.getMessage());
            // 不让数据库操作影响主要功能
        }
    }
    
    /**
     * 保存转写记录到数据库（本地转写 - 有 transcription_text）
     */
    public void saveLocalTranscriptionRecord(Long userId, String transcriptionText, String fileName, Long fileSize) {
        try {
            if (userId != null) {
                // 注意：构造函数参数顺序是 (userId, fileName, fileSize, transcriptionText)
                TranscriptionRecord record = new TranscriptionRecord(userId, fileName, fileSize, transcriptionText);
                transcriptionRecordRepository.save(record);
                System.out.println("地转写记录已保存：用户 ID=" + userId + ", 文本长度=" + transcriptionText.length());
            } else {
                System.out.println("用户未登录，跳过保存本地转写记录");
            }
        } catch (Exception e) {
            System.err.println("保存本地转写记录失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 根据用户 ID 获取转写记录列表
     */
    public List<TranscriptionRecord> getUserTranscriptionRecords(Long userId) {
        System.out.println("查询用户 " + userId + " 的转写记录...");
        List<TranscriptionRecord> records = transcriptionRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);
        System.out.println("找到 " + records.size() + " 条记录");
        return records;
    }
        
    /**
     * 统计用户转写记录数量
     */
    public long countUserTranscriptionRecords(Long userId) {
        long count = transcriptionRecordRepository.countByUserId(userId);
        System.out.println("用户 " + userId + " 的转写记录总数：" + count);
        return count;
    }
    
    /**
     * 根据订单 ID 获取转写结果
     */
    public String getTranscriptionResult(String orderId) throws Exception {
        return getResult(orderId);
    }
}
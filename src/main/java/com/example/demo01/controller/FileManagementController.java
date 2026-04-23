package com.example.demo01.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/admin/files")
public class FileManagementController {

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    /**
     * 获取上传目录下的所有文件列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listFiles() {
        Map<String, Object> response = new HashMap<>();
        try {
            Path dir = Paths.get(uploadPath).toAbsolutePath();
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            File folder = dir.toFile();
            File[] files = folder.listFiles();
            List<Map<String, Object>> fileList = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        Map<String, Object> fileInfo = new HashMap<>();
                        fileInfo.put("name", file.getName());
                        fileInfo.put("size", file.length());
                        fileInfo.put("sizeFormatted", formatFileSize(file.length()));
                        fileInfo.put("lastModified", sdf.format(new Date(file.lastModified())));
                        fileInfo.put("path", file.getAbsolutePath());
                        fileList.add(fileInfo);
                    }
                }
            }

            // 按修改时间倒序排列
            fileList.sort((a, b) -> ((String) b.get("lastModified")).compareTo((String) a.get("lastModified")));

            response.put("success", true);
            response.put("data", fileList);
            response.put("total", fileList.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取文件列表失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 删除指定文件
     */
    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteFile(@RequestParam String fileName) {
        Map<String, Object> response = new HashMap<>();
        try {
            Path filePath = Paths.get(uploadPath, fileName).toAbsolutePath();
            File file = filePath.toFile();

            if (!file.exists()) {
                response.put("success", false);
                response.put("message", "文件不存在");
                return ResponseEntity.badRequest().body(response);
            }

            if (file.delete()) {
                response.put("success", true);
                response.put("message", "文件删除成功");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "文件删除失败");
                return ResponseEntity.status(500).body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "删除文件异常: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        else if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        else if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024.0));
        else return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
    }
}

package com.example.demo01.controller;

import com.example.demo01.model.StudentClass;
import com.example.demo01.service.StudentClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/class")
public class StudentClassController {

    @Autowired
    private StudentClassService studentClassService;

    /**
     * 创建新班级
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createClass(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
//            System.out.println("=== 接收到创建班级请求 ===");
//            System.out.println("请求参数：" + request);
            
            String classname = (String) request.get("classname");
            String classcode = (String) request.get("classcode");
            int classnum = (int) request.get("classnum");
            Long creatorUserId = null;
            
            // 获取创建人用户 ID（如果前端传递了的话）
            if (request.containsKey("creatorUserId")) {
                Object userIdObj = request.get("creatorUserId");
                System.out.println("收到 creatorUserId: " + userIdObj);
                if (userIdObj != null) {
                    creatorUserId = Long.parseLong(userIdObj.toString());
                    System.out.println("解析后的用户 ID: " + creatorUserId);
                } else {
                    System.out.println("creatorUserId 为 null");
                }
            } else {
                System.out.println("请求中不包含 creatorUserId 字段");
            }

            // 参数验证
            if (classname == null || classname.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "班级名称不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            if (classcode == null || classcode.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "班级编号不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            // 创建班级（自动添加创建人为老师）
            StudentClass studentClass = studentClassService.createClass(classname, classcode, classnum, creatorUserId);

            response.put("success", true);
            response.put("message", "班级创建成功，您已成为该班级的老师");
            response.put("data", studentClass);
            response.put("classid", studentClass.getClassid());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取所有班级列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getAllClasses() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<StudentClass> classes = studentClassService.getAllClasses();

            response.put("success", true);
            response.put("data", classes);
            response.put("total", classes.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 根据 ID 获取班级详情
     */
    @GetMapping("/{classid}")
    public ResponseEntity<Map<String, Object>> getClassById(@PathVariable Long classid) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            StudentClass studentClass = studentClassService.getClassById(classid);

            response.put("success", true);
            response.put("data", studentClass);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 更新班级信息
     */
    @PutMapping("/{classid}")
    public ResponseEntity<Map<String, Object>> updateClass(
            @PathVariable Long classid,
            @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String classname = (String) request.get("classname");
            Integer classnum = (Integer) request.get("classnum");

            StudentClass studentClass = studentClassService.updateClass(classid, classname, classnum);

            response.put("success", true);
            response.put("message", "班级更新成功");
            response.put("data", studentClass);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 删除班级
     */
    @DeleteMapping("/{classid}")
    public ResponseEntity<Map<String, Object>> deleteClass(@PathVariable Long classid) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            studentClassService.deleteClass(classid);

            response.put("success", true);
            response.put("message", "班级删除成功");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 搜索班级（按名称或编号）
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchClasses(
            @RequestParam String keyword) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("=== 搜索班级 ===");
            System.out.println("搜索关键词：" + keyword);
            
            // 同时按名称和编号模糊搜索
            List<StudentClass> classes = studentClassService.searchClasses(keyword);

            response.put("success", true);
            response.put("data", classes);
            response.put("total", classes.size());
            System.out.println("搜索结果：" + classes.size() + " 条");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

package com.example.demo01.controller;

import com.example.demo01.model.User;
import com.example.demo01.service.UserService;
import com.example.demo01.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    
    /**
     *  用户登录接口
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> loginRequest) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String username = loginRequest.get("username");
            String password = loginRequest.get("password");
            //验证账号密码
            if (username == null || password == null) {
                response.put("success", false);
                response.put("message", "用户名和密码不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            User user = userService.loginUser(username, password);
            
            // 生成token
            String token = jwtTokenUtil.generateToken(user.getUsername(), user.getId());

            //返回Token和用户信息
            response.put("success", true);
            response.put("token", token);
            response.put("user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "role", user.getRole() != null ? user.getRole() : 0
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "登录失败：" + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @RequestBody Map<String, String> registerRequest) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String username = registerRequest.get("username");
            String password = registerRequest.get("password");
            String email = registerRequest.get("email");
            //验证信息
            if (username == null || password == null || email == null) {
                response.put("success", false);
                response.put("message", "请填写完整的注册信息");
                return ResponseEntity.badRequest().body(response);
            }
            
            User user = userService.registerUser(username, password, email);
            
            response.put("success", true);
            response.put("message", "注册成功");
            response.put("user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "role", user.getRole() != null ? user.getRole() : 0
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "注册失败：" + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @RequestHeader("Authorization") String authHeader) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            //验证是否带有token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.put("success", false);
                response.put("message", "未提供认证令牌");
                return ResponseEntity.badRequest().body(response);
            }
            
            String token = authHeader.substring(7);
            //验证token
            if (!jwtTokenUtil.validateToken(token)) {
                response.put("success", false);
                response.put("message", "认证令牌无效或已过期");
                return ResponseEntity.badRequest().body(response);
            }
            
            Long userId = jwtTokenUtil.getUserIdFromToken(token);
            String username = jwtTokenUtil.getUsernameFromToken(token);
            
            response.put("success", true);
            response.put("user", Map.of(
                "id", userId,
                "username", username
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取用户信息失败：" + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "退出登录成功");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 修改用户邮箱
     */
    @PutMapping("/update-email")
    public ResponseEntity<Map<String, Object>> updateEmail(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 验证 Token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.put("success", false);
                response.put("message", "未提供认证令牌");
                return ResponseEntity.badRequest().body(response);
            }
            
            String token = authHeader.substring(7);
            if (!jwtTokenUtil.validateToken(token)) {
                response.put("success", false);
                response.put("message", "认证令牌无效或已过期");
                return ResponseEntity.badRequest().body(response);
            }
            
            Long userId = jwtTokenUtil.getUserIdFromToken(token);
            String newEmail = request.get("email");
            
            if (newEmail == null || newEmail.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "邮箱不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            User user = userService.updateEmail(userId, newEmail);
            
            response.put("success", true);
            response.put("message", "邮箱修改成功");
            response.put("user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 修改用户密码
     */
    @PutMapping("/update-password")
    public ResponseEntity<Map<String, Object>> updatePassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 验证 Token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.put("success", false);
                response.put("message", "未提供认证令牌");
                return ResponseEntity.badRequest().body(response);
            }
            
            String token = authHeader.substring(7);
            if (!jwtTokenUtil.validateToken(token)) {
                response.put("success", false);
                response.put("message", "认证令牌无效或已过期");
                return ResponseEntity.badRequest().body(response);
            }
            
            Long userId = jwtTokenUtil.getUserIdFromToken(token);
            String oldPassword = request.get("oldPassword");
            String newPassword = request.get("newPassword");
            
            if (oldPassword == null || oldPassword.trim().isEmpty() ||
                newPassword == null || newPassword.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "原密码和新密码不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (newPassword.length() < 6) {
                response.put("success", false);
                response.put("message", "新密码长度不能少于6位");
                return ResponseEntity.badRequest().body(response);
            }
            
            userService.updatePassword(userId, oldPassword, newPassword);
            
            response.put("success", true);
            response.put("message", "密码修改成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 管理员更新用户角色
     */
    @PutMapping("/update-user-role")
    public ResponseEntity<Map<String, Object>> updateUserRole(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 验证 Token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.put("success", false);
                response.put("message", "未提供认证令牌");
                return ResponseEntity.badRequest().body(response);
            }
            
            String token = authHeader.substring(7);
            if (!jwtTokenUtil.validateToken(token)) {
                response.put("success", false);
                response.put("message", "认证令牌无效或已过期");
                return ResponseEntity.badRequest().body(response);
            }
            
            Long adminId = jwtTokenUtil.getUserIdFromToken(token);
            String targetUsername = (String) request.get("username");
            Integer newRole = Integer.parseInt(request.get("role").toString());
            
            if (targetUsername == null || targetUsername.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "用户名不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            User updatedUser = userService.updateUserRole(adminId, targetUsername, newRole);
            
            response.put("success", true);
            response.put("message", "用户角色更新成功");
            response.put("user", Map.of(
                "id", updatedUser.getId(),
                "username", updatedUser.getUsername(),
                "role", updatedUser.getRole()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
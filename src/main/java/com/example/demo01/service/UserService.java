package com.example.demo01.service;

import com.example.demo01.model.User;
import com.example.demo01.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;// Spring Security提供的BCrypt
    
    /**
     * 用户注册
     */
    public User registerUser(String username, String password, String email) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("邮箱已被注册");
        }
        
        // 创建新用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password)); // 密码加密
        user.setEmail(email);
        user.setCreatedAt(LocalDateTime.now());
        
        // 如果是第一个注册用户，自动设置为管理员
        long userCount = userRepository.count();
        if (userCount == 0) {
            user.setRole(5); // 设置为管理员
            System.out.println("检测到这是第一个注册用户，自动设置为管理员账号: " + username);
        } else {
            user.setRole(0); // 普通用户
        }
        
        return userRepository.save(user);
    }
    
    /**
     * 用户登录
     */
    public User loginUser(String username, String password) {
        Optional<User> CurUser = userRepository.findByUsername(username);
        
        if (CurUser.isPresent()) {
            User user = CurUser.get();
            // 验证密码
            if (passwordEncoder.matches(password, user.getPassword())) {
                // 更新最后登录时间
                user.setLastLogin(LocalDateTime.now());
                userRepository.save(user);
                return user;
            }
        }
        
        throw new RuntimeException("用户名或密码错误");
    }
    
    /**
     * 根据用户名查找用户
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    /**
     * 根据 ID 查找用户
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
        
    /**
     * 根据 ID 查找用户（返回确定对象）
     */
    public User findUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
    
    /**
     * 检查用户名是否存在
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    /**
     * 检查邮箱是否存在
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    /**
     * 修改用户邮箱
     */
    public User updateEmail(Long userId, String newEmail) {
        Optional<User> CurUser = userRepository.findById(userId);
        
        if (CurUser.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }
        
        // 检查新邮箱是否已被其他用户使用
        User user = CurUser.get();
        if (userRepository.existsByEmail(newEmail) && !user.getEmail().equals(newEmail)) {
            throw new RuntimeException("邮箱已被其他用户使用");
        }
        
        user.setEmail(newEmail);
        return userRepository.save(user);
    }
    
    /**
     * 修改用户密码
     */
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        Optional<User> CurUser = userRepository.findById(userId);
        
        if (CurUser.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }
        
        User user = CurUser.get();
        
        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }
        
        // 设置新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
    
    /**
     * 管理员更新用户角色
     * @param adminId 管理员ID
     * @param targetUsername 目标用户账号
     * @param newRole 新角色：3-老师，5-管理员
     */
    public User updateUserRole(Long adminId, String targetUsername, Integer newRole) {
        // 验证管理员身份
        Optional<User> adminOpt = userRepository.findById(adminId);
        if (adminOpt.isEmpty() || !adminOpt.get().getRole().equals(5)) {
            throw new RuntimeException("权限不足，只有管理员可以执行此操作");
        }
        
        // 查找目标用户
        Optional<User> targetUserOpt = userRepository.findByUsername(targetUsername);
        if (targetUserOpt.isEmpty()) {
            throw new RuntimeException("用户不存在：" + targetUsername);
        }

        // 验证角色值
        if (newRole != 3 && newRole != 5) {
            throw new RuntimeException("无效的角色值，只能设置为3(老师)或5(管理员)");
        }

        User targetUser = targetUserOpt.get();
        targetUser.setRole(newRole);
        return userRepository.save(targetUser);
    }
}
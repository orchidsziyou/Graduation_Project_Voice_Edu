package com.example.demo01.controller;

import com.example.demo01.model.User;
import com.example.demo01.service.SqlQueryService;
import com.example.demo01.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api")
public class SqlQueryController {

    @Autowired
    private SqlQueryService sqlQueryService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Value("${app.login.enabled:true}")
    private boolean loginEnabled;

    /**
     * SQL查询页面
     */
    @GetMapping("/sql-query")
    public String sqlQueryPage(HttpSession session, Model model) {
        // 检查登录状态
        if (loginEnabled) {
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser == null) {
                return "redirect:/login";
            }
            model.addAttribute("currentUser", currentUser);
        }

        // 获取所有表名
        List<String> tableNames = sqlQueryService.getAllTableNames();
        model.addAttribute("tableNames", tableNames);
        model.addAttribute("loginEnabled", loginEnabled);

        return "sql-query";
    }

    /**
     * 执行 SQL 查询
     */
    @PostMapping("/sql/execute")
    @ResponseBody
    public Map<String, Object> executeQuery(@RequestBody Map<String, String> request,
                                          HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();
    
        // 检查登录状态（支持 JWT Token 和 Session）
        User currentUser = getCurrentUser(httpRequest);
        if (loginEnabled && currentUser == null) {
            response.put("success", false);
            response.put("error", "请先登录");
            return response;
        }
    
        try {
            String sql = request.get("sql");
            if (sql == null || sql.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "SQL 语句不能为空");
                return response;
            }
    
            // 执行查询
            Map<String, Object> result = sqlQueryService.executeQuery(sql);
            response.putAll(result);
    
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "执行 SQL 时发生错误：" + e.getMessage());
        }
    
        return response;
    }

    /**
     * 获取表结构信息
     */
    @GetMapping("/sql/table/{tableName}/structure")
    @ResponseBody
    public Map<String, Object> getTableStructure(@PathVariable String tableName,
                                               HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();

        // 检查登录状态（支持 JWT Token 和 Session）
        User currentUser = getCurrentUser(httpRequest);
        if (loginEnabled && currentUser == null) {
            response.put("success", false);
            response.put("error", "请先登录");
            return response;
        }

        try {
            List<Map<String, Object>> structure = sqlQueryService.getTableStructure(tableName);
            response.put("success", true);
            response.put("structure", structure);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "获取表结构失败：" + e.getMessage());
        }

        return response;
    }

    @GetMapping("/sql/tables")
    @ResponseBody
    public Map<String, Object> getAllTables(HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();

        // 检查登录状态（支持 JWT Token 和 Session）
        User currentUser = getCurrentUser(httpRequest);
        if (loginEnabled && currentUser == null) {
            response.put("success", false);
            response.put("error", "请先登录");
            return response;
        }

        try {
            List<String> tableNames = sqlQueryService.getAllTableNames();
            response.put("success", true);
            response.put("tableNames", tableNames);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "获取表名失败：" + e.getMessage());
        }

        return response;
    }

    @PostMapping("/sql/batch-execute")
    @ResponseBody
    public Map<String, Object> executeBatch(@RequestBody Map<String, List<String>> request,
                                          HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();

        // 检查登录状态（支持 JWT Token 和 Session）
        User currentUser = getCurrentUser(httpRequest);
        if (loginEnabled && currentUser == null) {
            response.put("success", false);
            response.put("error", "请先登录");
            return response;
        }

        try {
            List<String> sqlStatements = request.get("sqlStatements");
            if (sqlStatements == null || sqlStatements.isEmpty()) {
                response.put("success", false);
                response.put("error", "SQL 语句列表不能为空");
                return response;
            }

            Map<String, Object> result = sqlQueryService.executeBatch(sqlStatements);
            response.putAll(result);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "批量执行 SQL 时发生错误：" + e.getMessage());
        }

        return response;
    }

    @GetMapping("/sql/stats")
    @ResponseBody
    public Map<String, Object> getDatabaseStats(HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();

        // 检查登录状态（支持 JWT Token 和 Session）
        User currentUser = getCurrentUser(httpRequest);
        if (loginEnabled && currentUser == null) {
            response.put("success", false);
            response.put("error", "请先登录");
            return response;
        }

        try {
            Map<String, Object> stats = sqlQueryService.getDatabaseStats();
            response.put("success", true);
            response.putAll(stats);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "获取数据库统计信息失败：" + e.getMessage());
        }

        return response;
    }

    /**
     * API: 检查SQL查询权限
     */
    @GetMapping("/api/sql/permission")
    @ResponseBody
    public Map<String, Object> checkSqlPermission(HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        if (loginEnabled) {
            User currentUser = (User) session.getAttribute("currentUser");
            response.put("requiresLogin", true);
            response.put("hasPermission", currentUser != null);
            if (currentUser != null) {
                response.put("username", currentUser.getUsername());
            }
        } else {
            response.put("requiresLogin", false);
            response.put("hasPermission", true);
        }

        return response;
    }

    /**
     * 获取当前登录用户 (支持 JWT Token 和 Session)
     */
    private User getCurrentUser(HttpServletRequest request) {
        // 首先尝试从 JWT Token 获取
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String username = jwtTokenUtil.getUsernameFromToken(token);
                if (username != null && jwtTokenUtil.validateToken(token)) {
                    // 创建一个简单的 User 对象
                    User user = new User();
                    user.setUsername(username);
                    return user;
                }
            }
        } catch (Exception e) {
            // Token 解析失败，继续尝试从 Session 获取
            System.err.println("JWT Token 解析失败：" + e.getMessage());
        }
        
        // 从 Session 获取
        HttpSession session = request.getSession(false);
        if (session != null) {
            return (User) session.getAttribute("currentUser");
        }
        
        return null;
    }
}
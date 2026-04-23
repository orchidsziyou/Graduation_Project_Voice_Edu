package com.example.demo01.controller;

import com.example.demo01.model.ClassMembers;
import com.example.demo01.service.ClassMembersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/class-members")
public class ClassMembersController {

    @Autowired
    private ClassMembersService classMembersService;

    /**
     * 添加班级成员
     */
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addMember(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long userid = Long.parseLong(request.get("userid").toString());
            Long classid = Long.parseLong(request.get("classid").toString());
            int userrole = (int) request.get("userrole");

            // 参数验证
            if (userid == null || userid <= 0) {
                response.put("success", false);
                response.put("message", "用户 ID 无效");
                return ResponseEntity.badRequest().body(response);
            }

            if (classid == null || classid <= 0) {
                response.put("success", false);
                response.put("message", "班级 ID 无效");
                return ResponseEntity.badRequest().body(response);
            }

            if (userrole != 1 && userrole != 5) {
                response.put("success", false);
                response.put("message", "角色类型无效（1=学生，5=老师）");
                return ResponseEntity.badRequest().body(response);
            }

            // 添加成员
            ClassMembers member = classMembersService.addMember(userid, classid, userrole);

            response.put("success", true);
            response.put("message", "添加成员成功");
            response.put("data", member);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 移除班级成员
     */
    @DeleteMapping("/remove")
    public ResponseEntity<Map<String, Object>> removeMember(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long classid = Long.parseLong(request.get("classid").toString());
            Long userid = Long.parseLong(request.get("userid").toString());

            classMembersService.removeMember(classid, userid);

            response.put("success", true);
            response.put("message", "移除成员成功");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取班级所有成员
     */
    @GetMapping("/list-with-users/{classid}")
    public ResponseEntity<Map<String, Object>> getClassMembersWithUsers(@PathVariable Long classid) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Map<String, Object>> members = classMembersService.getClassMembersWithUserInfo(classid);

            response.put("success", true);
            response.put("data", members);
            response.put("total", members.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取班级所有成员
     */
    @GetMapping("/list/{classid}")
    public ResponseEntity<Map<String, Object>> getClassMembers(@PathVariable Long classid) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<ClassMembers> members = classMembersService.getClassMembers(classid);

            response.put("success", true);
            response.put("data", members);
            response.put("total", members.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取用户加入的所有班级
     */
    @GetMapping("/my-classes/{userid}")
    public ResponseEntity<Map<String, Object>> getUserClasses(@PathVariable Long userid) {
        Map<String, Object> response = new HashMap<>();
        
        try {
//            System.out.println("=== 获取用户班级列表 ===");
//            System.out.println("用户 ID: " + userid);
            
            List<Map<String, Object>> classes = classMembersService.getUserClassesWithDetails(userid);
            response.put("success", true);
            response.put("data", classes);
            response.put("total", classes.size());
//            System.out.println("返回 " + classes.size() + " 个班级");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 统计班级人数
     */
    @GetMapping("/count/{classid}")
    public ResponseEntity<Map<String, Object>> countMembers(@PathVariable Long classid) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            int count = classMembersService.countClassMembers(classid);

            response.put("success", true);
            response.put("count", count);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 更新成员角色
     */
    @PutMapping("/update-role")
    public ResponseEntity<Map<String, Object>> updateMemberRole(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long classid = Long.parseLong(request.get("classid").toString());
            Long userid = Long.parseLong(request.get("userid").toString());
            int userrole = (int) request.get("userrole");

            if (userrole != 1 && userrole != 5) {
                response.put("success", false);
                response.put("message", "角色类型无效（1=学生，5=老师）");
                return ResponseEntity.badRequest().body(response);
            }

            ClassMembers member = classMembersService.updateMemberRole(classid, userid, userrole);

            response.put("success", true);
            response.put("message", "角色更新成功");
            response.put("data", member);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

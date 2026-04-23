package com.example.demo01.service;

import com.example.demo01.model.ClassMembers;
import com.example.demo01.model.StudentClass;
import com.example.demo01.model.User;
import com.example.demo01.repository.ClassMembersRepository;
import com.example.demo01.repository.StudentClassRepository;
import com.example.demo01.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClassMembersService {

    @Autowired
    private ClassMembersRepository classMembersRepository;
    
    @Autowired
    private StudentClassRepository studentClassRepository;
    
    @Autowired
    private UserRepository userRepository;

    /**
     * 添加班级成员
     */
    @Transactional
    public ClassMembers addMember(Long userid, Long classid, int userrole) throws Exception {
        // 检查用户是否已经在该班级中
        ClassMembers existing = classMembersRepository.findByClassidAndUserid(classid, userid);
        if (existing != null) {
            throw new Exception("用户已经在该班级中");
        }

        // 创建成员对象
        ClassMembers member = new ClassMembers();
        member.setUserid(userid);
        member.setClassid(classid);
        member.setUserrole(userrole);
        member.setJoinAt(LocalDateTime.now());

        return classMembersRepository.save(member);
    }

    /**
     * 移除班级成员
     */
    @Transactional
    public void removeMember(Long classid, Long userid) throws Exception {
        ClassMembers member = classMembersRepository.findByClassidAndUserid(classid, userid);
        if (member == null) {
            throw new Exception("用户不在该班级中");
        }
        classMembersRepository.delete(member);
    }

    /**
     * 获取班级所有成员（包含用户信息）
     */
    public List<Map<String, Object>> getClassMembersWithUserInfo(Long classid) {
//        System.out.println("=== 获取班级成员列表 ===");
//        System.out.println("班级 ID: " + classid);
        
        List<ClassMembers> members = classMembersRepository.findByClassidOrderByJoinAtDesc(classid);
//        System.out.println("找到 " + members.size() + " 个成员");
        
        return members.stream().map(member -> {
            Map<String, Object> result = new HashMap<>();
            
            // 添加成员信息
            result.put("memberId", member.getId());
            result.put("userid", member.getUserid());
            result.put("classid", member.getClassid());
            result.put("userrole", member.getUserrole());
            result.put("joinAt", member.getJoinAt());
            
            // 获取用户信息
            Optional<User> userOpt = userRepository.findById(member.getUserid());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                result.put("username", user.getUsername());
                result.put("email", user.getEmail());
            }
            return result;
        }).collect(Collectors.toList());
    }
    
    /**
     * 获取班级所有成员
     */
    public List<ClassMembers> getClassMembers(Long classid) {
        return classMembersRepository.findByClassidOrderByJoinAtDesc(classid);
    }

    /**
     * 获取用户加入的所有班级（包含班级详情）
     */
    public List<Map<String, Object>> getUserClassesWithDetails(Long userid) {
        System.out.println("=== 获取用户班级列表 ===");
        System.out.println("用户 ID: " + userid);
        
        List<ClassMembers> members = classMembersRepository.findByUseridOrderByJoinAtDesc(userid);
        System.out.println("找到 " + members.size() + " 条成员记录");
        
        return members.stream().map(member -> {
            Map<String, Object> result = new HashMap<>();
            
            // 获取班级详情
            Optional<StudentClass> classOpt = studentClassRepository.findById(member.getClassid());
            if (classOpt.isPresent()) {
                StudentClass studentClass = classOpt.get();
                result.put("classid", studentClass.getClassid());
                result.put("classname", studentClass.getClassname());
                result.put("classcode", studentClass.getClasscode());
                result.put("classnum", studentClass.getClassnum());
                result.put("createdAt", studentClass.getCreatedAt());
                result.put("updatedAt", studentClass.getUpdatedAt());
            }
            
            // 添加成员信息
            result.put("userrole", member.getUserrole());
            result.put("joinAt", member.getJoinAt());
            result.put("isCreator", member.getUserrole() == 5);  // 老师角色视为创建者
            
            return result;
        }).collect(Collectors.toList());
    }
    
    /**
     * 获取用户加入的所有班级（仅成员信息）
     */
    public List<ClassMembers> getUserClasses(Long userid) {
        return classMembersRepository.findByUseridOrderByJoinAtDesc(userid);
    }

    /**
     * 统计班级人数
     */
    public int countClassMembers(Long classid) {
        return classMembersRepository.countByClassid(classid);
    }

    /**
     * 更新成员角色
     */
    @Transactional
    public ClassMembers updateMemberRole(Long classid, Long userid, int userrole) throws Exception {
        ClassMembers member = classMembersRepository.findByClassidAndUserid(classid, userid);
        if (member == null) {
            throw new Exception("用户不在该班级中");
        }
        
        member.setUserrole(userrole);
        return classMembersRepository.save(member);
    }
}

package com.example.demo01.service;

import com.example.demo01.model.StudentClass;
import com.example.demo01.model.ClassMembers;
import com.example.demo01.repository.StudentClassRepository;
import com.example.demo01.repository.ClassMembersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class StudentClassService {

    @Autowired
    private StudentClassRepository studentClassRepository;
    
    @Autowired
    private ClassMembersRepository classMembersRepository;

    /**
     * 创建新班级
     */
    @Transactional
    public StudentClass createClass(String classname, String classcode, int classnum, Long creatorUserId) throws Exception {
//        System.out.println("=== 开始创建班级 ===");
//        System.out.println("班级名称：" + classname);
//        System.out.println("班级编号：" + classcode);
//        System.out.println("班级人数：" + classnum);
//        System.out.println("创建人用户 ID: " + (creatorUserId != null ? creatorUserId : "null"));
        
        // 检查班级编号是否已存在
        if (studentClassRepository.existsByClasscode(classcode)) {
            throw new Exception("班级编号已存在，请使用其他编号");
        }

        // 创建班级对象
        StudentClass studentClass = new StudentClass();
        studentClass.setClassname(classname);
        studentClass.setClasscode(classcode);
        studentClass.setClassnum(classnum);
        studentClass.setCreatedAt(LocalDateTime.now());
        studentClass.setUpdatedAt(LocalDateTime.now());

        // 保存班级
        StudentClass savedClass = studentClassRepository.save(studentClass);
        System.out.println(" 班级保存成功，班级 ID: " + savedClass.getClassid());
        
        // 如果提供了创建人用户 ID，自动将其添加为班级老师
        if (creatorUserId != null && creatorUserId > 0) {
            System.out.println(" 准备添加班级成员...");
            
            ClassMembers member = new ClassMembers();
            member.setUserid(creatorUserId);
            member.setClassid(savedClass.getClassid());
            member.setUserrole(5); // 5=老师
            member.setJoinAt(LocalDateTime.now());
            
            classMembersRepository.save(member);
//            System.out.println("自动添加班级创建人 (用户 ID: " + creatorUserId + ") 为班级老师");
//            System.out.println("班级成员记录 ID: " + member.getId());
        } else {
            System.out.println("未提供有效的创建人用户 ID，跳过添加班级成员");
        }

        return savedClass;
    }

    /**
     * 根据 ID 获取班级
     */
    public StudentClass getClassById(Long classid) throws Exception {
        Optional<StudentClass> optional = studentClassRepository.findById(classid);
        if (optional.isEmpty()) {
            throw new Exception("班级不存在");
        }
        return optional.get();
    }

    /**
     * 根据编号获取班级
     */
    public StudentClass getClassByCode(String classcode) throws Exception {
        return studentClassRepository.findByClasscode(classcode)
                .orElseThrow(() -> new Exception("班级不存在"));
    }

    /**
     * 更新班级信息
     */
    @Transactional
    public StudentClass updateClass(Long classid, String classname, Integer classnum) throws Exception {
        StudentClass studentClass = getClassById(classid);
        
        if (classname != null && !classname.trim().isEmpty()) {
            studentClass.setClassname(classname);
        }
        
        if (classnum != null && classnum > 0) {
            studentClass.setClassnum(classnum);
        }
        
        studentClass.setUpdatedAt(LocalDateTime.now());
        return studentClassRepository.save(studentClass);
    }

    /**
     * 删除班级
     */
    @Transactional
    public void deleteClass(Long classid) throws Exception {
        StudentClass studentClass = getClassById(classid);
        studentClassRepository.delete(studentClass);
    }

    /**
     * 获取所有班级列表
     */
    public List<StudentClass> getAllClasses() {
        return studentClassRepository.findAllOrderByCreatedAtDesc();
    }

    /**
     * 搜索班级（按名称或编号模糊匹配）
     */
    public List<StudentClass> searchClasses(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String pattern = "%" + keyword.toLowerCase() + "%";
        System.out.println("搜索模式：" + pattern);
        
        // 使用 JPQL 查询，同时匹配名称和编号
        return studentClassRepository.searchByKeyword(pattern);
    }
}

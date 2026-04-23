package com.example.demo01.repository;

import com.example.demo01.model.ClassMembers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassMembersRepository extends JpaRepository<ClassMembers, Long> {
    
    /**
     * 根据班级 ID 查询所有成员
     */
    List<ClassMembers> findByClassidOrderByJoinAtDesc(Long classid);
    
    /**
     * 根据用户 ID 查询所有加入的班级
     */
    List<ClassMembers> findByUseridOrderByJoinAtDesc(Long userid);
    
    /**
     * 查询用户在指定班级中的成员信息
     */
    ClassMembers findByClassidAndUserid(Long classid, Long userid);
    
    /**
     * 统计班级人数
     */
    @Query("SELECT COUNT(m) FROM ClassMembers m WHERE m.classid = :classid")
    int countByClassid(@Param("classid") Long classid);
    
    /**
     * 统计某个用户在多少个班级中
     */
    int countByUserid(Long userid);
}

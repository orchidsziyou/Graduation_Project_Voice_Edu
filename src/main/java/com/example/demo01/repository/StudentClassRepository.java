package com.example.demo01.repository;

import com.example.demo01.model.StudentClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentClassRepository extends JpaRepository<StudentClass, Long> {
    
    /**
     * 根据班级编号查找
     */
    Optional<StudentClass> findByClasscode(String classcode);
    
    /**
     * 根据班级名称模糊查询
     */
    List<StudentClass> findByClassnameContaining(String classname);
    
    /**
     * 检查班级编号是否存在
     */
    boolean existsByClasscode(String classcode);
    
    /**
     * 查询所有班级（按创建时间倒序）
     */
    @Query("SELECT sc FROM StudentClass sc ORDER BY sc.createdAt DESC")
    List<StudentClass> findAllOrderByCreatedAtDesc();
    
    /**
     * 根据关键词搜索班级（同时匹配名称和编号）
     */
    @Query("SELECT sc FROM StudentClass sc WHERE LOWER(sc.classname) LIKE :pattern OR LOWER(sc.classcode) LIKE :pattern")
    List<StudentClass> searchByKeyword(@Param("pattern") String pattern);
}

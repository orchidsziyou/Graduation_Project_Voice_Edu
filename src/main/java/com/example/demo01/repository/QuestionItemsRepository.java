package com.example.demo01.repository;

import com.example.demo01.model.question_items;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionItemsRepository extends JpaRepository<question_items, Long> {
    
    /**
     * 根据题干内容模糊查询
     */
    @Query("SELECT q FROM question_items q WHERE q.question_body LIKE %:keyword%")
    Page<question_items> findByQuestionBodyContaining(@Param("keyword") String keyword, Pageable pageable);
}

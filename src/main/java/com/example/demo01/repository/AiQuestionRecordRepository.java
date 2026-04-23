package com.example.demo01.repository;

import com.example.demo01.model.AiQuestionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiQuestionRecordRepository extends JpaRepository<AiQuestionRecord,Long> {
    List<AiQuestionRecord>findByUsernameOrderByCreateAtDesc(String username);

    long countByUsername(String username);

}

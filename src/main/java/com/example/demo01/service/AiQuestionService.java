package com.example.demo01.service;


import com.example.demo01.model.AiQuestionRecord;
import com.example.demo01.repository.AiQuestionRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AiQuestionService {

    @Autowired
    private AiQuestionRecordRepository aiQuestionRecordRepository;

    /**
     * 保存 AI 出题记录
     */
    public AiQuestionRecord saveQuestionRecord(String username,
                                               String transcriptionContent,
                                               String customRemark,
                                               String generatedQuestion) {
        AiQuestionRecord record = new AiQuestionRecord();
        record.setUsername(username);
        record.setTranscriptionContent(transcriptionContent);
        record.setCustomRemark(customRemark);
        record.setGeneratedQuestion(generatedQuestion);
        record.setCreateAt(LocalDateTime.now());

        return aiQuestionRecordRepository.save(record);
    }

    /**
     * 获取用户的所有出题记录（按时间倒序）
     */
    public List<AiQuestionRecord> getUserRecords(String username) {
        return aiQuestionRecordRepository.findByUsernameOrderByCreateAtDesc(username);
    }

    /**
     * 统计用户的出题记录数量
     */
    public long countUserRecords(String username) {
        return aiQuestionRecordRepository.countByUsername(username);
    }

    /**
     * 根据 ID 删除记录
     */
    public void deleteRecord(Long id) {
        aiQuestionRecordRepository.deleteById(id);
    }

    /**
     * 根据 ID 获取记录
     */
    public AiQuestionRecord getRecordById(Long id) {
        return aiQuestionRecordRepository.findById(id).orElse(null);
    }
}

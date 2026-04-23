package com.example.demo01.service;

import com.example.demo01.model.question_items;
import com.example.demo01.model.userQuestionRecords;
import com.example.demo01.repository.QuestionItemsRepository;
import com.example.demo01.repository.UserQuestionRecordsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Service
public class UserQuestionRecordsService {
    
    @Autowired
    private UserQuestionRecordsRepository userQuestionRecordsRepository;
    
    @Autowired
    private QuestionItemsRepository questionItemsRepository;
    
    /**
     * 随机获取一道题目
     */
    public question_items getRandomQuestion() {
        List<question_items> allQuestions = questionItemsRepository.findAll();
        if (allQuestions.isEmpty()) {
            return null;
        }
        Random random = new Random();
        return allQuestions.get(random.nextInt(allQuestions.size()));
    }
    
    /**
     * 保存答题记录
     */
    @Transactional
    public userQuestionRecords saveRecord(Long userid, Long questionid, String answer, Integer questionType) {
        userQuestionRecords record = new userQuestionRecords(userid, questionid, answer, questionType);
        return userQuestionRecordsRepository.save(record);
    }
    
    /**
     * 分页查询用户的答题记录
     */
    public Page<userQuestionRecords> getUserRecords(Long userid, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return userQuestionRecordsRepository.findByUseridOrderByCreatedAtDesc(userid, pageable);
    }
    
    /**
     * 查询用户的所有答题记录
     */
    public List<userQuestionRecords> getUserRecords(Long userid) {
        return userQuestionRecordsRepository.findByUseridOrderByCreatedAtDesc(userid);
    }
}

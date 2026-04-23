package com.example.demo01.service;

import com.example.demo01.model.question_items;
import com.example.demo01.repository.QuestionItemsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionItemsService {
    
    @Autowired
    private QuestionItemsRepository questionItemsRepository;
    
    /**
     * 保存题目
     */
    @Transactional
    public question_items saveQuestion(String questionBody, int questionType, 
                                       String questionAnswer, String choosingAnswer, Long userid) {
        question_items item = new question_items();
        item.setQuestion_body(questionBody);
        item.setQuestion_type(questionType);
        item.setQuestion_answer(questionAnswer);
        item.setChoosing_answer(choosingAnswer);
        item.setUserid(userid);
        
        return questionItemsRepository.save(item);
    }
    
    /**
     * 分页查询题目列表
     */
    public Page<question_items> findAll(int page, int size, String keyword) {
        // 创建排序：按 ID 降序
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(page, size, sort);
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 有关键词，进行模糊查询
            return questionItemsRepository.findByQuestionBodyContaining(keyword, pageable);
        } else {
            // 无关键词，返回全部
            return questionItemsRepository.findAll(pageable);
        }
    }
}

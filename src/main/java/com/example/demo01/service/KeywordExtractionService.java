package com.example.demo01.service;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 关键词提取服务
 * 使用 HanLP 实现中文分词和关键词提取
 */
@Service
public class KeywordExtractionService {

    /**
     * 从文本中提取关键词
     * 
     * @param text 输入文本
     * @param topN 返回前 N 个关键词
     * @return 关键词列表（按权重排序）
     */
    public List<Map<String, Object>> extractKeywords(String text, int topN) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // 使用 HanLP 的 TextRank 算法提取关键词
        List<String> keywordList = HanLP.extractKeyword(text, topN);
        
        // 构建返回结果，包含关键词和简单权重
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < keywordList.size(); i++) {
            Map<String, Object> keywordMap = new HashMap<>();
            keywordMap.put("keyword", keywordList.get(i));
            // 根据排名计算权重（排名越靠前权重越高）
            double weight = 1.0 - (i * 0.1);
            keywordMap.put("weight", Math.max(weight, 0.1));
            result.add(keywordMap);
        }

        return result;
    }



    /**
     * 从文本中提取关键短语
     * 
     * @param text 输入文本
     * @param topN 返回前 N 个关键短语
     * @return 关键短语列表
     */
    public List<String> extractKeyPhrases(String text, int topN) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // 使用 HanLP 提取关键短语
        return HanLP.extractPhrase(text, topN);
    }

    /**
     * 对文本进行分词
     * 
     * @param text 输入文本
     * @return 分词结果列表
     */
    public List<Map<String, String>> segmentText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // 使用 HanLP 进行标准分词
        List<Term> termList = HanLP.segment(text);
        
        List<Map<String, String>> result = new ArrayList<>();
        for (Term term : termList) {
            Map<String, String> termMap = new HashMap<>();
            termMap.put("word", term.word);
            termMap.put("nature", term.nature.toString()); // 词性
            result.add(termMap);
        }

        return result;
    }

    /**
     * 提取关键词并格式化为字符串
     * 
     * @param text 输入文本
     * @param topN 返回前 N 个关键词
     * @return 逗号分隔的关键词字符串
     */
    public String extractKeywordsAsString(String text, int topN) {
        List<Map<String, Object>> keywords = extractKeywords(text, topN);
        return keywords.stream()
                .map(k -> (String) k.get("keyword"))
                .collect(Collectors.joining("、"));
    }
}

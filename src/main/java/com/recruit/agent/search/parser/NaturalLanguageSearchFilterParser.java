package com.recruit.agent.search.parser;

import com.recruit.agent.search.dto.CandidateSearchFilter;

/**
 * 自然语言搜索过滤条件解析器。
 */
public interface NaturalLanguageSearchFilterParser {

    /**
     * 从自然语言中提取结构化过滤条件。
     *
     * @param text 用户输入
     * @return 解析出的过滤条件
     */
    CandidateSearchFilter parse(String text);

    /**
     * 从自然语言中去掉已识别的过滤条件表达，保留剩余 query。
     *
     * @param text 用户输入
     * @return 清洗后的 query
     */
    String stripFilterTerms(String text);
}

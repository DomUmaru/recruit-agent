package com.recruit.agent.search.rerank;

import java.io.IOException;
import java.util.List;

/**
 * Rerank provider 抽象。
 */
public interface RerankService {

    /**
     * 当前 provider 是否可用。
     */
    boolean isAvailable();

    /**
     * 对 query-document 对进行重排序打分。
     *
     * @param query 用户查询
     * @param documents 候选文档列表
     * @return 与 documents 顺序一致的 rerank 分数
     */
    List<Double> rerank(String query, List<String> documents) throws IOException;
}

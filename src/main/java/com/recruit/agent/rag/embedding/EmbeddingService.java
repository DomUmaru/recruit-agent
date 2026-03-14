package com.recruit.agent.rag.embedding;

import java.io.IOException;
import java.util.List;

/**
 * Embedding provider 抽象。
 */
public interface EmbeddingService {

    /**
     * 当前 provider 是否可用。
     */
    boolean isAvailable();

    /**
     * 批量生成文本向量。
     *
     * @param texts 输入文本
     * @return 与输入顺序一致的向量列表
     */
    List<float[]> embedAll(List<String> texts) throws IOException;
}

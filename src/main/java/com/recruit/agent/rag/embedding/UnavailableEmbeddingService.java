package com.recruit.agent.rag.embedding;

import java.io.IOException;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.embedding", name = "provider", havingValue = "none", matchIfMissing = true)
public class UnavailableEmbeddingService implements EmbeddingService {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public List<float[]> embedAll(List<String> texts) throws IOException {
        throw new IOException("Embedding service is not configured");
    }
}

package com.recruit.agent.search.rerank;

import java.io.IOException;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.rerank", name = "provider", havingValue = "none", matchIfMissing = true)
public class UnavailableRerankService implements RerankService {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public List<Double> rerank(String query, List<String> documents) throws IOException {
        throw new IOException("Rerank service is not configured");
    }
}

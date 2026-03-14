package com.recruit.agent.search.rerank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class BgeRerankServiceImplTest {

    @Test
    void shouldReportAvailableWhenBaseUrlExists() {
        RerankProviderProperties properties = new RerankProviderProperties();
        properties.setProvider("bge-reranker-v2-m3");
        properties.setBaseUrl("http://localhost:9003");

        BgeRerankServiceImpl service = new BgeRerankServiceImpl(properties, new ObjectMapper());

        assertTrue(service.isAvailable());
    }

    @Test
    void shouldFailFastWhenBaseUrlIsMissing() {
        RerankProviderProperties properties = new RerankProviderProperties();
        properties.setProvider("bge-reranker-v2-m3");

        BgeRerankServiceImpl service = new BgeRerankServiceImpl(properties, new ObjectMapper());

        assertThrows(IOException.class, () -> service.rerank("java", List.of("doc")));
    }

    @Test
    void shouldReadScoresFromAdapterResponse() throws Exception {
        RerankProviderProperties properties = new RerankProviderProperties();
        properties.setProvider("bge-reranker-v2-m3");
        properties.setBaseUrl("http://localhost:9003");

        BgeRerankServiceImpl service = new BgeRerankServiceImpl(properties, new ObjectMapper());

        BgeRerankResponse response = new BgeRerankResponse();
        response.setModel("BAAI/bge-reranker-v2-m3");
        response.setScores(List.of(0.9, 0.7));

        assertEquals(List.of(0.9, 0.7), response.getScores());
        assertEquals("BAAI/bge-reranker-v2-m3", response.getModel());
    }
}

package com.recruit.agent.rag.embedding;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;

class BgeM3EmbeddingServiceImplTest {

    @Test
    void shouldReportAvailableWhenBaseUrlExists() {
        EmbeddingProviderProperties properties = new EmbeddingProviderProperties();
        properties.setProvider("bge-m3");
        properties.setBaseUrl("http://localhost:9002");

        BgeM3EmbeddingServiceImpl service = new BgeM3EmbeddingServiceImpl(properties, new ObjectMapper());

        assertTrue(service.isAvailable());
    }

    @Test
    void shouldFailFastWhenBaseUrlIsMissing() {
        EmbeddingProviderProperties properties = new EmbeddingProviderProperties();
        properties.setProvider("bge-m3");

        BgeM3EmbeddingServiceImpl service = new BgeM3EmbeddingServiceImpl(properties, new ObjectMapper());

        assertThrows(IOException.class, () -> service.embedAll(List.of("Java")));
    }

    @Test
    void shouldMapAdapterResponseToFloatVectors() throws Exception {
        EmbeddingProviderProperties properties = new EmbeddingProviderProperties();
        properties.setProvider("bge-m3");
        properties.setBaseUrl("http://localhost:9002");
        properties.setDimension(3);

        BgeM3EmbeddingServiceImpl service = new BgeM3EmbeddingServiceImpl(properties, new ObjectMapper());

        BgeM3EmbeddingResponse response = new BgeM3EmbeddingResponse();
        response.setModel("bge-m3");
        response.setDimension(3);
        response.setEmbeddings(List.of(
            List.of(0.1f, 0.2f, 0.3f),
            List.of(0.4f, 0.5f, 0.6f)
        ));

        Method method = BgeM3EmbeddingServiceImpl.class.getDeclaredMethod("toEmbeddings", BgeM3EmbeddingResponse.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<float[]> vectors = (List<float[]>) method.invoke(service, response);

        assertEquals(2, vectors.size());
        assertArrayEquals(new float[]{0.1f, 0.2f, 0.3f}, vectors.get(0));
        assertArrayEquals(new float[]{0.4f, 0.5f, 0.6f}, vectors.get(1));
    }
}

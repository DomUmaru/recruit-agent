package com.recruit.agent.rag.embedding;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.embedding", name = "provider", havingValue = "bge-m3")
public class BgeM3EmbeddingServiceImpl implements EmbeddingService {

    private final EmbeddingProviderProperties properties;
    private final ObjectMapper objectMapper;

    public BgeM3EmbeddingServiceImpl(EmbeddingProviderProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isAvailable() {
        return properties.getBaseUrl() != null && !properties.getBaseUrl().isBlank();
    }

    @Override
    public List<float[]> embedAll(List<String> texts) throws IOException {
        if (!isAvailable()) {
            throw new IOException("Embedding service is not configured");
        }
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        BgeM3EmbeddingRequest request = new BgeM3EmbeddingRequest();
        request.setTexts(texts);

        try {
            byte[] requestBody = objectMapper.writeValueAsBytes(request);
            HttpURLConnection connection = openConnection();
            connection.setFixedLengthStreamingMode(requestBody.length);

            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(requestBody);
                outputStream.flush();
            }

            int statusCode = connection.getResponseCode();
            String responseBody = readResponseBody(connection, statusCode);
            if (statusCode >= 400) {
                throw new IOException("Embedding service returned status " + statusCode + ": " + responseBody);
            }

            BgeM3EmbeddingResponse response = objectMapper.readValue(responseBody, BgeM3EmbeddingResponse.class);
            return toEmbeddings(response);
        } catch (JsonProcessingException exception) {
            throw new IOException("Failed to serialize or parse embedding payload", exception);
        }
    }

    private HttpURLConnection openConnection() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(properties.getBaseUrl() + properties.getEmbedPath()).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(properties.getConnectTimeoutMs());
        connection.setReadTimeout(properties.getReadTimeoutMs());
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json");
        return connection;
    }

    private String readResponseBody(HttpURLConnection connection, int statusCode) throws IOException {
        InputStream inputStream = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (inputStream == null) {
            return "";
        }
        try (InputStream stream = inputStream) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private List<float[]> toEmbeddings(BgeM3EmbeddingResponse response) throws IOException {
        if (response == null || response.getEmbeddings() == null) {
            return List.of();
        }

        Integer dimension = response.getDimension() == null ? properties.getDimension() : response.getDimension();
        List<float[]> vectors = new ArrayList<>();
        for (List<Float> embedding : response.getEmbeddings()) {
            if (embedding == null) {
                vectors.add(new float[0]);
                continue;
            }
            if (dimension != null && !embedding.isEmpty() && embedding.size() != dimension) {
                throw new IOException("Embedding dimension mismatch. expected=" + dimension + ", actual=" + embedding.size());
            }
            float[] vector = new float[embedding.size()];
            for (int index = 0; index < embedding.size(); index++) {
                Float value = embedding.get(index);
                vector[index] = value == null ? 0.0f : value;
            }
            vectors.add(vector);
        }
        return vectors;
    }
}

package com.recruit.agent.search.rerank;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.rerank", name = "provider", havingValue = "bge-reranker-v2-m3")
public class BgeRerankServiceImpl implements RerankService {

    private final RerankProviderProperties properties;
    private final ObjectMapper objectMapper;

    public BgeRerankServiceImpl(RerankProviderProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isAvailable() {
        return properties.getBaseUrl() != null && !properties.getBaseUrl().isBlank();
    }

    @Override
    public List<Double> rerank(String query, List<String> documents) throws IOException {
        if (!isAvailable()) {
            throw new IOException("Rerank service is not configured");
        }
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        BgeRerankRequest request = new BgeRerankRequest();
        request.setQuery(query);
        request.setDocuments(documents);

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
                throw new IOException("Rerank service returned status " + statusCode + ": " + responseBody);
            }

            BgeRerankResponse response = objectMapper.readValue(responseBody, BgeRerankResponse.class);
            return response == null || response.getScores() == null ? List.of() : response.getScores();
        } catch (JsonProcessingException exception) {
            throw new IOException("Failed to serialize or parse rerank payload", exception);
        }
    }

    private HttpURLConnection openConnection() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(properties.getBaseUrl() + properties.getRerankPath()).openConnection();
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
}

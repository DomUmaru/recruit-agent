package com.recruit.agent.resume.parser.ocr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.ocr", name = "provider", havingValue = "paddle")
public class PaddleOcrServiceImpl implements OcrService {

    private final OcrProviderProperties properties;
    private final ObjectMapper objectMapper;

    public PaddleOcrServiceImpl(OcrProviderProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isAvailable() {
        return properties.getBaseUrl() != null && !properties.getBaseUrl().isBlank();
    }

    @Override
    public OcrResult recognize(Path filePath) throws IOException {
        if (!isAvailable()) {
            throw new IOException("Paddle OCR service is not configured");
        }

        PaddleOcrRecognizeRequest request = new PaddleOcrRecognizeRequest();
        request.setFilename(filePath.getFileName().toString());
        request.setFileBase64(Base64.getEncoder().encodeToString(Files.readAllBytes(filePath)));

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
                throw new IOException("Paddle OCR service returned status " + statusCode + ": " + responseBody);
            }

            PaddleOcrRecognizeResponse response = objectMapper.readValue(responseBody, PaddleOcrRecognizeResponse.class);
            return toOcrResult(response);
        } catch (JsonProcessingException exception) {
            throw new IOException("Failed to serialize or parse Paddle OCR payload", exception);
        }
    }

    private HttpURLConnection openConnection() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(properties.getBaseUrl() + properties.getRecognizePath()).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout((int) properties.getConnectTimeoutMs());
        connection.setReadTimeout((int) properties.getReadTimeoutMs());
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

    private OcrResult toOcrResult(PaddleOcrRecognizeResponse response) {
        OcrResult result = new OcrResult();
        if (response == null) {
            result.setPageTexts(List.of());
            result.setRawText("");
            result.setEngineName("paddle");
            return result;
        }

        List<String> pageTexts = response.getPages() == null
            ? List.of()
            : response.getPages().stream()
                .map(PaddleOcrPageResponse::getText)
                .map(text -> text == null ? "" : text)
                .toList();

        result.setPageTexts(pageTexts);
        result.setRawText(response.getRawText() == null ? String.join("\n\n", pageTexts) : response.getRawText());
        result.setEngineName(response.getEngineName() == null ? "paddle" : response.getEngineName());
        return result;
    }
}

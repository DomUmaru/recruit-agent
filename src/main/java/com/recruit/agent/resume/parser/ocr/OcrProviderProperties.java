package com.recruit.agent.resume.parser.ocr;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ocr")
public class OcrProviderProperties {

    /**
     * none | paddle
     */
    private String provider = "none";

    private String baseUrl;

    private String recognizePath = "/api/ocr/recognize";

    private int connectTimeoutMs = 3000;

    private int readTimeoutMs = 30000;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public String getRecognizePath() {
        return recognizePath;
    }

    public void setRecognizePath(String recognizePath) {
        this.recognizePath = recognizePath;
    }
}

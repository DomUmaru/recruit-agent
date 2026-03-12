package com.recruit.agent.search.rerank;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rerank")
public class RerankProviderProperties {

    /**
     * none | bge-reranker-v2-m3
     */
    private String provider = "none";

    private String baseUrl;

    private String rerankPath = "/api/rerank";

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

    public String getRerankPath() {
        return rerankPath;
    }

    public void setRerankPath(String rerankPath) {
        this.rerankPath = rerankPath;
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
}

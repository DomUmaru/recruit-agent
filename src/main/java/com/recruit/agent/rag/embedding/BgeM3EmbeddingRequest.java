package com.recruit.agent.rag.embedding;

import java.util.List;

public class BgeM3EmbeddingRequest {

    private List<String> texts;

    private Boolean normalize = Boolean.TRUE;

    public List<String> getTexts() {
        return texts;
    }

    public void setTexts(List<String> texts) {
        this.texts = texts;
    }

    public Boolean getNormalize() {
        return normalize;
    }

    public void setNormalize(Boolean normalize) {
        this.normalize = normalize;
    }
}

package com.recruit.agent.rag.embedding;

import java.util.List;

public class BgeM3EmbeddingResponse {

    private String model;

    private Integer dimension;

    private List<List<Float>> embeddings;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getDimension() {
        return dimension;
    }

    public void setDimension(Integer dimension) {
        this.dimension = dimension;
    }

    public List<List<Float>> getEmbeddings() {
        return embeddings;
    }

    public void setEmbeddings(List<List<Float>> embeddings) {
        this.embeddings = embeddings;
    }
}

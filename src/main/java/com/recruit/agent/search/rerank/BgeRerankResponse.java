package com.recruit.agent.search.rerank;

import java.util.List;

public class BgeRerankResponse {

    private String model;

    private List<Double> scores;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Double> getScores() {
        return scores;
    }

    public void setScores(List<Double> scores) {
        this.scores = scores;
    }
}

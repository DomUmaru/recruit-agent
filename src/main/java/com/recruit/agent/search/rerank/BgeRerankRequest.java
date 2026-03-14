package com.recruit.agent.search.rerank;

import java.util.List;

public class BgeRerankRequest {

    private String query;

    private List<String> documents;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<String> getDocuments() {
        return documents;
    }

    public void setDocuments(List<String> documents) {
        this.documents = documents;
    }
}

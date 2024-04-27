package com.example.metasearch.model.request;

public class NLQueryRequest {
    private String dbName;
    private String query;

    public NLQueryRequest(String dbName, String query) {
        this.dbName = dbName;
        this.query = query;
    }

    // Getters and Setters
    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}

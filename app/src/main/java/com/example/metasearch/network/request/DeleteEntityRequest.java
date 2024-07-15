package com.example.metasearch.network.request;

public class DeleteEntityRequest {
    private String dbName;
    private String entityName;
    public DeleteEntityRequest(String dbName, String entityName) {
        this.dbName = dbName;
        this.entityName = entityName;
    }
}

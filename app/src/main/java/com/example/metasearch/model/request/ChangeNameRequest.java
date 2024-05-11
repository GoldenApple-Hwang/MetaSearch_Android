package com.example.metasearch.model.request;

public class ChangeNameRequest {
    private String dbName;
    private String oldName;
    private String newName;

    public ChangeNameRequest(String dbName, String oldName, String newName) {
        this.dbName = dbName;
        this.oldName = oldName;
        this.newName = newName;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getOldName() {
        return oldName;
    }

    public void setOldName(String oldName) {
        this.oldName = oldName;
    }

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }
}

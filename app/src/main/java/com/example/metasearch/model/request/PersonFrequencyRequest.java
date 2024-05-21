package com.example.metasearch.model.request;

import java.util.List;

public class PersonFrequencyRequest {
    private String dbName;
    private List<String> personNames;

    public PersonFrequencyRequest(String dbName, List<String> personNames) {
        this.dbName = dbName;
        this.personNames = personNames;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public List<String> getPersonNames() {
        return personNames;
    }

    public void setPersonNames(List<String> personNames) {
        this.personNames = personNames;
    }
}

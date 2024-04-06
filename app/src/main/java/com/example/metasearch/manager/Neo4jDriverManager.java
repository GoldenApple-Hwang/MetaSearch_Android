package com.example.metasearch.manager;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

public class Neo4jDriverManager {
    private static Driver driver = null;
    private static final String neo4jAddress = "bolt://172.20.10.2:7687";
    private static final String username = "neo4j";
    private static final String password = "00000000";

    public static Driver getDriver() {
        if (driver == null) {
            driver = GraphDatabase.driver(neo4jAddress, AuthTokens.basic(username, password));
        }
        return driver;
    }

    public static void closeDriver() {
        if (driver != null) {
            driver.close();
            driver = null;
        }
    }
}


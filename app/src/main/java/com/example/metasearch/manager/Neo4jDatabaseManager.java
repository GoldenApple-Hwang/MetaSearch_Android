package com.example.metasearch.manager;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Values;

import java.util.ArrayList;
import java.util.List;

public class Neo4jDatabaseManager {
    private final Driver driver;

    public Neo4jDatabaseManager() {
        this.driver = Neo4jDriverManager.getDriver();
    }

    public List<String> fetchPhotoNamesFromNeo4j(String relationship, String entity2) {
        List<String> photoNames = new ArrayList<>();
        String queryString = "MATCH (photo)-[:" + relationship + "]->(entity {name: $entity2}) RETURN photo.name AS PhotoName";
        Result result = driver.session().run(queryString, Values.parameters("entity2", entity2));
        while (result.hasNext()) {
            String photoName = result.next().get("PhotoName").asString();
            photoNames.add(photoName);
        }
        return photoNames;
    }

    // 사이퍼쿼리 생성 함수
    public String createCypherQuery(List<String> entities, List<String> relationships, int count) {
        StringBuilder query = new StringBuilder("MATCH ");
        for (int i = 0; i < count; i++) {
            query.append("(photo)-[:").append(relationships.get(i)).append("]->(entity {name: ").append(entities.get(i)).append("})");
            if (i < count - 1) {
                query.append(", ");
            }
        }
        query.append(" RETURN photo.name AS PhotoName");
        return query.toString();
    }
}

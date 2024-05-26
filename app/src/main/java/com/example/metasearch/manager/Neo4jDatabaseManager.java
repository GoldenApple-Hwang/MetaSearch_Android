package com.example.metasearch.manager;

import java.util.List;

public class Neo4jDatabaseManager {
    // 사이퍼쿼리 생성 함수 : relationship과 entity를 모두 아는 경우
    public static String createCypherQuery(List<String> entities, List<String> relationships, int count) {
        StringBuilder query = new StringBuilder("MATCH ");
        for (int i = 0; i < count; i++) {
//            query.append("(photo)-[:").append(relationships.get(i)).append("]->(entity {name: \"").append(entities.get(i)).append("\"})");
            query.append("(photo)-[:").append(relationships.get(i)).append("]->(a").append(i).append(":Entity {name: \"").append(entities.get(i)).append("\"})");
            if (i < count - 1) {
                query.append(", \n");
            }
        }
        query.append("\n RETURN photo.name AS PhotoName");
        // 반환 형식
        // MATCH (photo)-[:RELATIONSHIP_1]->(a1:Entity {name: "ENTITY_1"}),
        // (photo)-[:RELATIONSHIP_2]->(a2:Entity {name: "ENTITY_2"})
        // RETURN photo.name AS PhotoName
        return query.toString();
    }

    // 사이퍼쿼리 생성 함수 : relationship만 아는 경우
    public static String createCypherQueryForRelationships(List<String> relationships) {
        StringBuilder query = new StringBuilder("MATCH ");
        for (int i = 0; i < relationships.size(); i++) {
            query.append("(photo)-[:").append(relationships.get(i)).append("]->(entity)");
            if (i < relationships.size() - 1) {
                query.append(", ");
            }
        }
        query.append(" RETURN photo.name AS PhotoName");
        // 반환 형식
        // MATCH
        //(photo)-[:RELATIONSHIP_1]->(entity),
        //(photo)-[:RELATIONSHIP_2]->(entity),
        //...
        //RETURN photo.name AS PhotoName
        return query.toString();
    }
}

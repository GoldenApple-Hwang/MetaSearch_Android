package com.example.metasearch.network.response;

import java.util.List;

public class CircleDetectionResponse {
    private String message;
    private List<String> detected_objects; // 원에서 분석된 객체 이름 리스트

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getDetectedObjects() {
        return detected_objects;
    }

    public void setDetectedObjects(List<String> detectedObjects) {
        this.detected_objects = detectedObjects;
    }
}

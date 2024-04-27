package com.example.metasearch.model;

import java.util.List;

public class NLQueryResponse {
    private List<String> PhotoName; // 필드명을 PhotoName으로 변경

    public List<String> getPhotoName() {
        return PhotoName;
    }

    public void setPhotoName(List<String> photoName) {
        this.PhotoName = photoName;
    }
}
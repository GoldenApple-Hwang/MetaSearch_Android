package com.example.metasearch.model;

public class Person {
    private String name;
    private String imageUrl; // 홈 화면 상단에 작게 표시되는 얼굴 이미지를 위한 데이터
//    private Uri imageUrl; // 추후 데이터 타입 수정 필요

    public Person(String name, String imageUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
    }
    public String getName() {
        return name;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}


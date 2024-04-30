package com.example.metasearch.model;

public class Person {
    private String name;
    private byte[] image; // 홈 화면 상단에 작게 표시되는 얼굴 이미지를 위한 데이터
//    private Uri imageUrl; // 추후 데이터 타입 수정 필요

    public Person(String name, byte[] image) {
        this.name = name;
        this.image = image;
    }
    public String getName() {
        return name;
    }

    public byte[] getImage() {
        return image;
    }
}


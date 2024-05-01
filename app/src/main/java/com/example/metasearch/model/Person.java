package com.example.metasearch.model;

public class Person {
    private String imageName; // 사진 이름
    private String userName; // 사용자가 입력한 이름
    private byte[] image; // 홈 화면 상단에 작게 표시되는 얼굴 이미지를 위한 데이터

    public Person(String imageName, String name, byte[] image) {
        this.imageName = imageName;
        this.userName = name;
        this.image = image;
    }
    public String getImageName() { return imageName; }
    public String getName() {
        return userName;
    }
    public byte[] getImage() {
        return image;
    }
}


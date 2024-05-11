package com.example.metasearch.model;

public class Person {
    private String imageName; // 사진 이름
    private String userName; // 사용자가 입력한 이름
    private byte[] image; // 홈 화면 상단에 작게 표시되는 얼굴 이미지를 위한 데이터
    private String phone; // 인물의 전화번호. 기본값은 "".
    private Integer isDelete; // 삭제된 인물인지 나타냄. 기본값은 1, 삭제되면 0

    public Person(String imageName, byte[] image) {
        this.imageName = imageName;
        this.userName = userName != null ? userName : "";
        this.image = image;
        this.phone = "";
        this.isDelete = 0;
    }
    public String getImageName() { return imageName; }
    public String getUserName() {
        return userName;
    }
    public byte[] getImage() {
        return image;
    }
    public String getPhone() { return phone; }
    public Integer getIsDelete() { return isDelete; }
    public void setImageName(String imageName) {
        this.imageName = imageName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }
    public void setImage(byte[] image) {
        this.image = image;
    }
    public void setPhone(String phone) {
        this.phone = phone != null ? phone : "";
    }
    public void setIsDelete(Integer isDelete) {
        this.isDelete = isDelete;
    }
}


//public class Person {
//    private String imageName; // 사진 이름
//    private String userName; // 사용자가 입력한 이름
//    private byte[] image; // 홈 화면 상단에 작게 표시되는 얼굴 이미지를 위한 데이터
//
//    public Person(String imageName, String name, byte[] image) {
//        this.imageName = imageName;
//        this.userName = name;
//        this.image = image;
//    }
//    public String getImageName() { return imageName; }
//    public String getName() {
//        return userName;
//    }
//    public byte[] getImage() {
//        return image;
//    }
//}
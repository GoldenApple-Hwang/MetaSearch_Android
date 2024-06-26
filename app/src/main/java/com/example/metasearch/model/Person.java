package com.example.metasearch.model;

public class Person {
    private int id; // 고유 ID
    private String imageName; // 사진 이름
    private String inputName; // 사용자가 입력한(수정한) 인물 이름
    private byte[] image; // 홈 화면 상단에 작게 표시되는 얼굴 이미지를 위한 데이터
    private String phone; // 인물의 전화번호. 기본값은 "".
    private Integer isDelete; // 삭제된 인물인지 나타냄. 기본값은 1, 삭제되면 0
    private long totalDuration; // 총 통화 시간
    private double rank;
    public Person(int id, String imageName, byte[] image) {
        this.id = id;
        this.imageName = imageName;
        this.image = image;
        this.inputName = "";
        this.phone = "";
        this.isDelete = 0;
        this.totalDuration = 0;
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getInputName() {
        return inputName;
    }
    public void setInputName(String inputName) {
        this.inputName = inputName;
    }
    public String getImageName() { return imageName; }
    public byte[] getImage() {
        return image;
    }
    public String getPhone() { return phone; }
    public Integer getIsDelete() { return isDelete; }
    public void setImageName(String imageName) {
        this.imageName = imageName;
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
    public long getTotalDuration() {
        return totalDuration;
    }
    public void setTotalDuration(long totalDuration) {
        this.totalDuration = totalDuration;
    }
    public void setRank(double rank) {
        this.rank = rank;
    }

    public double getRank() {
        return rank;
    }
}

package com.example.metasearch.model.response;

import java.util.List;

public class UploadResponse {

    private List<ImageData> images;

    public List<ImageData> getImages() {
        return images;
    }

    public static class ImageData {
        private String imageName;
        private String imageBytes;
        private String division;
        private boolean isFaceExit;

        public String getImageName() {
            return imageName;
        }

        public String getImageBytes() {
            return imageBytes;
        }
        public boolean getIsExit(){return isFaceExit;}

        public java.lang.String getDivision() {
            return division;
        }
    }
}

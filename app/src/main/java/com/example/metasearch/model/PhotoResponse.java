package com.example.metasearch.model;

import java.util.List;
import java.util.Map;

public class PhotoResponse {
    private Photos photos;

    public Photos getPhotos() {
        return photos;
    }

    public static class Photos {
        private List<String> commonPhotos;
        private Map<String, List<String>> individualPhotos;

        public List<String> getCommonPhotos() {
            return commonPhotos;
        }

        public Map<String, List<String>> getIndividualPhotos() {
            return individualPhotos;
        }
    }
}

package com.example.metasearch.interfaces;

import com.example.metasearch.model.response.PhotoResponse;

public interface WebServerUploadCallbacks {
    void onWebServerUploadSuccess(PhotoResponse detectedObjects);
    void onWebServerUploadFailure(String message);
}

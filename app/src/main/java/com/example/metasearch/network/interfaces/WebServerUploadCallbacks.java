package com.example.metasearch.network.interfaces;

import com.example.metasearch.network.response.PhotoResponse;

public interface WebServerUploadCallbacks {
    void onWebServerUploadSuccess(PhotoResponse detectedObjects);
    void onWebServerUploadFailure(String message);
}

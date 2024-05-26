package com.example.metasearch.interfaces;

import com.example.metasearch.model.response.PhotoNameResponse;

public interface WebServerQueryCallbacks {
    void onWebServerQuerySuccess(PhotoNameResponse photoNameResponse);
    void onWebServerQueryFailure();
}
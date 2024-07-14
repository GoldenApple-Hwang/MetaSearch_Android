package com.example.metasearch.network.interfaces;

import com.example.metasearch.network.response.PhotoNameResponse;

public interface WebServerQueryCallbacks {
    void onWebServerQuerySuccess(PhotoNameResponse photoNameResponse);
    void onWebServerQueryFailure();
}
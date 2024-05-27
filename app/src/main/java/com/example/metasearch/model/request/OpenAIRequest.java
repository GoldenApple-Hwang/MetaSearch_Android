package com.example.metasearch.model.request;

import com.example.metasearch.model.Message;

import java.util.List;

public class OpenAIRequest {
    private String model;
    private List<Message> messages;
    private double temperature;  // temperature 필드 추가
    private double top_p;         // top_p 필드 추가

    public OpenAIRequest(String model, List<Message> messages, double temperature, double topP) {
        this.model = model;
        this.messages = messages;
        this.temperature = temperature;
        this.top_p = topP;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getTop_p() {
        return top_p;
    }
}

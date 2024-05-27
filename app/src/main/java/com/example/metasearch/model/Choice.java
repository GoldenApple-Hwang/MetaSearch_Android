package com.example.metasearch.model;

public class Choice {
    private Message message;

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Choice{" +
                "message=" + (message != null ? message.toString() : "null") +
                '}';
    }
}

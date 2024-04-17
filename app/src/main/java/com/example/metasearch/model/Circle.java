package com.example.metasearch.model;

import android.graphics.PointF;

//public class Circle {
//    public PointF center;
//    public float radius;
//
//    public Circle(PointF center, float radius) {
//        this.center = center;
//        this.radius = radius;
//    }
//}

public class Circle {
    private float centerX;
    private float centerY;
    private float radius;

    public Circle(float centerX, float centerY, float radius) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
    }

    public float getCenterX() {
        return centerX;
    }

    public float getCenterY() {
        return centerY;
    }

    public float getRadius() {
        return radius;
    }

    // Setters omitted for brevity
}

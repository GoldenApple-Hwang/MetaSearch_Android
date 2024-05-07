package com.example.metasearch.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import com.example.metasearch.R;
import com.example.metasearch.model.Circle;

public class CustomImageView extends androidx.appcompat.widget.AppCompatImageView {
    private final List<Circle> circles = new ArrayList<>(); // 원들을 저장할 리스트
    private Paint paint;
    private float startX, startY, currentX, currentY;
    private boolean isDrawing = false;

    public CustomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    private void init(Context context) {
        int color = ContextCompat.getColor(context, R.color.white);
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6);
    }
    public List<Circle> getCircles() {
        return new ArrayList<>(circles);  // 원 정보를 포함하는 리스트 반환
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Circle circle : circles) {
            float drawCenterX = circle.getCenterX() * getWidth();
            float drawCenterY = circle.getCenterY() * getHeight();
            float drawRadius = circle.getRadius() * Math.max(getWidth(), getHeight());
            canvas.drawCircle(drawCenterX, drawCenterY, drawRadius, paint);
        }
        if (isDrawing) {
            float radius = (float) Math.hypot(currentX - startX, currentY - startY) / 2;
            canvas.drawCircle((startX + currentX) / 2, (startY + currentY) / 2, radius, paint);
        }
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (circles.size() >= 5) {
                    Toast.makeText(getContext(), "최대 5개의 원만 그릴 수 있습니다.", Toast.LENGTH_SHORT).show();
                    return false;  // 더 이상 그리지 않음
                }
                startX = event.getX();
                startY = event.getY();
                isDrawing = true;
                break;
            case MotionEvent.ACTION_MOVE:
                currentX = event.getX();
                currentY = event.getY();
                invalidate();  // 캔버스를 다시 그리도록 요청
                break;
            case MotionEvent.ACTION_UP:
                float radius = (float) Math.hypot(currentX - startX, currentY - startY) / 2;
                float normalizedCenterX = (startX + currentX) / 2 / getWidth();
                float normalizedCenterY = (startY + currentY) / 2 / getHeight();
                float normalizedRadius = radius / Math.max(getWidth(), getHeight());

                Log.d("CIRCLE_RADIUS", String.valueOf(radius));
                Log.d("CIRCLE_X", String.valueOf(currentX));
                Log.d("CIRCLE_y", String.valueOf(currentY));

                Log.d("NORMALIZED_RADIUS", String.valueOf(normalizedRadius));
                Log.d("NORMALIZED_X", String.valueOf(normalizedCenterX));
                Log.d("NORMALIZED_Y", String.valueOf(normalizedCenterY));

                circles.add(new Circle(normalizedCenterX, normalizedCenterY, normalizedRadius));
                isDrawing = false;
                invalidate();
                break;
        }
        return true;  // 이벤트를 처리했음을 시스템에 알림
    }
    public void setImageUri(Uri imageUri) {
        // 이미지 설정 메서드 추가
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(getContext().getContentResolver().openInputStream(imageUri));
            setImageBitmap(bitmap);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    public void clearCircles() {
        circles.clear();
        invalidate(); // 화면을 다시 그림
    }
}

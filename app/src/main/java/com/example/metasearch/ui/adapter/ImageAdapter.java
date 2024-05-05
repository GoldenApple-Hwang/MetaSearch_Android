package com.example.metasearch.ui.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.metasearch.R;

import java.util.ArrayList;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
    private List<Uri> imageUris;
    private Context context;
    private OnImageClickListener listener;

    public ImageAdapter(List<Uri> imageUris, Context context, OnImageClickListener listener) {
        this.imageUris = (imageUris != null) ? imageUris : new ArrayList<>(); // null 체크 추가
        this.context = context;
        this.listener = listener;
    }

    // 데이터 업데이트 메소드
    public void updateData(List<Uri> uris) {
        this.imageUris = uris; // 새로운 URI 리스트로 업데이트
        notifyDataSetChanged(); // RecyclerView를 갱신
    }

    // 이미지 클릭 리스너 인터페이스 정의
    public interface OnImageClickListener {
        void onImageClick(Uri uri);
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.photo_item, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ImageViewHolder holder, int position) {
        Uri imageUri = imageUris.get(position);
//        holder.imageView.setImageURI(imageUri);
        // Glide를 사용하여 이미지 로드 및 표시(속도 향상)
        Glide.with(context)
                .load(imageUri)
                .into(holder.imageView);

        // 클릭 이벤트 설정
        holder.imageView.setOnClickListener(v -> {
            if (imageUri != null) {
                listener.onImageClick(imageUri);
            }
        });

        // 화면 너비의 1/3 크기로 이미지 뷰의 크기를 조절
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int imageSize = screenWidth / 5;

        ViewGroup.LayoutParams layoutParams = holder.imageView.getLayoutParams();
        layoutParams.width = imageSize;
        layoutParams.height = imageSize;
        holder.imageView.setLayoutParams(layoutParams);
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageView;

        public ImageViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }
}

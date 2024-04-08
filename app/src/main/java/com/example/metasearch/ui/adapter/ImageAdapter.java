package com.example.metasearch.ui.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.metasearch.R;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
    private List<Uri> imageUris;
    private Context context;
    private OnImageClickListener listener;

    public ImageAdapter(List<Uri> imageUris, Context context, OnImageClickListener listener) {
        this.imageUris = imageUris;
        this.context = context;
        this.listener = listener;
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
        holder.imageView.setImageURI(imageUri);

        // 클릭 이벤트 설정
        holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (imageUri != null) {
                    listener.onImageClick(imageUri);
                }
            }
        });

        // 화면 너비의 1/3 크기로 이미지 뷰의 크기를 조절
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int imageSize = screenWidth / 3;

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


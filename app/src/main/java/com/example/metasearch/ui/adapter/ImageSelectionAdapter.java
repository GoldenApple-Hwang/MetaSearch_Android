package com.example.metasearch.ui.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.metasearch.R;

import java.util.List;

public class ImageSelectionAdapter extends RecyclerView.Adapter<ImageSelectionAdapter.ViewHolder> {

    private final Context context;
    private final List<Bitmap> images;
    private final OnImageClickListener listener;

    public interface OnImageClickListener {
        void onImageClick(Bitmap bitmap);
    }

    public ImageSelectionAdapter(Context context, List<Bitmap> images, OnImageClickListener listener) {
        this.context = context;
        this.images = images;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.thumbnail_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Bitmap image = images.get(position);
        holder.imageView.setImageBitmap(image);
        holder.imageView.setOnClickListener(v -> listener.onImageClick(image));
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }
}

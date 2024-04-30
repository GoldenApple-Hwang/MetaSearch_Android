package com.example.metasearch.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.metasearch.R;
import com.example.metasearch.model.Person;
import com.example.metasearch.ui.activity.PersonPhotosActivity;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class PersonAdapter extends RecyclerView.Adapter<PersonAdapter.PersonViewHolder> {
    private final List<Person> people;
    private ImageAdapter.OnImageClickListener listener;
    private final Context context;

    public PersonAdapter(List<Person> people, ImageAdapter.OnImageClickListener listener, Context context) {
        this.people = people;
        this.listener = listener;
        this.context = context;
    }

    public interface OnImageClickListener {
        void onImageClick(Uri uri);
    }

    public static class PersonViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imageView;
        TextView nameView;

        public PersonViewHolder(View view) {
            super(view);
            imageView = view.findViewById(R.id.face); // CircleImageView ID 수정 필요
            nameView = view.findViewById(R.id.name);
        }
    }

    @NonNull
    @Override
    public PersonViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.person_item, parent, false);
        return new PersonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PersonViewHolder holder, int position) {
        Person person = people.get(position);
        holder.nameView.setText(person.getName());

        // 바이트 배열을 Bitmap으로 변환
        Bitmap imageBitmap = BitmapFactory.decodeByteArray(person.getImage(), 0, person.getImage().length);
        holder.imageView.setImageBitmap(imageBitmap);

        // 클릭 이벤트 설정
        holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, PersonPhotosActivity.class);
                intent.putExtra("personName", person.getName()); // 예시로 personName을 전달
                context.startActivity(intent);
            }
        });

//        holder.imageView.setImageURI(person.getImageUrl());
        // 인물 이름으로 사이퍼쿼리 생성 후 서버에서 받은 데이터로 리사이클러뷰에 이미지 로딩

    }

    @Override
    public int getItemCount() {
        return people.size();
    }
}


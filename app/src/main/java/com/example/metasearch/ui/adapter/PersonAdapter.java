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
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.metasearch.R;
import com.example.metasearch.dao.DatabaseHelper;
import com.example.metasearch.model.Person;
import com.example.metasearch.ui.activity.PersonPhotosActivity;
import com.example.metasearch.ui.fragment.HomeFragment;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class PersonAdapter extends RecyclerView.Adapter<PersonAdapter.PersonViewHolder> {
    private DatabaseHelper databaseHelper;
    private final List<Person> people;
    private ImageAdapter.OnImageClickListener listener;
    private final Context context;

    public PersonAdapter(List<Person> people, ImageAdapter.OnImageClickListener listener, Context context) {
        this.people = people;
        this.listener = listener;
        this.context = context;
        databaseHelper = DatabaseHelper.getInstance(context);
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
    private void showDeletePersonDialog(int personId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomAlertDialogTheme);
        builder.setTitle("인물 리스트 수정");
        builder.setMessage("이 인물을 '내가 아는 사람들'에서 삭제하시겠습니까?");
        builder.setPositiveButton("삭제", (dialog, which) -> {
            // 데이터 삭제 로직 실행
            deletePerson(personId);
        });
        builder.setNegativeButton("취소", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
    // 실제 삭제는 아니고, 화면에 표시 여부만 변경
    private void deletePerson(int personId) {
        databaseHelper.markPersonAsDeleted(personId);
        // 화면 업데이트 필요
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
//        holder.nameView.setText(person.getUserName());
        holder.nameView.setText(person.getInputName());

        // 바이트 배열을 Bitmap으로 변환
        Bitmap imageBitmap = BitmapFactory.decodeByteArray(person.getImage(), 0, person.getImage().length);
        holder.imageView.setImageBitmap(imageBitmap);

        // 클릭 이벤트 설정
        holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, PersonPhotosActivity.class);
                intent.putExtra("id", person.getId());
                context.startActivity(intent); // 클릭한 인물이 나온 사진을 모두 찾아서 보여주는 화면으로 전환
            }
        });

        holder.imageView.setOnLongClickListener(v -> {
            showDeletePersonDialog(person.getId());
            return true; // 롱 클릭 이벤트 처리 완료
        });
    }
    @Override
    public int getItemCount() {
        return people.size();
    }
}


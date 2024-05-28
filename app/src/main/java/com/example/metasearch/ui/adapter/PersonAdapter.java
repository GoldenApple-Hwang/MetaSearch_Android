package com.example.metasearch.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.metasearch.R;
import com.example.metasearch.dao.DatabaseHelper;
import com.example.metasearch.helper.DatabaseUtils;
import com.example.metasearch.interfaces.WebServerDeleteEntityCallbacks;
import com.example.metasearch.manager.WebRequestManager;
import com.example.metasearch.model.Person;
import com.example.metasearch.ui.activity.PersonPhotosActivity;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import io.github.muddz.styleabletoast.StyleableToast;

public class PersonAdapter extends RecyclerView.Adapter<PersonAdapter.PersonViewHolder>
                            implements WebServerDeleteEntityCallbacks {
    private DatabaseHelper databaseHelper;
    private List<Person> people;
    private ImageAdapter.OnImageClickListener listener;
    private final Context context;
    private final WebRequestManager webRequestManager;

    public PersonAdapter(List<Person> people, ImageAdapter.OnImageClickListener listener, Context context) {
        this.people = people;
        this.listener = listener;
        this.context = context;
        this.databaseHelper = DatabaseHelper.getInstance(context);
        this.webRequestManager = WebRequestManager.getWebImageUploader();
    }
    public static class PersonViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imageView;
        TextView nameView;
        ImageView deleteIcon;

        public PersonViewHolder(View view) {
            super(view);
            imageView = view.findViewById(R.id.face); // CircleImageView ID 수정 필요
            nameView = view.findViewById(R.id.name);
            deleteIcon = view.findViewById(R.id.delete_icon);
        }
    }
    private void showDeletePersonDialog(String name, int personId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomAlertDialogTheme);
        builder.setTitle("인물 등록 해제");

        builder.setMessage("'" + name + "'님을 '내가 아는 사람들'에서 삭제하시겠습니까?");
        builder.setPositiveButton("삭제", (dialog, which) -> {
            // 데이터 삭제 로직 실행
            deletePerson(name);
        });
        builder.setNegativeButton("취소", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
    private void deletePerson(String inputName) {
        databaseHelper.deletePersonByName(inputName);
        // Web 서버에 엔티티 삭제 요청
        webRequestManager.deleteEntity(DatabaseUtils.getPersistentDeviceDatabaseName(context), inputName, this);
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
        holder.deleteIcon.setOnClickListener(v ->
                showDeletePersonDialog(person.getInputName(), person.getId()));
    }
    @Override
    public int getItemCount() {
        return people.size();
    }
    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onDeleteEntitySuccess(String message) {
        StyleableToast.makeText(context, "인물 등록 해제 완료", R.style.customToast).show();
        // 삭제 성공 시 화면 업데이트
        people.clear();
        people.addAll(databaseHelper.getAllPerson());
        notifyDataSetChanged();
    }
    public void updateData(List<Person> newPeople) {
        this.people = newPeople;
        notifyDataSetChanged();
    }
    @Override
    public void onDeleteEntityFailure(String message) {
        StyleableToast.makeText(context, "삭제 실패: " + message, R.style.customToast).show();
    }
}


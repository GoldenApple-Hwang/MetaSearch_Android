package com.example.metasearch.ui.adapter;

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
import com.example.metasearch.interfaces.WebServerDeleteEntityCallbacks;
import com.example.metasearch.manager.WebRequestManager;
import com.example.metasearch.model.Person;
import com.example.metasearch.ui.activity.PersonPhotosActivity;
import com.example.metasearch.ui.viewmodel.PersonViewModel;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import io.github.muddz.styleabletoast.StyleableToast;

public class PersonAdapter extends RecyclerView.Adapter<PersonAdapter.PersonViewHolder>
        implements WebServerDeleteEntityCallbacks {

    private List<Person> people;
    private final Context context;
    private final PersonViewModel personViewModel;

    public PersonAdapter(Context context, PersonViewModel personViewModel) {
        this.context = context;
        this.personViewModel = personViewModel;
    }

    public void setPeople(List<Person> people) {
        this.people = people;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PersonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.person_item, parent, false);
        return new PersonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PersonViewHolder holder, int position) {
        Person person = people.get(position);
        holder.bind(person);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, PersonPhotosActivity.class);
            intent.putExtra("person_id", person.getId());
            context.startActivity(intent);
        });

        holder.deleteIcon.setOnClickListener(v -> {
            showDeleteConfirmationDialog(person);
        });
    }

    private void showDeleteConfirmationDialog(Person person) {
        new AlertDialog.Builder(context, R.style.CustomAlertDialogTheme)
                .setTitle("인물 등록 취소")
                .setMessage("'" + person.getInputName() + "'님을 '내가 아는 사람들'에서 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> {
                    personViewModel.deletePerson(person);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return people == null ? 0 : people.size();
    }

    @Override
    public void onDeleteEntitySuccess(String message) {
        StyleableToast.makeText(context, "인물 삭제 완료", R.style.customToast).show();
    }

    @Override
    public void onDeleteEntityFailure(String message) {
        StyleableToast.makeText(context, "인물 삭제 실패: " + message, R.style.customToast).show();
    }

    public class PersonViewHolder extends RecyclerView.ViewHolder {

        private final CircleImageView imageView;
        private final TextView nameView;
        private final ImageView deleteIcon;

        public PersonViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.face);
            nameView = itemView.findViewById(R.id.name);
            deleteIcon = itemView.findViewById(R.id.delete_icon);
        }

        public void bind(Person person) {
            nameView.setText(person.getInputName());
            if (person.getImage() != null) {
                Bitmap imageBitmap = BitmapFactory.decodeByteArray(person.getImage(), 0, person.getImage().length);
                imageView.setImageBitmap(imageBitmap);
            } else {
                imageView.setImageResource(R.drawable.ic_launcher_foreground);
            }
        }
    }
}

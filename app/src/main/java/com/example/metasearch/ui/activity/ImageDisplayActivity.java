package com.example.metasearch.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.metasearch.R;
import com.example.metasearch.databinding.ActivityImageDisplayBinding;
import com.example.metasearch.helper.DatabaseUtils;
import com.example.metasearch.manager.ChatGPTManager;
import com.example.metasearch.manager.GalleryImageManager;
import com.example.metasearch.manager.WebRequestManager;
import com.example.metasearch.model.Message;
import com.example.metasearch.model.response.OpenAIResponse;
import com.example.metasearch.model.response.TripleResponse;

import java.util.ArrayList;
import java.util.List;

import io.github.muddz.styleabletoast.StyleableToast;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ImageDisplayActivity extends AppCompatActivity {
    private ActivityImageDisplayBinding binding;
    private String imageUriString;
    private WebRequestManager webRequestManager; // 웹 요청 관리자 추가
    private ChatGPTManager chatGPTManager;
    private static final String TEXT_VIEW_TAG = "descriptionTextView";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityImageDisplayBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        init();
        if (imageUriString != null) {
            Uri imageUri = Uri.parse(imageUriString);
            setImageToPhotoView(imageUri);
        } else {
            StyleableToast.makeText(this, "이미지를 불러올 수 없습니다.", R.style.customToast).show();
        }
        setListeners();
    }
    private void init() {
        webRequestManager = WebRequestManager.getWebImageUploader();
        // Intent에서 이미지 URI 추출
        imageUriString = getIntent().getStringExtra("imageUri");
        chatGPTManager = ChatGPTManager.getInstance();
    }
    public void setListeners() {
        // BottomNavigationView 리스너 설정
        binding.imageMenu.setOnNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.comment) {
                // 챗 지피티 사용해 이미지 설명 구현
                // 사진 위에 설명 텍스트를 바로 띄움
                fetchAndDisplayTripleData(); // csv 데이터 요청 및 표시
                return false;
            } else if (item.getItemId() == R.id.graph) {
                // 그래프 액티비티 실행
                startGraphActivity();
            } else if (item.getItemId() == R.id.search) {
                // 써클 투 써치 액티비티 실행
                startCircleToSearchActivity();
            } else if (item.getItemId() == R.id.share) {
                // 사진 공유 기능 동작
                shareImage();
                return false;
            }
            return true;
        });
    }
    private void fetchAndDisplayTripleData() {
        Uri imageUri = Uri.parse(imageUriString);
        String photoName = GalleryImageManager.getFileNameFromUri(this, imageUri);  // 파일 이름 추출

        Log.d("PHOTONAME",photoName);
        webRequestManager.fetchTripleData(
                DatabaseUtils.getPersistentDeviceDatabaseName(this),
                photoName,
                new Callback<TripleResponse>() {
                    @Override
                    public void onResponse(Call<TripleResponse> call, Response<TripleResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String tripleData = response.body().getTriple();
                            // UI 스레드에서 UI 업데이트 실행
                            runOnUiThread(() -> {
                                displayTripleData(tripleData);
                            });
                        } else {
                            runOnUiThread(() -> {
                                StyleableToast.makeText(ImageDisplayActivity.this, "Failed to load triple data.", R.style.customToast).show();
                            });
                        }
                    }
                    @Override
                    public void onFailure(Call<TripleResponse> call, Throwable t) {
                        runOnUiThread(() -> {
                            StyleableToast.makeText(ImageDisplayActivity.this, "Error: " + t.getMessage(), R.style.customToast).show();
                        });
                    }
                });
    }
    private void displayTripleData(String tripleData) {
        List<Message> messages = createMessagesFromTripleData(tripleData);
        chatGPTManager.getChatResponse(messages, 150, new Callback<OpenAIResponse>() {
            @Override
            public void onResponse(Call<OpenAIResponse> call, Response<OpenAIResponse> response) {
                if (response.isSuccessful()) {
                    OpenAIResponse openAIResponse = response.body();
                    if (openAIResponse != null && !openAIResponse.getChoices().isEmpty()) {
                        // Choice 객체에서 Message 객체를 가져오고, Message 객체의 내용(content)을 추출
                        Message message = openAIResponse.getChoices().get(0).getMessage();
                        if (message != null) {
                            String responseText = message.getContent();
                            runOnUiThread(() -> addTextViewOnImage(responseText));
                            Log.d("GPT", "Response: " + responseText);
                        } else {
                            runOnUiThread(() -> StyleableToast.makeText(ImageDisplayActivity.this, "No message found in response.", R.style.customToast).show());
                        }
                    } else {
                        runOnUiThread(() -> StyleableToast.makeText(ImageDisplayActivity.this, "No choices found in response.", R.style.customToast).show());
                    }
                } else {
                    runOnUiThread(() -> StyleableToast.makeText(ImageDisplayActivity.this, "Failed to get response from ChatGPT.", R.style.customToast).show());
                    Log.e("GPT", "API Response Error: " + response.errorBody());
                }
            }
            @Override
            public void onFailure(Call<OpenAIResponse> call, Throwable t) {
                runOnUiThread(() -> StyleableToast.makeText(ImageDisplayActivity.this, "Error: " + t.getMessage(), R.style.customToast).show());
                Log.e("GPT", "API Call Failure: " + t);
            }
        });
    }
    private void addTextViewOnImage(String text) {
        binding.tripleDataTextView.setText(text);
        binding.tripleDataTextView.setTextSize(16);
        binding.tripleDataTextView.setTextColor(getResources().getColor(R.color.light_pink)); // 글자색 설정
//        binding.tripleDataTextView.setBackgroundResource(R.drawable.rounded_button); // 배경 리소스 설정
    }

    private List<Message> createMessagesFromTripleData(String tripleData) {
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", tripleData + " 이건 사진의 트리플이야. 반드시 이 내용만 사용해서 어떤 사진인지 설명해. ")); // 트리플 데이터를 그대로 메시지에 추가
        return messages;
    }

    private void startGraphActivity() {
        Intent intent = new Intent(this, GraphDisplayActivity.class);
        intent.putExtra("imageUri", imageUriString);
        startActivity(intent);
    }
    private void startCircleToSearchActivity() {
        Intent intent = new Intent(this, CircleToSearchActivity.class);
        intent.putExtra("imageUri", imageUriString);
        startActivity(intent);
    }
    private void shareImage() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUriString);
        shareIntent.setType("image/*");
        startActivity(Intent.createChooser(shareIntent, "사진 공유하기"));
    }
    private void setImageToPhotoView(Uri imageUri) {
        // Glide를 사용하여 PhotoView에 이미지 설정
        Glide.with(this)
                .load(imageUri)
                .into(binding.imageViewFullScreen);
    }
}

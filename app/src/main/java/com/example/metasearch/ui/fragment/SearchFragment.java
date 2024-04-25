package com.example.metasearch.ui.fragment;

import static com.example.metasearch.manager.GalleryImageManager.findMatchedUris;
import static com.example.metasearch.manager.GalleryImageManager.getAllImageNamesWithoutExtension;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.metasearch.R;
import com.example.metasearch.ui.adapter.CustomArrayAdapter;
import com.example.metasearch.ui.adapter.ImageAdapter;
import com.example.metasearch.databinding.FragmentSearchBinding;
import com.example.metasearch.service.gptChat.Choice;
import com.example.metasearch.manager.Neo4jDatabaseManager;
import com.example.metasearch.manager.Neo4jDriverManager;
import com.example.metasearch.service.gptChat.OpenAIResponse;
import com.example.metasearch.service.gptChat.OpenAIServiceManager;
import com.example.metasearch.ui.activity.CircleToSearchActivity;
import com.example.metasearch.ui.viewmodel.ImageViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment implements ImageAdapter.OnImageClickListener {

    private ImageViewModel imageViewModel;
    private final Neo4jDatabaseManager Neo4jDatabaseManager = new Neo4jDatabaseManager();
    private OpenAIServiceManager openAIServiceManager = new OpenAIServiceManager();
    private FragmentSearchBinding binding;
    private String userInputText = ""; // 사용자 입력을 추적하는 변수
    private String neo4jQuery = ""; // Neo4j 서버에 보낼 쿼리문

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        imageViewModel = new ViewModelProvider(this).get(ImageViewModel.class);
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        imageViewModel.getImageUris().observe(getViewLifecycleOwner(), this::updateRecyclerView);

        setupRecyclerView();

        setupAutoCompleteTextView();
        binding.searchButton.setOnClickListener(v -> retrieve());
        binding.imageButton.setOnClickListener(v -> photoSearch());
        return root;
    }

    // OpenAI API 사용해서 사진 검색
    public void photoSearch() {
        // 사용자가 검색한 문장
        String userInput = binding.searchText.getText().toString();
        // 사용자가 아무것도 입력하지 않고 검색 버튼 클릭 시, api 호출하지 않도록 리턴
        if (userInput.length() == 0) return;

        // 사용자가 입력한 문장(찾고 싶은 사진) + gpt가 분석할 수 있도록 지시할 문장
        userInput = userInput + getString(R.string.user_input_eng);

        // OpenAIServiceHelper를 사용하여 API 호출
        openAIServiceManager.fetchOpenAIResponse(userInput, new Callback<OpenAIResponse>() {
            @Override
            public void onResponse(@NonNull Call<OpenAIResponse> call, @NonNull Response<OpenAIResponse> response) {
                // 응답 처리 로직
                // 성공적으로 응답을 받았을 경우
                if (response.isSuccessful()) {
                    StringBuilder msg = new StringBuilder();
                    OpenAIResponse openAIResponse = response.body();
                    assert openAIResponse != null;
                    for (Choice choice : openAIResponse.getChoices()) {
                        String text = choice.getMessage().getContent().trim();
                        // 여기에서 응답 처리, 예: TextView에 출력
                        msg.append(text).append("\n");
                        System.out.println("response : " + text); // test
                        // 응답 형식 : "추출된 속성 쌍의 개수" "entity2[0]" "relationship[0]" "entity2[1]" "relationship[1]"
                        // "2,짱구,인물,사과,과일"
                        String[] parts = text.split(",");
                        if (parts.length > 0) {
                            int pairCount = 0;
                            try {
                                pairCount = Integer.parseInt(parts[0].trim()); // 공백 제거
                                System.out.println("pairCount : " + pairCount);
                            } catch (NumberFormatException e) {
                                Log.e("Parse Error", "Error parsing pair count", e);
                            }
                            List<String> entities = new ArrayList<>();
                            List<String> relationships = new ArrayList<>();

                            int maxIndex = Math.min(parts.length, 1 + 2 * pairCount);
                            System.out.println("maxIndex : " + maxIndex);
                            for (int i = 1; i < maxIndex; i += 2) {
                                entities.add(parts[i].trim()); // 공백 제거
                                if (i + 1 < parts.length) {
                                    relationships.add(parts[i + 1].trim()); // 공백 제거
                                }
                            }

                            // test
                            System.out.println(entities);
                            System.out.println(relationships);

                            if (entities.size() != pairCount || relationships.size() != pairCount) {
                                Log.e("Data Mismatch", "The number of pairs does not match the pairCount.");
                                Toast.makeText(getContext(), "데이터 불일치. 응답 확인 필요.", Toast.LENGTH_LONG).show();
                            } else {
                                neo4jQuery = Neo4jDatabaseManager.createCypherQuery(entities, relationships, pairCount);
                                System.out.println("TEST_QUERY : " + neo4jQuery);
                            }
                        }
                    }
                    // UI 업데이트는 메인 스레드에서 실행
                    binding.textView2.post(() -> binding.textView2.setText(msg));
                    // 생성된 사이퍼쿼리를 텍스트뷰에 출력해서 확인 가능
                    binding.query.post(() -> binding.query.setText(neo4jQuery));
                } else {
                    Log.e("OpenAI Error", "Error fetching response");
                }
            }

            @Override
            public void onFailure(Call<OpenAIResponse> call, Throwable t) {
                Log.e("OpenAI Failure", t.getMessage());
            }
        });
    }

    // 검색어 자동 완성 기능을 가지는 검색 창
    private void setupAutoCompleteTextView() {
        // 배열 리소스에서 아이템(추천 단어 리스트) 가져오기
        String[] items = getResources().getStringArray(R.array.autocomplete_items);
        // 어댑터에 아이템 설정
        List<String> itemList = new ArrayList<>(Arrays.asList(items));
        CustomArrayAdapter adapter = new CustomArrayAdapter(requireContext(), R.layout.custom_autocomplete_item, itemList);
        // 자동 완성 텍스트 뷰에 어댑터 연결
        binding.searchText.setAdapter(adapter);
        // 자동 완성 시작 글자 수 설정
        binding.searchText.setThreshold(1);
        // 추천 단어가 출력 되는 뷰와 입력 뷰 간의 간격 조정
        binding.searchText.setDropDownVerticalOffset((int) (10 * getResources().getDisplayMetrics().density));

        // TextWatcher를 추가하여 사용자 입력 추적
        binding.searchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 사용자가 직접 입력할 때만 추적
                if (!binding.searchText.isPerformingCompletion()) {
                    userInputText = s.toString();
                    adapter.setUserInputText(userInputText); // adapter에 사용자 입력 전달
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        binding.searchText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 선택된 아이템
                String selectedItem = parent.getItemAtPosition(position).toString();

                // 마지막 공백이 있었던 위치
                int lastSpacePosition = userInputText.lastIndexOf(' ');

                // 마지막 단어를 대체하거나, 공백 뒤에 선택한 아이템 추가
                String newText;
                if (lastSpacePosition != -1) {
                    newText = userInputText.substring(0, lastSpacePosition + 1) + selectedItem + " ";
                } else {
                    newText = selectedItem + " ";
                }

                // AutoCompleteTextView에 새로운 텍스트를 설정하고, 커서 위치를 조정
                binding.searchText.setText(newText);
                binding.searchText.setSelection(newText.length());
            }
        });
    }

    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 3);
        binding.recyclerView.setLayoutManager(layoutManager);
    }

    private void updateRecyclerView(List<Uri> imageUris) {
        ImageAdapter adapter = new ImageAdapter(imageUris, requireContext(), this);
        binding.recyclerView.setAdapter(adapter);
    }

    @SuppressLint("SetTextI18n")
    private void findPicture(String relationship, String entity2) {
        try {
            // Neo4j 서버에서 사진 이름 받아오기
            List<String> photoNamesFromNeo4j = Neo4jDatabaseManager.fetchPhotoNamesFromNeo4j(relationship, entity2);
            // 갤러리에서 사진 이름 가져오기
            // 확장자가 사진마다 달라서 코드 수정 필요
            List<String> allGalleryImageNames = getAllImageNamesWithoutExtension(requireContext());
//            List<String> allGalleryImageNames = getAllImageNames(requireContext());
            // 같은 이름을 가지는 사진 탐색
            List<Uri> matchedUris = findMatchedUris(photoNamesFromNeo4j, allGalleryImageNames, requireContext());
            // 리사이클러뷰 업데이트
            updateUIWithMatchedUris(matchedUris);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 새로운 이미지로 리사이클러뷰 업데이트
    private void updateUIWithMatchedUris(List<Uri> matchedUris) {
        binding.recyclerView.post(() -> imageViewModel.setImageUris(matchedUris));
    }

    // 추후 이미지 버튼 클릭 시 동작하도록 수정 필요
    // 지금은 retrieve 버튼 클릭 시 동작함
    private void retrieve() {
        String relationship = binding.relationship.getText().toString();
        String entity2 = binding.entity2.getText().toString();
        // 데이터 베이스 작업은 별도의 스레드에서 실행
        new Thread(() -> {
            findPicture(relationship, entity2);
        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        Neo4jDriverManager.closeDriver();
    }

    @Override
    public void onImageClick(Uri uri) {
        Intent intent = new Intent(requireContext(), CircleToSearchActivity.class);
        intent.putExtra("imageUri", uri.toString());
        startActivity(intent);
    }
}

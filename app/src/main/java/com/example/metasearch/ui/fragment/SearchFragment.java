package com.example.metasearch.ui.fragment;

import static com.example.metasearch.manager.GalleryImageManager.findMatchedUris;

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

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.metasearch.BuildConfig;
import com.example.metasearch.R;
import com.example.metasearch.databinding.FragmentSearchBinding;
import com.example.metasearch.helper.DatabaseUtils;
import com.example.metasearch.helper.HttpHelper;
import com.example.metasearch.interfaces.Update;
import com.example.metasearch.manager.GalleryImageManager;
import com.example.metasearch.manager.Neo4jDatabaseManager;
import com.example.metasearch.manager.Neo4jDriverManager;
import com.example.metasearch.model.request.NLQueryRequest;
import com.example.metasearch.model.response.PhotoNameResponse;
import com.example.metasearch.service.ApiService;
import com.example.metasearch.model.Choice;
import com.example.metasearch.model.Message;
import com.example.metasearch.model.request.OpenAIRequest;
import com.example.metasearch.model.response.OpenAIResponse;
import com.example.metasearch.ui.activity.CircleToSearchActivity;
import com.example.metasearch.ui.activity.MainActivity;
import com.example.metasearch.ui.adapter.CustomArrayAdapter;
import com.example.metasearch.ui.adapter.ImageAdapter;
import com.example.metasearch.ui.viewmodel.ImageViewModel;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.muddz.styleabletoast.StyleableToast;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment
        implements ImageAdapter.OnImageClickListener, Update {
    private static final String OPENAI_URL = "https://api.openai.com/";
    private static final String WEB_SERVER_URL = "http://113.198.85.4";
    private ImageViewModel imageViewModel;
    private final Neo4jDatabaseManager Neo4jDatabaseManager = new Neo4jDatabaseManager();
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
        setupListeners();
        setupAutoCompleteTextView();

        return root;
    }
    private void setupListeners() {
        binding.searchButton.setOnClickListener(v -> retrieve());
        // 리사이클러뷰 스크롤에 따라 하단의 네비바 높이 조절
        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null) {
                    if (dy > 0) {
                        // 스크롤 내릴 때, 네비게이션 바 숨기기
                        activity.hideBottomNavigationView();
                    } else if (dy < 0) {
                        // 스크롤 올릴 때, 네비게이션 바 보이기
                        activity.showBottomNavigationView();
                    }
                }
            }
        });
    }
    private void sendQueryToServer(String dbName, String query) {
        ApiService service = HttpHelper.getInstance(WEB_SERVER_URL).create(ApiService.class);
        Gson gson = new Gson();

        NLQueryRequest nlQueryRequest = new NLQueryRequest(dbName, query);
        Map<String, String> jsonMap = new HashMap<>();
        jsonMap.put("dbName", nlQueryRequest.getDbName());
        jsonMap.put("query", nlQueryRequest.getQuery());

        String jsonObject = gson.toJson(jsonMap);
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonObject);

        Call<PhotoNameResponse> call = service.sendCypherQuery(requestBody);
        call.enqueue(new Callback<PhotoNameResponse>() {
            @Override
            public void onResponse(@NonNull Call<PhotoNameResponse> call, @NonNull Response<PhotoNameResponse> response) {
                if (response.isSuccessful()) {
                    PhotoNameResponse photoNameResponse = response.body();
                    if (photoNameResponse != null && photoNameResponse.getPhotoName() != null) {
                        Log.d("PhotoNames", photoNameResponse.getPhotoName().toString());

                        List<Uri> matchedUris = findMatchedUris(photoNameResponse.getPhotoName(), requireContext());

                        updateUIWithMatchedUris(matchedUris);

                    } else {
                        Log.e("Response Error", "Received null response body or empty photos list");
                    }
                } else {
                    Log.e("Response Error", "Failed to receive successful response: " + response.message());
                }
            }
            @Override
            public void onFailure(@NonNull Call<PhotoNameResponse> call, @NonNull Throwable t) {
                updateUIWithMatchedUris(new ArrayList<>());
                Log.e("Request Error", "Failed to send request to server", t);
            }
        });
    }
    // OpenAI API 사용해서 사진 검색
    public void photoSearch() {
        // 사용자가 검색한 문장
        String userInput = binding.searchText.getText().toString();
        // 사용자가 아무것도 입력하지 않고 검색 버튼 클릭 시, api 호출하지 않도록 리턴
        if (userInput.length() == 0) {
            // 사용자가 아무것도 입력하지 않았을 때 UI 업데이트와 로직을 종료
            getActivity().runOnUiThread(() -> {
                StyleableToast.makeText(getContext(), "검색어를 입력해주세요.", R.style.customToast).show();
                binding.searchButton.setEnabled(true);
                binding.spinKit.setVisibility(View.GONE);
                updateUIWithMatchedUris(new ArrayList<>());
            });

            return;  // 메서드를 여기서 종료
        }
        // 사용자가 입력한 문장(찾고 싶은 사진) + gpt가 분석할 수 있도록 지시할 문장
        userInput = userInput + getString(R.string.user_input_kor);
        if (userInput.length() == 0) return;
        ApiService service = HttpHelper.getInstance(OPENAI_URL).create(ApiService.class);

        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", userInput));
        OpenAIRequest request = new OpenAIRequest("gpt-3.5-turbo", messages);
        String apiKey = "Bearer " + BuildConfig.OPENAI_API_KEY;

        service.createChatCompletion(apiKey, request).enqueue(new Callback<OpenAIResponse>() {
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
                                StyleableToast.makeText(getContext(), "데이터 불일치. 응답 확인 필요.", R.style.customToast).show();

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

                    // 웹 서버에 쿼리 전송
                    sendQueryToServer(DatabaseUtils.getPersistentDeviceDatabaseName(getContext()), neo4jQuery);
                } else {
                    Log.e("OpenAI Error", "Error fetching response");
                }
                binding.searchButton.setEnabled(true); // 버튼 활성화
                binding.spinKit.setVisibility(View.GONE); // 로딩 아이콘 숨김
            }
            @Override
            public void onFailure(Call<OpenAIResponse> call, Throwable t) {
                Log.e("OpenAI Failure", t.getMessage());
                binding.searchButton.setEnabled(true); // 버튼 활성화
                binding.spinKit.setVisibility(View.GONE); // 로딩 아이콘 숨김
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
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 5);
        binding.recyclerView.setLayoutManager(layoutManager);
    }
    private void updateRecyclerView(List<Uri> imageUris) {
        ImageAdapter adapter = new ImageAdapter(imageUris, requireContext(), this);
        binding.recyclerView.setAdapter(adapter);
    }
    // 새로운 이미지로 리사이클러뷰 업데이트
    private void updateUIWithMatchedUris(List<Uri> matchedUris) {
//        binding.recyclerView.post(() -> imageViewModel.setImageUris(matchedUris));
        if (matchedUris.isEmpty()) {
            Log.d("SearchFragment", "No matched photos found, updating with empty list.");
        }
        getActivity().runOnUiThread(() -> {
            if (binding != null && imageViewModel != null) {
                imageViewModel.setImageUris(matchedUris);
                binding.recyclerView.getAdapter().notifyDataSetChanged();
            }
        });
    }
    private void retrieve() {
        binding.searchButton.setEnabled(false);
        binding.spinKit.setVisibility(View.VISIBLE);
        // 데이터 베이스 작업은 별도의 스레드에서 실행
        new Thread(this::photoSearch).start();
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        Neo4jDriverManager.closeDriver();
    }
    // 자연어 검색 창에서 검색 결과로 출력된 사진 클릭 시, 써클 투 써치로 전환
    @Override
    public void onImageClick(Uri uri) {
        Intent intent = new Intent(requireContext(), CircleToSearchActivity.class);
        intent.putExtra("imageUri", uri.toString());
        startActivity(intent);
    }
    // 화면 초기화
    @Override
    public void performDataUpdate() {
        updateUIWithMatchedUris(new ArrayList<>()); // 검색된 사진 제거
        binding.searchText.setText(""); // 검색한 문장 초기화
    }
}

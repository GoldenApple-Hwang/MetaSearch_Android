package com.example.metasearch.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.metasearch.R;

import java.util.ArrayList;
import java.util.List;

// 검색어 자동 완성 기능에 필요한 데이터 관리 클래스
public class CustomArrayAdapter extends ArrayAdapter<String> implements Filterable {
    private final int resourceLayout;
    private final Context mContext;
    private Filter filter; // 필터링을 위한 필터 객체
    private final List<String> originalList; // 필터링 되기 전의 원본 아이템 리스트(추천 단어 리스트)
    private String userInputText = "";

    public CustomArrayAdapter(@NonNull Context context, int resource, @NonNull List<String> items) {
        super(context, resource, items);
        this.resourceLayout = resource;
        this.mContext = context;
        this.originalList = new ArrayList<>(items);
    }

    // 각 아이템의 뷰를 생성하여 반환
    @SuppressLint("ResourceAsColor")
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // 뷰가 재활용되지 않는 경우, 레이아웃 인플레이터를 사용하여 뷰 생성
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(resourceLayout, parent, false);
        }

        // 현재 포지션의 아이템
        String item = getItem(position);
        if (item != null) {
            // 아이템의 텍스트 설정
            TextView textView = convertView.findViewById(R.id.text);

            // 사용자가 입력한 마지막 단어
            String[] words = userInputText.split("\\s+");
            String lastWord = words[words.length - 1];

            int startPos = item.toLowerCase().indexOf(lastWord.toLowerCase());
            if (startPos != -1) {
                int endPos = startPos + lastWord.length();

                SpannableString spannableString = new SpannableString(item);
                // colors.xml에서 정의된 색상을 사용
                int highlightColor = ContextCompat.getColor(mContext, R.color.white);
                spannableString.setSpan(new ForegroundColorSpan(highlightColor), startPos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                textView.setText(spannableString);
            } else {
                textView.setText(item);
            }
        }
        return convertView;
    }

    public void setUserInputText(String text) {
        userInputText = text;
        // 필터링 강제 실행
        getFilter().filter(text);
    }

    // 필터 객체 반환
    @NonNull
    @Override
    public Filter getFilter() {
        if (filter == null) {
            filter = new Filter() {
                // performFiltering 메서드는 백그라운드 스레드에서 실행되어 필터링 작업을 수행
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();

                    // 입력된 텍스트가 있을 경우 필터링 수행
                    if (constraint != null && constraint.length() > 0) {
                        String searchPattern = constraint.toString().toLowerCase().trim();
                        List<String> filteredList = new ArrayList<>();

                        // 마지막 입력된 단어를 기준으로 필터링
                        String[] words = searchPattern.split("\\s+"); // 공백으로 분리
                        String lastWord = words[words.length - 1]; // 마지막 단어 추출

                        // 마지막 단어로 시작하는 아이템만 필터링하여 리스트에 추가
                        for (String item : originalList) {
                            if (item.toLowerCase().startsWith(lastWord)) {
                                filteredList.add(item);
                            }
                        }

                        results.values = filteredList;
                        results.count = filteredList.size();
                    }
                    return results;
                }

                // publishResults 메서드는 UI 스레드에서 실행되어 필터링 결과 반영
                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    clear(); // 필터링 결과를 반영하기 전에 리스트 클리어
                    if (results != null && results.count > 0) {
                        // 필터링 결과가 있으면 리스트에 추가
                        addAll((List<String>) results.values);
                    }
                    // 데이터가 변경된 것을 알려 리스트 갱신
                    notifyDataSetChanged();
                }
            };
        }
        return filter; // 필터 객체 반환
    }

}
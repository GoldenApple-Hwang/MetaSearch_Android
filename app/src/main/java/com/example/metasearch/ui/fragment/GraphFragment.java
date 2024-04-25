package com.example.metasearch.ui.fragment;

import static com.example.metasearch.manager.GalleryImageManager.findMatchedUri;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.metasearch.databinding.FragmentGraphBinding;
import com.example.metasearch.ui.viewmodel.GraphViewModel;

import java.util.List;

public class GraphFragment extends Fragment {

    private FragmentGraphBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        GraphViewModel graphViewModel =
                new ViewModelProvider(this).get(GraphViewModel.class);

        binding = FragmentGraphBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                // SSL 에러가 발생해도 계속 진행
                handler.proceed();
            }
        });

        binding.webView.getSettings().setJavaScriptEnabled(true); //자바스크립트 실행을 허용. 이거 꼭 해줘야 화면 나타남
        binding.webView.getSettings().setLoadWithOverviewMode(true);  // WebView 화면크기에 맞추도록 설정 - setUseWideViewPort 와 같이 써야함
        binding.webView.getSettings().setUseWideViewPort(true);  // wide viewport 설정 - setLoadWithOverviewMode 와 같이 써야함

        // JavascriptInterface 추가
        binding.webView.addJavascriptInterface(new WebAppInterface(requireContext()), "Android");

        //수정해야됨 제발
        String dbName = "youjeong";
        binding.webView.loadUrl("http://113.198.85.4/graph/" + dbName );

        return root;
    }

    private Handler handler = new Handler(Looper.getMainLooper());
    public class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
        }

        // 자바스크립트에서 호출할 수 있는 메서드
        @JavascriptInterface
        public void receivePhotoName(final String photoName) {
            // 새 스레드에서 작업 실행
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // 백그라운드 스레드에서 갤러리 이미지 이름들을 가져오기
                    final Uri matchedUri = findMatchedUri(photoName, requireContext());

                    // 메인 스레드에서 UI 업데이트
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (matchedUri != null) {
                                binding.photoView.setImageURI(matchedUri);
                            } else {
                                @SuppressLint("ShowToast") Toast toast = Toast.makeText(mContext, "사진을 찾지 못했습니다", Toast.LENGTH_SHORT);
                                toast.show();

                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        toast.cancel();
                                    }
                                }, 500); // 1000ms = 1초 후에 실행
                            }
                        }
                    });
                }
            }).start();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
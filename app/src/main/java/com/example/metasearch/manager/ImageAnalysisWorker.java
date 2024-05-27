package com.example.metasearch.manager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.metasearch.dao.DatabaseHelper;

public class ImageAnalysisWorker extends Worker {
    private ImageServiceRequestManager imageServiceRequestManager;

    public ImageAnalysisWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        DatabaseHelper databaseHelper = DatabaseHelper.getInstance(context);
        this.imageServiceRequestManager = ImageServiceRequestManager.getInstance(context,databaseHelper);
    }

    @NonNull
    @Override
    public Result doWork() {
        // 이미지 분석 시작
        try {
            imageServiceRequestManager.getImagePathsAndUpload();
            return Result.success();
        } catch (Exception e) {
            return Result.failure();
        }
    }
}


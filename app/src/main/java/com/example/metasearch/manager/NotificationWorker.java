package com.example.metasearch.manager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.WorkManager;
import androidx.work.OneTimeWorkRequest;


public class NotificationWorker extends Worker {
    private ImageNotificationManager imageNotificationManager;

    public NotificationWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        this.imageNotificationManager = ImageNotificationManager.getImageNotification(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        // 여기에 알림 전송 로직을 넣으세요.
        // 예를 들어, 위에 정의된 showCompleteNotification 메서드를 호출합니다.

        imageNotificationManager.showCompleteNotification(getApplicationContext());
        // 작업이 성공적으로 완료되었음을 나타냅니다.
        return Result.success();
    }
}


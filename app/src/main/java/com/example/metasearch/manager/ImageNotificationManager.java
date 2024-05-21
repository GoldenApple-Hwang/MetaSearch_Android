package com.example.metasearch.manager;

import static android.content.ContentValues.TAG;
import static android.content.Context.NOTIFICATION_SERVICE;

import static java.lang.Thread.sleep;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.metasearch.R;
import com.example.metasearch.ui.activity.MainActivity;
import com.example.metasearch.ui.fragment.HomeFragment;

public class ImageNotificationManager {
    private NotificationManager mNotificationManager; //알림 매니저
    private NotificationCompat.Builder notifyBuilder; //알림 빌더
    private static final String CHNANNEL_ID ="channelID";//channel을 구분하기 위한 ID
    private static final String COMPLETE_CHNANNEL_ID = "completeChannelID"; //channel을 구분하기 위한 ID
    private static final String COMPLETE_CHANNEL_ID = "completeChannelID"; //마지막 알림 channelID
    private static final int NOTIFICATION_ID = 0; //Notification에 대한 ID 생성
    private NotificationChannel notificationChannel;
    private int progressMax = 100;
    private int progressCurrent = 0;
    private boolean is_finish_analyze = false;
    private boolean isCanceled = false; //작업을 중단하기 위한 플래그
    private static ImageNotificationManager instance;
    private Handler handler; //ui 관리 handler
    private Runnable notificationRunnable; // 알림 진행 중 runnable
    private boolean isCompleteNotification = false;
    private ImageNotificationManager(Context context){
        mNotificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        //업데이트 알람 생성
        makeUpdateNotificationChannel(context);

        //완료 업데이트 알람 생성
        makeCompleteNotificationChannel(context);

    }

    // 업데이트 알림 채널 생성
    public void makeUpdateNotificationChannel(Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Channel 정의 생성자
            notificationChannel = new NotificationChannel(CHNANNEL_ID, "Update Image Analyze Notification", NotificationManager.IMPORTANCE_LOW);
            //channel에 대한 기본 설정
            notificationChannel.setDescription("update image analyze notification");

            //Manager을 이용하여 Channel 생성
            mNotificationManager.createNotificationChannel(notificationChannel);

        }
    }

    // 완료 알림 채널 생성
    public void makeCompleteNotificationChannel(Context context){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // int importance = NotificationManager.IMPORTANCE_HIGH; // 중요도 설정
            mNotificationManager.cancel(NOTIFICATION_ID);
            mNotificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            notificationChannel = new NotificationChannel(COMPLETE_CHNANNEL_ID,"Complete Image Analyze Notification",mNotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription("complete image analyze notification");
            notificationChannel.enableVibration(true);
            notificationChannel.enableLights(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            // 채널에 대한 추가 설정 가능 (예: 진동 패턴 설정)

            // NotificationManager를 통해 채널 생성
            mNotificationManager.createNotificationChannel(notificationChannel);

//            notifyBuilder = new NotificationCompat.Builder(context, CHNANNEL_ID)
//                    .setContentTitle("이미지 분석 중...") //알림 제목 설정
//                    .setContentText("이미지 분석이 시작됩니다.") //알림 내용 설정
//                    .setSmallIcon(R.drawable.baseline_image_search_24) //알림 아이콘 설정
//                    .setProgress(progressMax, progressCurrent, false);
        }
    }


    public static ImageNotificationManager getImageNotification(Context context){
        if(instance == null){
            instance = new ImageNotificationManager(context);
        }
        return instance;
    }

    //Builder를 설정하는 함수
    public NotificationCompat.Builder settingBuilder(Context context,String channelID){
        if(channelID.equals(CHNANNEL_ID)){
            //새로운 알림 생성
            notifyBuilder = new NotificationCompat.Builder(context, CHNANNEL_ID)
                    .setContentTitle("이미지 분석 중...") //알림 제목 설정
                    .setContentText("이미지 분석이 시작됩니다.") //알림 내용 설정
                    .setSmallIcon(R.drawable.baseline_image_search_24) //알림 아이콘 설정
                    .setProgress(progressMax, progressCurrent, false);
        }
        else if(channelID.equals(COMPLETE_CHANNEL_ID)){
            //새로운 알림 생성
            notifyBuilder = new NotificationCompat.Builder(context, COMPLETE_CHNANNEL_ID)
                    .setContentTitle("이미지 분석 완료") // 새로운 제목 설정
                    .setContentText("검색을 시작해보세요.") // 새로운 내용 설정
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setSmallIcon(R.drawable.ic_launcher_foreground); // 새로운 아이콘 설정
        }
        return notifyBuilder;
    }
//    public void show_notificaiton(Context context,int imageListSize){
//        if (context instanceof Activity) {
//            ((Activity) context).runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    showUpdateNotificationProgress(context,imageListSize);
//
//                }
//            });
//        }
//    }

    // 진행 중인 알림창 띄우기
    public void showUpdateNotificationProgress(Context context, int imageListSize){
        final int totalImages = imageListSize;
        final int updateInterval = 30000; // 30초 동안 이미지 처리
        notifyBuilder = settingBuilder(context,CHNANNEL_ID);

        //handler를 사용하여 비동기적으로 알림 업데이트
        handler = new Handler(Looper.getMainLooper());

        notificationRunnable = new Runnable() {
            int currentProgress = 0;

            @Override
            public void run() {
                if (isCanceled) {
                    //작업이 중간에 강제 취소된 경우
                    //100% 강제 완료 or 제대로 다 끝낸 후 종료
                    Log.d(TAG,"isCanceled는 true로 변경");
                    notifyBuilder.setProgress(progressMax, progressMax, false);
                    notifyBuilder.setContentTitle("이미지 분석 100% 완료");
                    notifyBuilder.setContentText("잠시만 기다려주세요...");
                    mNotificationManager.notify(NOTIFICATION_ID, notifyBuilder.build());

                    if (!is_finish_analyze) //false 이면
                        is_finish_analyze = true;//프로그래스 바 마지막까지 완료
                    isCanceled = false; //다시 원상복구
                    return;
                }
                if (currentProgress <= 100) {
                    //알림 업데이트
                    notifyBuilder.setProgress(progressMax, currentProgress, false);
                    notifyBuilder.setContentTitle("이미지 분석 " + currentProgress + "% 완료");
                    Log.d(TAG, "현재 완료 숫자 :" + currentProgress);
                    mNotificationManager.notify(NOTIFICATION_ID, notifyBuilder.build());

                    //다음 업데이트를 위한 프로그래스 증가
                    currentProgress += (100 / totalImages);

                    //다음 업데이트 스케줄 //30초 이후
                    handler.postDelayed(this, updateInterval);

                    //마지막 업데이트에서 100% 설정
                    if (currentProgress >= 100) {
                        notifyBuilder.setProgress(progressMax, progressMax, false);
                        notifyBuilder.setContentTitle("이미지 분석 100% 완료");
                        notifyBuilder.setContentText("잠시만 기다려주세요...");

                        is_finish_analyze = true; //프로그래스바 마지막까지 완료
                    }
                }
            }
        };
        //첫 업데이트 시작
        handler.post(notificationRunnable);
    }


    // 종료된 알림창 띄우기
    public void showCompleteNotification(Context context){
        // 전의 채널 알림 종료
        mNotificationManager.cancel(NOTIFICATION_ID);

        // 새로운 알림 띄우기
        notifyBuilder = settingBuilder(context,COMPLETE_CHANNEL_ID);

//        //알림을 클릭하면 MainActivity로 이동하게 된다.
//        // MainActivity로 이동하는 Intent 생성
//        Intent intent = new Intent(context, MainActivity.class);
//        // 사용자가 알림을 클릭했을 때 기존에 있던 액티비티 스택 위에 새 액티비티를 띄우도록 설정
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        // PendingIntent 생성
//        // 안드로이드 12 (API 수준 31) 이상에서는 FLAG_IMMUTABLE 또는 FLAG_MUTABLE을 명시적으로 지정해야 합니다.
//        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            flags |= PendingIntent.FLAG_IMMUTABLE; // FLAG_MUTABLE을 사용할 필요가 있는 경우에는 이를 FLAG_IMMUTABLE 대신 사용하세요.
//        }
//        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, flags);
//
//        // 알림 빌더에 PendingIntent 설정
//        notifyBuilder.setContentIntent(pendingIntent);

        // 알림 발송
        mNotificationManager.notify(NOTIFICATION_ID, notifyBuilder.build());
        isCompleteNotification = true;
//        OneTimeWorkRequest notificationWork = new OneTimeWorkRequest.Builder(NotificationWorker.class)
//                .build();
//        WorkManager.getInstance(context).enqueue(notificationWork);

    }

    //현재 화면에 완료 알람이 띄워져있는지 확인하는 함수
    public boolean getIsCompleteNotification(){
        return this.isCompleteNotification;
    }

    //진행 중이던 프로그래스 바 강제 100%로 종료시키는 함수
    public void cancelProceedProgressbar() throws InterruptedException {
        //플래그를 설정하여 runnable이 다음 살행을 중지하도록 함
        isCanceled = true;

        sleep(2000); //2초 후에 해당 runnable 삭제
        handler.removeCallbacks(notificationRunnable); //현재 예약된 runnable을 제거함
        Log.d(TAG,"cancelProceedProgressbar 함수 실행");
    }

}

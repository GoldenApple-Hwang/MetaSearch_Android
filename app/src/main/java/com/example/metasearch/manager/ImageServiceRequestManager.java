package com.example.metasearch.manager;

import static java.lang.Thread.sleep;

import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.metasearch.dao.AnalyzedImageListDatabaseHelper;
import com.example.metasearch.dao.DatabaseHelper;
import com.example.metasearch.helper.DatabaseUtils;
import com.example.metasearch.interfaces.ImageAnalysisCompleteListener;
import com.example.metasearch.service.ApiService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Handler;

public class ImageServiceRequestManager {
    private ImageAnalysisCompleteListener listener;
    private static final String TAG = "ImageUploader"; //로그에 표시될 태그
    private Context context; //Context 객체
    private DatabaseHelper databaseHelper; //sqlite 데이터베이스 접근 객체
    private AnalyzedImageListDatabaseHelper analyzedImageListDatabaseHelper;
    private ImageAnalyzeListManager imageAnalyzeListController; //이미지 분석된 리스트 객체
    private ApiService aiService;
    private AIRequestManager aiRequestManager;
    private WebRequestManager webRequestManager;
    private ApiService webService;
    private ImageDialogManager imageDialogManager; // 다이얼로그 매니저
    private ImageNotificationManager imageNotificationManager; // 알림 매니저
    private static ImageServiceRequestManager instance;
    private Dialog image_dialog; //이미지 분석/삭제 관련 다이얼로그
    private NotificationManager mNotificationManager; // 알림 매니저
    private NotificationCompat.Builder notifyBuilder; // 알림 빌더
    private Notification notificationChannel; //알림 채널
    private int progressMax = 100;
    private int progressCurrent = 0;
    private boolean is_finish_analyze = false; //현재 모든 분석이 100% 되었는지 여부
    private static final String CHANNEL_ID = "channelID"; //channel을 구분하기 위한 ID
    private static final String COMPLETE_CHANNEL_ID = "completeChannelID"; //channel을 구분하기 위한 ID(완료)
    private static final int NOTIFICATION_ID = 0; //Notificaiton에 대한 ID 생성
    private String DBName; //클라이언트가 사용하는 neo4j dbName


    private ImageServiceRequestManager(Context context, DatabaseHelper databaseHelper) {
        this.context = context;
        // 인물 데이터베이스 설정
        this.databaseHelper = databaseHelper;
        // 분석된 이미지 데이터베이스 설정
//        this.imageAnalyzeListController = imageAnalyzeList;
        this.imageAnalyzeListController = ImageAnalyzeListManager.getInstance(context);
        this.imageDialogManager = ImageDialogManager.getImageDialogManager(context); //이미지 다이얼로그 객체 생성
        this.imageNotificationManager = ImageNotificationManager.getImageNotification(context); //이미지 알림 객체 생성
        

    } //생성자

    public static ImageServiceRequestManager getInstance(Context context, DatabaseHelper databaseHelper){
        if (instance == null){
            instance = new ImageServiceRequestManager(context,databaseHelper);
        }
        return instance;
    }
    public void setImageAnalysisCompleteListener(ImageAnalysisCompleteListener listener) {
        this.listener = listener;
    }
    public void getImagePathsAndUpload() throws IOException, ExecutionException, InterruptedException { // 갤러리 이미지 경로 / 데이터베이스의 모든 얼굴 byte 가져옴
        Log.d(TAG,"getImagePathsAndUpload 함수 들어옴");
        //aiRetrofit = AIHttpService.getInstance(AIserver_BASE_URL).getRetrofit();
//         webRetrofit = WebHttpService.getInstance(Webserver_BASE_URL).getRetrofit();
//         //aiService = aiRetrofit.create(ImageUploadService.class);
//         webService = webRetrofit.create(ImageUploadService.class);

        aiRequestManager = AIRequestManager.getAiImageUploader(context);
        aiService = aiRequestManager.getAiService();
        webRequestManager = WebRequestManager.getWebImageUploader();
        webService = webRequestManager.getWebService();
        ArrayList<String> imagePaths = GalleryImageManager.getAllGalleryImagesUriToString(context);
        Map<String,byte[]> dbImages = databaseHelper.getAllImages(); //데이터베이스에서 이미지를 byte로 로드

        Log.d(TAG,"imagePaths의 사이즈 : "+imagePaths.size());
        Log.d(TAG,"데이터베이스 이미지 수: " + (dbImages != null ? dbImages.size() : "null"));


        if(!imagePaths.isEmpty()){
            Log.d(TAG,"imagesPath는 안 비어있음");
        }
        //갤러리 이미지 경로 리스트 또는 데이터베이스 바이트 리스트가 null이어도 일단은 전달
        DBName = DatabaseUtils.getPersistentDeviceDatabaseName(context);
        uploadImage(imagePaths, dbImages, DBName); //이미지를 각각의 파일로 업로드 하는 함수 호출
    }
//    public void show_image_dialog_notificaiton(Context context,boolean is_add){
//        if (context instanceof Activity) {
//            ((Activity) context).runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    image_dialog = imageDialogManager.getImage_dialog(is_add);
//                    image_dialog.show();
//
//                }
//            });
//        }
//    }
    //이미지를 업로드하는 함수
    public void uploadImage(ArrayList<String> imagesPaths, Map<String,byte[]> dbBytes,String DBName) throws IOException, ExecutionException, InterruptedException {
        Log.d(TAG,"UplaodImage 함수에 들어옴");

        //일단 추가하거나 삭제할 이미지가 있는지 체크한다.
        ArrayList<String> deletePaths = checkDeleteImagePath(imagesPaths);
        ArrayList<String> addImagePaths = checkAddImagePath(imagesPaths);

        Log.d(TAG,"uploadImage deletePaths 사이즈 : "+deletePaths.size());
        Log.d(TAG,"uploadImage addPaths 사이즈 : "+addImagePaths.size());

        if (!addImagePaths.isEmpty()){ //추가 분석할 것이 있다면
            Log.d(TAG,"!addImagePaths.isEmpty()임");
            int size = 0;
            // 다이얼로그 띄움
            imageDialogManager.show_image_dialog_notificaiton(context,true);
            if (!deletePaths.isEmpty()){ // 만약 삭제 이미지가 있다면
                // 삭제 이미지 사이즈를 size에 합침
                size+=deletePaths.size();
            }
            // 알림 띄움
            imageNotificationManager.showUpdateNotificationProgress(context, size+=addImagePaths.size()); //분석할 이미지의 개수를 size에 합침
            //imageNotificationManager.show_notificaiton(context,size+=addImagePaths.size());

        }
        else if(!deletePaths.isEmpty()){ //삭제할 것이 있다면(추가 분석할 것은 없음)
            Log.d(TAG,"!deletePaths.isEmpty()임");

            // 다이얼로그 띄움
            imageDialogManager.show_image_dialog_notificaiton(context,false);
            // 삭제만 있으면 알림 안 띄움 (이미지 분석을 기준으로 알림을 띄우는 것)
        }
        else if(addImagePaths.isEmpty() && deletePaths.isEmpty()){ // 추가/삭제 할 것이 없음
            Log.d(TAG,"추가 삭제할 것이 없음");
            imageDialogManager.show_no_image_dialog_notification(context);
            // 분석이 없으면 알림할 필요없음
        }

        //만약 삭제할 이미지나 추가 이미지가 있으면
        if (!deletePaths.isEmpty() || !addImagePaths.isEmpty()) {
            Log.d(TAG,"삭제할 이미지나 추가할 이미지가 있음");
            //ArrayList<String> safeDeletePaths = deletePaths != null ? deletePaths : new ArrayList<>();
            //ArrayList<String> safeAddImagePaths = addImagePaths != null ? addImagePaths : new ArrayList<>();
            if(dbBytes!=null){

                //이미지 분석이 시작된다는 첫 번째 요청 실행 ( db의 faces 삭제)
                aiRequestManager.firstUploadImage(DBName).thenRun(()->{
                    //db의 faces에 얼굴 업로드
                    aiRequestManager.uploadDBImage(dbBytes, DBName).thenRun(() -> { //콜백 설정함 //db 요청 끝나고 사진 분석 요청 보냄
                        try {
                            //추가나 삭제 이미지를 서버에 전송
                            request_image_AIServer(addImagePaths,deletePaths,DBName);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                });
            }
            else{ //데이터베이스에 이미지가 없는 경우,
                request_image_AIServer(addImagePaths,deletePaths,DBName);
            }
        }
    }
    //이미지 분석 리스트를 통해 해당 이미지가 추가될 이미지인지, 삭제될 이미지인지 확인하고 그에 맞게 서버에 요청하는 함수
    public ArrayList<String> checkAddImagePath(ArrayList<String> imagesPaths){
//        //추가할 것이 있는지 찾을 것
        ArrayList<String> addImagePaths = new ArrayList<>();
        addImagePaths = imageAnalyzeListController.checkAddImagePath(imagesPaths);
        return addImagePaths;

    }
    public ArrayList<String> checkDeleteImagePath(ArrayList<String> imagesPaths){
        Log.d(TAG,"checkDeleteImagePath 함수에 들어옴");
        ArrayList<String> deleteImagePaths = new ArrayList<>();
        deleteImagePaths = imageAnalyzeListController.checkDeleteImagePath(imagesPaths);
        return deleteImagePaths;
    }


    public void request_image_AIServer(ArrayList<String>addImagePaths,ArrayList<String>deleteImagePaths,String DBName) throws IOException {
        Log.d(TAG,"request_image_AIServer 함수에 들어옴");
        Log.d(TAG,"추가 분석할 리스트 사이즈 : "+addImagePaths.size());
        Log.d(TAG,"삭제할 리스트 사이즈 : "+deleteImagePaths.size());
        boolean isAddExit = !addImagePaths.isEmpty();; //추가 요청이 있는지 확인
        boolean isDeleteExit = !deleteImagePaths.isEmpty(); //삭제 요청이 있는지 확인


        if(isDeleteExit && isAddExit){ // 삭제 요청 o, 추가 요청 o
            Log.d(TAG,"삭제 요청과 추가 요청이 있었음");

            //web 서버에 삭제 관련 이미지 이름 전송
            webRequestManager.uploadDeleteGalleryImage(deleteImagePaths,DBName);

            //AI 서버에 삭제 관련 이미지 이름 전송
            aiRequestManager.uploadDeleteGalleryImage(databaseHelper,deleteImagePaths,DBName).thenRun(() -> { //콜백 설정함
                Log.d(TAG,"삭제 끝 추가 분석 시작하려고 함");
                Log.d(TAG,"추가 분석 이미지 개수 : "+addImagePaths.size());
                //웹 서버에 추가 분석 이미지 전송
                webRequestManager.uploadAddGalleryImage(addImagePaths, DBName);
                //AI 서버에 추가 분석 이미지 전송
                aiRequestManager.uploadAddGalleryImage(addImagePaths,DBName).thenRun(() -> {
                    //콜백 설정
                    //추가 이미지 경로 리스트, 삭제 이미지 경로 리스트 초기화
                    imageAnalyzeListController.clearAddDeleteImageList();

                    //AI 서버에 모든 요청이 보내졌다는 마무리 요청
                    aiRequestManager.completeUploadImage(databaseHelper,DBName).thenRun(()->{
                        //이미지 이름과 input 이름이 다른 것을 확인하여 neo4j서버에 csv 이름 변경을 요청함
                        requestChangeName();
                        informCompleteImageAnalyze();

                    });
                });
            });
        }
        else if(!isAddExit && isDeleteExit){ //삭제 요청 o, 추가 요청 x
            //웹 서버에 삭제 관련 이미지 전송
            //webRequestManager.uploadDeleteGalleryImage(webService,deleteImagePaths,DBName);
//웹 서버에 삭제 관련 이미지 전송
            webRequestManager.uploadDeleteGalleryImage(deleteImagePaths,DBName);
            //AI 서버에 삭제 관련 이미지 이름 전송
            aiRequestManager.uploadDeleteGalleryImage(databaseHelper,deleteImagePaths,DBName).thenRun(() -> { //콜백 설정함
                //추가 이미지 경로 리스트, 삭제 이미지 경로 리스트 초기화
                imageAnalyzeListController.clearAddDeleteImageList();

                //AI 서버에 모든 요청이 마무리 되었다는 요청
                aiRequestManager.completeUploadImage(databaseHelper,DBName).thenRun(()->{
                    Log.d(TAG,"모든 이미지 전송 완료");
                });
            });
        }
        else if(isAddExit) { //삭제 요청 x, 추가 요청 o
            Log.d(TAG, "추가 작업만 진행");

            //웹 서버에 추가 분석 이미지 전송
            webRequestManager.uploadAddGalleryImage(addImagePaths, DBName);

            //AI 서버에 추가 분석 이미지 전송
            aiRequestManager.uploadAddGalleryImage(addImagePaths,DBName).thenRun(() -> {
                //콜백 설정
                //추가 이미지 경로 리스트, 삭제 이미지 경로 리스트 초기화
                imageAnalyzeListController.clearAddDeleteImageList();
                //AI 서버에 모든 요청이 보내졌다는 마무리 요청
                aiRequestManager.completeUploadImage(databaseHelper,DBName).thenRun(()->{
                    //이미지 이름과 input 이름이 다른 것을 확인하여 neo4j서버에 csv 이름 변경을 요청함
                    requestChangeName();
                    informCompleteImageAnalyze();


                });
            });
        }
    }
    // 이미지 분석이 완료된 후 호출
    public void completeAnalysis() {
        if (listener != null) {
            listener.onImageAnalysisComplete();
        }
    }
    // 이미지 이름과 input 이름이 다른 것들이 들어있는 해쉬맵 함수를 순회하며 neo4j서버에서 csv를 변경하도록 요청함
    public void requestChangeName() {
        Map<String,String> misMatchMap = databaseHelper.getMismatchedImageInputNames();
        if (!misMatchMap.isEmpty()){
            for (Map.Entry<String,String> misMatchMapEntity : misMatchMap.entrySet()){
                //key = oldName, value = newName
                //neo4j 서버에게 oldName과 newName을 보낸다.
                webRequestManager.changePersonName(DBName,misMatchMapEntity.getKey(),misMatchMapEntity.getValue());
            }
        }
    }

    //이미지 분석이 끝났다는 것을 알리는 함수
    public void informCompleteImageAnalyze(){
        try {
            // 해당 프로그래스 바 강제 100% 종료, runnable에서 삭제
            imageNotificationManager.cancelProceedProgressbar();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // imageNotificationManager.showCompleteNotification(context);
        // 완료 알림을 화면에 띄움

        //해당 work가 imageNotificationManager.showCompleteNotification(context)를 호출하도록 되어있음
        OneTimeWorkRequest notificationWork = new OneTimeWorkRequest.Builder(NotificationWorker.class)
                .build();
        WorkManager.getInstance(context).enqueue(notificationWork);



    }


}

package com.example.metasearch.manager;

import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.metasearch.dao.DatabaseHelper;
import com.example.metasearch.helper.DatabaseUtils;
import com.example.metasearch.service.ApiService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Handler;

public class ImageServiceRequestManager {
    private static final String TAG = "ImageUploader"; //로그에 표시될 태그
    private Context context; //Context 객체
    private DatabaseHelper databaseHelper; //sqlite 데이터베이스 접근 객체
    private ImageAnalyzeListManager imageAnalyzeListController; //이미지 분석된 리스트 객체
    private ApiService aiService;
    private AIRequestManager aiRequestManager;
    private WebRequestManager webRequestManager;
    private ApiService webService;
    private ImageDialogManager imageDialogManager;
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
    private Runnable notificationRunnable;
    private Handler handler;
    private boolean isCancelled = false; //작업을 중단하기 위한 플래그

    private ImageServiceRequestManager(Context context, DatabaseHelper databaseHelper) {
        this.context = context;
        this.databaseHelper = databaseHelper;
//        this.imageAnalyzeListController = imageAnalyzeList;
        this.imageAnalyzeListController = ImageAnalyzeListManager.getInstance(context);
        this.imageDialogManager = ImageDialogManager.getImageDialogManager(context); //이미지 다이얼로그 객체 생성


    } //생성자

    public static ImageServiceRequestManager getInstance(Context context,DatabaseHelper databaseHelper){
        if (instance == null){
            instance = new ImageServiceRequestManager(context,databaseHelper);
        }
        return instance;
    }


    public void getImagePathsAndUpload() throws IOException, ExecutionException, InterruptedException { // 갤러리 이미지 경로 / 데이터베이스의 모든 얼굴 byte 가져옴
        Log.d(TAG,"getImagePathsAndUpload 함수 들어옴");
        //aiRetrofit = AIHttpService.getInstance(AIserver_BASE_URL).getRetrofit();
//         webRetrofit = WebHttpService.getInstance(Webserver_BASE_URL).getRetrofit();
//         //aiService = aiRetrofit.create(ImageUploadService.class);
//         webService = webRetrofit.create(ImageUploadService.class);

        aiRequestManager = AIRequestManager.getAiImageUploader();
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
        uploadImage(imagePaths, dbImages, DatabaseUtils.getPersistentDeviceDatabaseName(context)); //이미지를 각각의 파일로 업로드 하는 함수 호출
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

        if (!addImagePaths.isEmpty()){
            imageDialogManager.show_image_dialog_notificaiton(context,true);
        }
        else if(!deletePaths.isEmpty()){ //delete가 있는 것
            imageDialogManager.show_image_dialog_notificaiton(context,false);
        }
        else if(addImagePaths.isEmpty() && deletePaths.isEmpty()){
            imageDialogManager.show_no_image_dialog_notification(context);
        }

        //만약 삭제할 이미지나 추가 이미지가 있으면
        if (!deletePaths.isEmpty() || !addImagePaths.isEmpty()) {
            //ArrayList<String> safeDeletePaths = deletePaths != null ? deletePaths : new ArrayList<>();
            //ArrayList<String> safeAddImagePaths = addImagePaths != null ? addImagePaths : new ArrayList<>();
            if(dbBytes!=null){
                aiRequestManager.fristUploadImage(DBName).thenRun(()->{
                    Log.d(TAG, "Before calling uploadDBImage");
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
        ArrayList<String> addImagePaths = null;
        return addImagePaths = imageAnalyzeListController.checkAddImagePath(imagesPaths);

    }
    public ArrayList<String> checkDeleteImagePath(ArrayList<String> imagesPaths){
        ArrayList<String> deleteImagePaths = null;

        return deleteImagePaths = imageAnalyzeListController.checkDeleteImagePath(imagesPaths);
    }


    public void request_image_AIServer(ArrayList<String>addImagePaths,ArrayList<String>deleteImagePaths,String DBName) throws IOException {

        boolean isAddExit = !addImagePaths.isEmpty();; //추가 요청이 있는지 확인
        boolean isDeleteExit = !deleteImagePaths.isEmpty(); //삭제 요청이 있는지 확인


        if(isDeleteExit && isAddExit){ // 삭제 요청 o, 추가 요청 o
            Log.d(TAG,"삭제 요청과 추가 요청이 있었음");

            //AI 서버에 삭제 관련 이미지 이름 전송

            aiRequestManager.uploadDeleteGalleryImage(databaseHelper,deleteImagePaths,DBName).thenRun(() -> { //콜백 설정함

                //웹 서버에 추가 분석 이미지 전송
                webRequestManager.uploadAddGalleryImage(webService, addImagePaths, DBName);
                try {
                    //AI 서버에 추가 분석 이미지 전송
                    aiRequestManager.uploadAddGalleryImage(addImagePaths,DBName).thenRun(() -> {
                        //콜백 설정
                        //모든 요청이 끝났다는 마지막 요청
                        aiRequestManager.completeUploadImage(databaseHelper,DBName);
                    });
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        else if(!isAddExit && isDeleteExit){ //삭제 요청 o, 추가 요청 x
            //웹 서버에 삭제 관련 이미지 전송
            //webRequestManager.uploadDeleteGalleryImage(webService,deleteImagePaths,DBName);

            //AI 서버에 삭제 관련 이미지 이름 전송
            aiRequestManager.uploadDeleteGalleryImage(databaseHelper,deleteImagePaths,DBName).thenRun(() -> { //콜백 설정함

                //AI 서버에 모든 요청이 마무리 되었다는 요청
                aiRequestManager.completeUploadImage(databaseHelper,DBName);
                Log.d(TAG,"모든 이미지 전송 완료");
            });
        }
        else if(isAddExit) { //삭제 요청 x, 추가 요청 o
            Log.d(TAG, "추가 작업만 진행");

            //웹 서버에 추가 분석 이미지 전송
            webRequestManager.uploadAddGalleryImage(webService, addImagePaths, DBName);

            //AI 서버에 추가 분석 이미지 전송
            aiRequestManager.uploadAddGalleryImage(addImagePaths,DBName).thenRun(() -> {
                //콜백 설정

                //AI 서버에 모든 요청이 보내졌다는 마무리 요청
                aiRequestManager.completeUploadImage(databaseHelper,DBName);
            });
        }

        //추가 이미지 경로 리스트, 삭제 이미지 경로 리스트 초기화
        imageAnalyzeListController.clearAddDeleteImageList();
    }





    // 사용 예
    //uploadAndProcessNext(addImagePath, 0, /* 기타 필요한 매개변수 */);



    //삭제될 이미지를 서버에 전송하는 코드
//    public void uploadDeleteGalleryImage(ImageUploadService service,String imageName, String source, boolean isFinal){
//
//        RequestBody requestBody;
//        MultipartBody.Part imagePart;
//
//        Log.d(TAG,"deleteImage file 이름 : "+ imageName);
//        requestBody = RequestBody.create(MediaType.parse("filename"),imageName);
//        //파일의 경로를 해당 이미지의 이름으로 설정함
//        imagePart = MultipartBody.Part.createFormData("deleteImage",imageName,requestBody);
//
//        //이미지 출처 정보를 전송할 RequestBody 생성
//        RequestBody sourceBody = RequestBody.create(MediaType.parse("text/plain"),source); //DB이름과 어디에 저장되어야하는지에 관한 정보를 전달
//        RequestBody endIndicatorBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(isFinal)); //갤러리의 마지막 요청인지에 대한 정보 전달
//
//        //API 호출
//        Call<UploadResponse> call = service.UploadDeleteImage(imagePart,sourceBody,endIndicatorBody); //이미지 업로드 API 호출
//        call.enqueue(new Callback<UploadResponse>() { //비동기
//            @Override
//            public void onResponse(Call<UploadResponse> call, Response<UploadResponse> response) {
//                if (response.isSuccessful()) {
//                    //Log.e(TAG, "Image upload 성공: " + response.message());
//                    // 서버로부터 받은 응답 처리
//                    if(response.body() != null){
//                        for (UploadResponse.ImageData imageData : response.body().getImages()) {
//                            boolean isExit = imageData.getIsExit();
//                            if(isExit){
//                                Log.d(TAG,"삭제 이미지에 대한 응답 받음 / 삭제할 얼굴 있음");
//                                String imageName = imageData.getImageName();
////                                String imageBytes = imageData.getImageBytes();
////                                // 여기서 바이트 배열로 변환 후 이미지 처리를 할 수 있음
////                                byte[] decodedString = Base64.decode(imageBytes, Base64.DEFAULT);
//                                Log.d(TAG,"imageName : "+imageName);
//                                databaseHelper.deleteImage(imageName); //이미지 이름을 통해 해당 데이터 삭제함
//                            }
//                            else{
//                                Log.e(TAG, "삭제 이미지에 대한 응답 받음 / 삭제할 얼굴 없음" + response.message());
//                            }
//                        }
//                    }
//                }
//            }
//            @Override
//            public void onFailure(Call<UploadResponse> call, Throwable t) {
//                Log.e(TAG, "삭제 이미지 업로드 실패함" + t.getMessage());
//            }
//        });
//    }

    //추가될 이미지를 서버에 전송하는 코드
//    public void uploadAddGalleryImage(ImageUploadService service, File imageFile, String source, boolean isFinal) throws IOException {
//        Log.d(TAG,"uploadAddGalleryImage 안에 들어옴");
//        //List<CompletableFuture<Void>> futuresList = new ArrayList<>();
//        Log.d(TAG,"addImge 이름 : " + imageFile.getName());
//        RequestBody requestBody;
//        MultipartBody.Part imagePart;
//        //추가 이미지 분석이 필요한 이미지를 전달
//        requestBody = RequestBody.create(MediaType.parse("image"),imageFile);
//        //파일의 경로를 해당 이미지의 이름으로 설정함
//        imagePart = MultipartBody.Part.createFormData("addImage",imageFile.getName(),requestBody);
//
//        //이미지 출처 정보를 전송할 RequestBody 생성
//        RequestBody sourceBody = RequestBody.create(MediaType.parse("text/plain"),source); //DB이름과 어디에 저장되어야하는지에 관한 정보를 전달
//        RequestBody endIndicatorBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(isFinal)); //갤러리의 마지막 요청인지에 대한 정보 전달
//
//        //API 호출
//        Call<UploadResponse> call = service.uploadAddImage(imagePart,sourceBody,endIndicatorBody); //이미지 업로드 API 호출
//        call.enqueue(new Callback<UploadResponse>() { //비동기
//            @Override
//            public void onResponse(Call<UploadResponse> call, Response<UploadResponse> response) {
//                if (response.isSuccessful()) {
//                    Log.e(TAG, "Image upload 성공: " + response.message());
//                    // 서버로부터 받은 응답 처리
//                    if(response.body() != null) {
//                        for (UploadResponse.ImageData imageData : response.body().getImages()) {
//                            boolean isFaceExit = imageData.getIsExit(); //추출된 이미지가 있다는 것
//                            if (isFaceExit) {
//                                //Log.d(TAG,"추출한 얼굴 이미지 있음");
//                                Log.d(TAG, "추가 이미지에 대한 응답 받음 / 추가할 얼굴 있음");
//                                String imageName = imageData.getImageName();
//                                String imageBytes = imageData.getImageBytes();
//                                // 여기서 바이트 배열로 변환 후 이미지 처리를 할 수 있음
//                                byte[] decodedString = Base64.decode(imageBytes, Base64.DEFAULT);
//                                Log.d(TAG, "imageName : " + imageName);
//                                databaseHelper.insertImage(imageName, decodedString);
//                            } else {
//                                Log.d(TAG, "추가 이미지에 대한 응답 받음 / 추가할 얼굴 없음");
//                                //Log.e(TAG, "이미지 업로드 성공, 추출된 이미지 없음" + response.message());
//                            }
//                        }
//                    }
//                }
//            }
//            @Override
//            public void onFailure(Call<UploadResponse> call, Throwable t) {
//                Log.e(TAG, "추가 이미지 업로드 실패함" + t.getMessage());
//            }
//        });
//    }

    //DB에 있는 모든 이미지들을 서버에 전송하는 코드
//    public CompletableFuture<Void> uploadDBImage(ImageUploadService service, Map<String, byte[]> imagesList, String source){
//        //데이터베이스 서버 요청이 다 끝나면, 다 끝났다는 것을 반환함
//        Log.d(TAG,"uploadDBImage 들어옴");
//        // 모든 비동기 작업을 추적하기 위한 CompletableFuture 리스트 생성
//        List<CompletableFuture<Void>> futuresList = new ArrayList<>();
//        for (Map.Entry<String,byte[]> database_image_element : imagesList.entrySet()){
//            CompletableFuture<Void> future = new CompletableFuture<>();
//            futuresList.add(future);
//            //byte[]에서 RequestBody 생성
//            RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpeg"),database_image_element.getValue());
//            MultipartBody.Part imagePart = MultipartBody.Part.createFormData("faceImage",database_image_element.getKey(),requestBody);
//
//            //이미지 출처 정보를 전송할 RequestBody 생성
//            RequestBody sourceBody = RequestBody.create(MediaType.parse("text/plain"),source);
//
//            //API 호출
//            Call<Void> call = service.uploadDatabaseImage(imagePart,sourceBody); //이미지 업로드 API 호출
//            call.enqueue(new Callback<Void>() { //비동기
//                @Override
//                public void onResponse(Call<Void> call, Response<Void> response) {
//                    if (response.isSuccessful()) {
//                        Log.e(TAG, "데이터베이스 업로드 성공함" + response.message());
//                        future.complete(null); // 작업 성공 시 future를 완료 상태로 설정
//                    }
//
//                }
//                @Override
//                public void onFailure(Call<Void> call, Throwable t) {
//                    Log.e(TAG, "데이터베이스 업로드 실패함" + t.getMessage());
//                    future.completeExceptionally(t); // 작업 실패 시 future에 예외를 설정
//                }
//            });
//        }
//        // 모든 futures가 완료될 때까지 기다림
//        return CompletableFuture.allOf(futuresList.toArray(new CompletableFuture[0]));
//    }

}

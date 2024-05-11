package com.example.metasearch.manager;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.metasearch.R;

//이미지 분석/ 삭제 다이얼로그를 띄우는 것
public class ImageDialogManager {
    private Dialog image_dialog; //이미지 다이얼로그
    private Dialog no_image_dialog; //분석할 이미지가 없으면 뜨는 다이얼로그
    private static ImageDialogManager instance;

    private ImageDialogManager(Context context){
        image_dialog = new Dialog(context);
        image_dialog.setContentView(R.layout.image_analyze_dialog);
        image_dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        Button imageDialogOkBtn = image_dialog.findViewById(R.id.btnDialogOk);
        //이미지 다이얼로그 버튼을 클릭하면 다이얼로그를 닫는다.
        imageDialogOkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                image_dialog.dismiss();
            }
        });

        no_image_dialog = new Dialog(context);
        no_image_dialog.setContentView(R.layout.no_image_analyze_dialog);
        no_image_dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        Button noimageDialogOkBtn = no_image_dialog.findViewById(R.id.btnDialogOk);
        noimageDialogOkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                no_image_dialog.dismiss();
            }
        });

    }
    public static ImageDialogManager getImageDialogManager(Context context){
        if (instance == null){
            instance = new ImageDialogManager(context);
        }
        return instance;
    }

    Dialog getImage_dialog(boolean is_add){ // 알림 반환
        if(!is_add){ //추가 분석이 아니라면
            TextView image_dialog_title = image_dialog.findViewById(R.id.dialogTitle);
            image_dialog_title.setText(R.string.title_text_delete);
            TextView image_dialog_text = image_dialog.findViewById(R.id.checkChange);
            image_dialog_text.setText(R.string.explain_text_delete); //삭제된 이미지 글
            TextView image_dialog_text_explain = image_dialog.findViewById(R.id.explainAlarm1); //업데이트 알림
            image_dialog_text_explain.setText(R.string.formatted_text_delete);

            return image_dialog;
        }
        return image_dialog;
    }

    public void show_image_dialog_notificaiton(Context context,boolean is_add){
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    image_dialog = getImage_dialog(is_add);
                    image_dialog.show();

                }
            });
        }
    }

    //분석할 이미지가 없을 시에 분석할 이미지가 없다는 다이얼로그가 띄워짐
    public void show_no_image_dialog_notification(Context context){
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    no_image_dialog.show();
                }
            });
        }
    }

}

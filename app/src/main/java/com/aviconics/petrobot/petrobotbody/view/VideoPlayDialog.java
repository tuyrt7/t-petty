package com.aviconics.petrobot.petrobotbody.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import com.aviconics.petrobot.petrobotbody.R;
import com.aviconics.petrobot.petrobotbody.util.SignDevice;



public class VideoPlayDialog extends Dialog {

    Context context;
    private View dialogView;


    public VideoPlayDialog(Context context) {
        super(context);
        this.context = context;
    }

    public VideoPlayDialog(Context context, int theme) {
        super(context, theme);//全屏
        this.context = context;
        LayoutInflater inflater = LayoutInflater.from(context);
        dialogView = inflater.inflate(R.layout.dialog_video, null);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(dialogView);

    }

    @Override
    public View findViewById(int id) {
        //重写findViewById方法获取对话框中控件
        return super.findViewById(id);
    }

    public View getDialogView() {
        //获得对话框view
        return dialogView;
    }

    @Override
    public void show() {
        super.show();
        SignDevice.getSign().setUiDialogShow(true);
        showBackLight();
    }

    @Override
    public void dismiss() {
        super.dismiss();
        SignDevice.getSign().setUiDialogShow(false);
        disBackLight();
    }


    private void showBackLight() {
    }

    private void disBackLight() {
    }
}


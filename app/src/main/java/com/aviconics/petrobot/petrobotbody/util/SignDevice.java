package com.aviconics.petrobot.petrobotbody.util;

/**
 * Created by futao on 2017/3/5.
 */

public class SignDevice {

    private SignDevice(){}
    private static class SignDeviceton {
        private static final SignDevice SINGLE = new SignDevice();
    }

    public static SignDevice getSign() {
        return SignDeviceton.SINGLE;
    }

    private boolean proToCapture;//wifi 保护 机制触发--重新配置网络
    private boolean isSendBc = false;//是否发局域网广播标记
    private boolean isPetUpdating = false;//** 是否设备升级中 *
    private boolean isLidOpen = false;//** 食盒盖是否打开
    private boolean isCallByUser = false; //** 是否接收到电话请求
    private boolean isPlayMusic = false;//** 音乐服务播放音乐的标记
    private boolean isLoginHX = false;//**  环信账号登陆状态
    private boolean isUpdateWifi = false; //** 是否主动关闭wifi(更新wifi)
    private boolean isConnectAc;//是否连上云平台
    private boolean isSingleCall = true;//单双向通话
    private boolean isUiDialogShow = false;//ui在显示
    private boolean isCompleteMediaData;//media 信息插入数据库完成
    private boolean isVideoPlay;//视频播放状态
    private int mediaState;//media 播放状态 0、1、2 未播、播音乐、播视频
    private boolean isHintOn;//显示提示页面
    private boolean isShowConnInMain;//进入main显示连接页面

    private boolean isCaptureOn;//正在显示扫描
    private boolean isUiVideoOn;//正在显示ui activity
    private boolean isVideoPlayOn;//正在显示视频 activity

    public boolean isVideoPlayOn() {
        return isVideoPlayOn;
    }

    public void setVideoPlayOn(boolean videoPlayOn) {
        isVideoPlayOn = videoPlayOn;
    }


    public boolean isCaptureOn() {
        return isCaptureOn;
    }

    public void setCaptureOn(boolean captureOn) {
        isCaptureOn = captureOn;
    }

    public boolean isUiVideoOn() {
        return isUiVideoOn;
    }

    public void setUiVideoOn(boolean uiVideoOn) {
        isUiVideoOn = uiVideoOn;
    }


    public boolean isShowConnInMain() {
        return isShowConnInMain;
    }

    public void setShowConnInMain(boolean showConnInMain) {
        isShowConnInMain = showConnInMain;
    }


    public boolean isHintOn(){
        return isHintOn;
    }

    public void setHintOn(boolean hintOn) {
        isHintOn = hintOn;
    }

    public boolean isVideoPlay() {
        return isVideoPlay;
    }

    public void setVideoPlay(boolean videoPlay) {
        isVideoPlay = videoPlay;
    }

    public int getMediaState() {
        return mediaState;
    }

    public void setMediaState(int mediaState) {
        this.mediaState = mediaState;
    }


    public boolean isUiDialogShow() {
        return isUiDialogShow;
    }

    public void setUiDialogShow(boolean uiDialogShow) {
        isUiDialogShow = uiDialogShow;
    }

    public boolean isCompleteMediaData() {
        return isCompleteMediaData;
    }

    public void setCompleteMediaData(boolean completeMediaData) {
        isCompleteMediaData = completeMediaData;
    }

    public boolean isSingleCall() {
        return isSingleCall;
    }

    public void setSingleCall(boolean singleCall) {
        isSingleCall = singleCall;
    }


    public boolean isConnectAc() {
        return isConnectAc;
    }

    public void setConnectAc(boolean connectAc) {
        isConnectAc = connectAc;
    }


    public boolean isUpdateWifi() {
        return isUpdateWifi;
    }

    public void setUpdateWifi(boolean updateWifi) {
        isUpdateWifi = updateWifi;
    }

    public boolean isLoginHX() {

        return isLoginHX;
    }

    public void setLoginHX(boolean loginHX) {
        isLoginHX = loginHX;
    }

    public boolean isPlayMusic() {
        return isPlayMusic;
    }

    public void setPlayMusic(boolean playMusic) {
        isPlayMusic = playMusic;
    }


    public boolean isCallByUser() {
        return isCallByUser;
    }

    public void setCallByUser(boolean callByUser) {
        isCallByUser = callByUser;
    }

    public boolean isLidOpen() {
        return isLidOpen;
    }

    public void setLidOpen(boolean lidOpen) {
        isLidOpen = lidOpen;
    }

    public boolean isPetUpdating() {
        return isPetUpdating;
    }

    public void setPetUpdating(boolean petUpdating) {
        isPetUpdating = petUpdating;
    }

    public boolean isSendBc() {
        return isSendBc;
    }

    public void setSendBc(boolean sendBc) {
        isSendBc = sendBc;
    }

    public boolean isProToCapture() {
        return proToCapture;
    }

    public void setProToCapture(boolean proToCapture) {
        this.proToCapture = proToCapture;
    }
}

package com.aviconics.petrobot.petrobotbody.module.ui.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.widget.Toast;

import com.aviconics.petrobot.petrobotbody.R;
import com.hyphenate.EMCallBack;
import com.hyphenate.EMError;
import com.hyphenate.chat.EMCallManager;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.exceptions.EMServiceNotReadyException;
import com.hyphenate.util.EMLog;

@SuppressLint("Registered")
public class CallActivity extends Activity {
    public final static String TAG = "NICK";
    protected final int MSG_CALL_MAKE_VIDEO = 0;
    protected final int MSG_CALL_MAKE_VOICE = 1;
    protected final int MSG_CALL_ANSWER = 2;
    protected final int MSG_CALL_REJECT = 3;
    protected final int MSG_CALL_END = 4;
    protected final int MSG_CALL_RLEASE_HANDLER = 5;
    protected final int MSG_CALL_SWITCH_CAMERA = 6;

    protected boolean isInComingCall;
    protected String username;
    protected CallingState callingState = CallingState.CANCELLED;
    protected AudioManager audioManager;
    // protected SoundPool soundPool;
    // protected Ringtone ringtone;
    // protected int outgoing;
    protected boolean isAnswered = false; //表示接电话 的标记

    EMCallManager.EMCallPushProvider pushProvider;
    /**
     * 0：voice call，1：video call
     */
    protected int callType = 0;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

        pushProvider = new EMCallManager.EMCallPushProvider() {

            void updateMessageText(final EMMessage oldMsg, final String to) {
                // update local message text
                EMConversation conv = EMClient.getInstance().chatManager().getConversation(oldMsg.getTo());
                conv.removeMessage(oldMsg.getMsgId());
            }

            @Override
            public void onRemoteOffline(final String to) {

                //this function should exposed & move to Demo
                EMLog.d(TAG, "onRemoteOffline, to:" + to);

                final EMMessage message = EMMessage.createTxtSendMessage("You have an incoming call", to);
                // set the user-defined extension field
                message.setAttribute("em_apns_ext", true);

                message.setAttribute("is_voice_call", callType == 0 ? true : false);

                message.setMessageStatusCallback(new EMCallBack() {

                    @Override
                    public void onSuccess() {
                        EMLog.d(TAG, "onRemoteOffline success");
                        updateMessageText(message, to);
                    }

                    @Override
                    public void onError(int code, String error) {
                        EMLog.d(TAG, "onRemoteOffline Error");
                        updateMessageText(message, to);
                    }

                    @Override
                    public void onProgress(int progress, String status) {
                    }
                });
                // send messages
                EMClient.getInstance().chatManager().sendMessage(message);
            }
        };

        EMClient.getInstance().callManager().setPushProvider(pushProvider);
    }

    @Override
    protected void onDestroy() {
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setMicrophoneMute(false);

        if (pushProvider != null) {
            EMClient.getInstance().callManager().setPushProvider(null);
            pushProvider = null;
        }
        releaseHandler();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        handler.sendEmptyMessage(MSG_CALL_END);
        finish();
        super.onBackPressed();
    }

   /* Runnable timeoutHangup = new Runnable() {

        @Override
        public void run() {
            handler.sendEmptyMessage(MSG_CALL_END);
        }
    };*/

    HandlerThread callHandlerThread = new HandlerThread("callHandlerThread");

    {
        callHandlerThread.start();
    }

    protected Handler handler = new Handler(callHandlerThread.getLooper()) {
        @Override
        public void handleMessage(Message msg) {
            EMLog.d("EMCallManager CallActivity", "handleMessage ---enter block--- msg.what:" + msg.what);
            switch (msg.what) {
                case MSG_CALL_MAKE_VIDEO:
                case MSG_CALL_MAKE_VOICE:
                    try {
                        if (msg.what == MSG_CALL_MAKE_VIDEO) {
                            EMClient.getInstance().callManager().makeVideoCall(username);
                        } else {
                            EMClient.getInstance().callManager().makeVoiceCall(username);
                        }
                    } catch (final EMServiceNotReadyException e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            public void run() {
                                String st2 = e.getMessage();
                                if (e.getErrorCode() == EMError.CALL_REMOTE_OFFLINE) {
                                    st2 = getResources().getString(R.string.The_other_is_not_online);
                                } else if (e.getErrorCode() == EMError.USER_NOT_LOGIN) {
                                    st2 = getResources().getString(R.string.Is_not_yet_connected_to_the_server);
                                } else if (e.getErrorCode() == EMError.INVALID_USER_NAME) {
                                    st2 = getResources().getString(R.string.illegal_user_name);
                                } else if (e.getErrorCode() == EMError.CALL_BUSY) {
                                    st2 = getResources().getString(R.string.The_other_is_on_the_phone);
                                } else if (e.getErrorCode() == EMError.NETWORK_ERROR) {
                                    st2 = getResources().getString(R.string.can_not_connect_chat_server_connection);
                                }
                                Toast.makeText(CallActivity.this, st2, Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        });
                    }
                    break;
                case MSG_CALL_ANSWER:
                    EMLog.d(TAG, "MSG_CALL_ANSWER");
                    //                if (ringtone != null)
                    //                    ringtone.stop();
                    if (isInComingCall) {
                        try {
                            EMClient.getInstance().callManager().answerCall();
                            isAnswered = true;
                            // meizu MX5 4G, hasDataConnection(context) return status is incorrect
                            // MX5 con.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected() return false in 4G
                            // so we will not judge it, App can decide whether judge the network status

                            //                        if (NetUtils.hasDataConnection(CallActivity.this)) {
                            //                            EMClient.getInstance().callManager().answerCall();
                            //                            isAnswered = true;
                            //                        } else {
                            //                            runOnUiThread(new Runnable() {
                            //                                public void run() {
                            //                                    final String st2 = getResources().getString(R.string.Is_not_yet_connected_to_the_server);
                            //                                    Toast.makeText(CallActivity.this, st2, Toast.LENGTH_SHORT).show();
                            //                                }
                            //                            });
                            //                            throw new Exception();
                            //                        }
                        } catch (Exception e) {
                            e.printStackTrace();
                            finish();
                            return;
                        }
                    } else {
                        EMLog.d(TAG, "answer call isInComingCall:false");
                    }
                    break;
                case MSG_CALL_REJECT:
                    //                if (ringtone != null)
                    //                    ringtone.stop();
                    try {
                        EMClient.getInstance().callManager().rejectCall();
                    } catch (Exception e1) {
                        e1.printStackTrace();
                        finish();
                    }
                    callingState = CallingState.REFUSED;
                    break;
                case MSG_CALL_END:
                    try {
                        EMClient.getInstance().callManager().endCall();
                        isAnswered = false;
                    } catch (Exception e) {
                        finish();
                    }

                    break;
                case MSG_CALL_RLEASE_HANDLER:
                    try {
                        EMClient.getInstance().callManager().endCall();
                        isAnswered = false;
                    } catch (Exception e) {
                    }
                    //handler.removeCallbacks(timeoutHangup);
                    handler.removeMessages(MSG_CALL_MAKE_VIDEO);
                    handler.removeMessages(MSG_CALL_MAKE_VOICE);
                    handler.removeMessages(MSG_CALL_ANSWER);
                    handler.removeMessages(MSG_CALL_REJECT);
                    handler.removeMessages(MSG_CALL_END);
                    callHandlerThread.quit();
                    break;
                case MSG_CALL_SWITCH_CAMERA:
                    EMClient.getInstance().callManager().switchCamera();
                    break;
                default:
                    break;
            }
            EMLog.d("EMCallManager CallActivity", "handleMessage ---exit block--- msg.what:" + msg.what);
        }
    };

    void releaseHandler() {
        handler.sendEmptyMessage(MSG_CALL_RLEASE_HANDLER);
    }


    protected void closeSpeakerOn() {

        try {
            if (audioManager != null) {
                // int curVolume =
                // audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
                if (audioManager.isSpeakerphoneOn())
                    audioManager.setSpeakerphoneOn(false);
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                // audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
                // curVolume, AudioManager.STREAM_VOICE_CALL);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    enum CallingState {
        CANCELLED, NORMAL, REFUSED, BEREFUSED, UNANSWERED, OFFLINE, NO_RESPONSE, BUSY, VERSION_NOT_SAME
    }
}

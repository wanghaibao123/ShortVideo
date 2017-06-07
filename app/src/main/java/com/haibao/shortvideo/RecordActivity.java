package com.haibao.shortvideo;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.wysaid.camera.CameraInstance;
import org.wysaid.myUtils.FileUtil;
import org.wysaid.myUtils.ImageUtil;
import org.wysaid.myUtils.MsgUtil;
import org.wysaid.view.CameraRecordGLSurfaceView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.haibao.shortvideo.R.id.iv_back;
import static com.haibao.shortvideo.R.id.rb_start;
import static com.haibao.shortvideo.R.id.rl_bottom;
import static com.haibao.shortvideo.R.id.rl_bottom2;

public class RecordActivity extends AppCompatActivity implements View.OnClickListener {
    private List<String> listViews = new ArrayList<>();
    private List<Integer> listtime = new ArrayList<>();

    private CameraRecordGLSurfaceView mCameraRecordGLSurfaceView;
    private ImageView mSwithView;
    private RecordedButton rb_start;


    private int totalTomer = 0;


    private RelativeLayout rl_bottom2;
    private RelativeLayout rl_bottom;
    private ImageView iv_back;
    private String outputPath;
    private MyVideoSplicing myVideoSplicing;

    public final static String LOG_TAG = CameraRecordGLSurfaceView.LOG_TAG;

    public static String lastVideoPathFileName = FileUtil.getPath() + "/lastVideoPath.txt";
    boolean isRecording = false;
    String recordFilename;
    Timer timer;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 100:
                    totalTomer += 1;
                    rb_start.setProgress(totalTomer);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        initView();
        initData();
        initEvent();
    }

    public void initView() {
        mCameraRecordGLSurfaceView = (CameraRecordGLSurfaceView) findViewById(R.id.glsurfaceview_camera);
        mSwithView = (ImageView) findViewById(R.id.btn_camera_switch);
        rb_start = (RecordedButton) findViewById(R.id.rb_start);
        rl_bottom2 = (RelativeLayout) findViewById(R.id.rl_bottom2);
        rl_bottom = (RelativeLayout) findViewById(R.id.rl_bottom);
        ImageView iv_next = (ImageView) findViewById(R.id.iv_next);
        ImageView iv_close = (ImageView) findViewById(R.id.iv_close);
        ImageView iv_finish = (ImageView) findViewById(R.id.iv_finish);
        iv_back = (ImageView) findViewById(R.id.iv_back);

        mSwithView.setOnClickListener(this);
        iv_back.setOnClickListener(this);
        iv_finish.setOnClickListener(this);
        iv_next.setOnClickListener(this);
        iv_close.setOnClickListener(this);
        mCameraRecordGLSurfaceView.setFitFullView(true);


        mCameraRecordGLSurfaceView.presetRecordingSize(480, 640);
        //mCameraRecordGLSurfaceView.presetRecordingSize(720, 1280);
        mCameraRecordGLSurfaceView.setZOrderOnTop(false);
        mCameraRecordGLSurfaceView.setZOrderMediaOverlay(true);
    }

    public void initData() {

    }

    //本次段落是否录制完成
    private boolean isRecordedOver;

    public void initEvent() {
        rb_start.setMax(600);
        rb_start.setOnGestureListener(new RecordedButton.OnGestureListener() {
            @Override
            public void onLongClick() {
                isRecordedOver = false;
                rb_start.setSplit();
                startRecord();
            }

            @Override
            public void onClick() {
            }

            @Override
            public void onLift() {
                isRecordedOver = true;
                endRecord();
                changeButton(listViews.size() > 0);
            }

            @Override
            public void onOver() {
                isRecordedOver = true;
                rb_start.closeButton();
                endRecord();
                SystemClock.sleep(1000);
                videofinish();
            }
        });

        myVideoSplicing = new MyVideoSplicing(this, listViews);

        myVideoSplicing.setCallBack(new MyVideoSplicing.ResoultCallBack() {
            @Override
            public void success() {
                showText("拼接成功");
            }

            @Override
            public void fail() {
                showText("拼接失败");
            }

            @Override
            public void start() {
                showText("拼接开始");
            }
        });
    }


    /**
     * 开始拼接
     */
    public void startVideoSplice() {
        outputPath = ImageUtil.getPath() + "/rec_" + System.currentTimeMillis() + ".mp4";
        myVideoSplicing.videoSplice(outputPath);
    }

    public void videofinish() {
        startVideoSplice();
        changeButton(false);
        rl_bottom2.setVisibility(View.VISIBLE);
        rb_start.setVisibility(View.INVISIBLE);
    }

    public void deleteLastOne() {
        listViews.remove(listViews.size() - 1);
        listtime.remove(listtime.size() - 1);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_back://删除段落
                if (rb_start.isDeleteMode()) {//判断是否要删除视频段落
                    deleteLastOne();
                    rb_start.setProgress(totalTomer = listtime.size() > 0 ? listtime.get(listtime.size() - 1) : 0);
                    rb_start.deleteSplit();
                    iv_back.setImageResource(R.mipmap.video_delete);
                    changeButton(listViews.size() > 0);
                } else if (listViews.size() > 0) {
                    rb_start.setDeleteMode(true);
                    iv_back.setImageResource(R.mipmap.video_delete_click);
                }
                break;
            case R.id.iv_finish://合成视频
                videofinish();
                break;
            case R.id.iv_next://跳转
                Intent intent = new Intent(this, VideoActivity.class);
                intent.putExtra("outputPath", outputPath);
                startActivity(intent);
                break;
            case R.id.iv_close://关闭
                finish();
                break;
            case R.id.btn_camera_switch:
                mCameraRecordGLSurfaceView.switchCamera();
                break;
        }
    }

    private void changeButton(boolean flag) {
        if (flag) {
            rl_bottom.setVisibility(View.VISIBLE);
        } else {
            rl_bottom.setVisibility(View.GONE);
        }
    }

    private void showText(final String s) {
        mCameraRecordGLSurfaceView.post(new Runnable() {
            @Override
            public void run() {
                MsgUtil.toastMsg(RecordActivity.this, s);
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        CameraInstance.getInstance().stopCamera();
        Log.i(LOG_TAG, "activity onPause...");
        mCameraRecordGLSurfaceView.release(null);
        mCameraRecordGLSurfaceView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mCameraRecordGLSurfaceView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
    }

    /**
     * 开始录制
     */
    public void startRecord() {
        if (!isRecording) {
            Log.i(LOG_TAG, "Start recording...");
            recordFilename = ImageUtil.getPath() + "/rec_" + System.currentTimeMillis() + ".mp4";
            mCameraRecordGLSurfaceView.startRecording(recordFilename, new CameraRecordGLSurfaceView.StartRecordingCallback() {
                @Override
                public void startRecordingOver(boolean success) {
                    if (success) {
                        isRecording = !isRecording;
                        showText("开始录制");
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                Message message = new Message();
                                message.what = 100;
                                handler.sendMessage(message);
                            }
                        }, 0, 100);
                        FileUtil.saveTextContent(recordFilename, lastVideoPathFileName);
                    } else {
                        showText("开始录制失败");
                    }
                }
            });
        }
    }

    public void endRecord() {
        isRecording = !isRecording;
        timer.cancel();
        showText("录制结束 视频地址: " + recordFilename);
        listViews.add(recordFilename);
        listtime.add(totalTomer);
        Log.i(LOG_TAG, "End recording...");
        mCameraRecordGLSurfaceView.endRecording(new CameraRecordGLSurfaceView.EndRecordingCallback() {
            @Override
            public void endRecordingOK() {
                Log.i(LOG_TAG, "End recording OK");
            }
        });
    }
}

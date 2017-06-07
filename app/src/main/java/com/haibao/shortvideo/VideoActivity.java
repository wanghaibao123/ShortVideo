package com.haibao.shortvideo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.wysaid.common.Common;
import org.wysaid.myUtils.FileUtil;
import org.wysaid.myUtils.MsgUtil;
import org.wysaid.nativePort.CGEFFmpegNativeLibrary;
import org.wysaid.nativePort.CGENativeLibrary;
import org.wysaid.view.VideoPlayerGLSurfaceView;

import java.io.InputStream;

public class VideoActivity extends AppCompatActivity {
    VideoPlayerGLSurfaceView mPlayerView;

    private View layout_filter;
    private Button summit;
    private RecyclerView recyclerView;
    String mCurrentConfig;
    private String outputFilename;
    private String lastVideoFileName;

    public static final String LOG_TAG = "VideoActivity";

    protected Thread mThread;

    protected boolean mShouldStopThread = false;
    private ProgressDialog progressDialog;

    protected void showLoadDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("正在处理");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    protected void hideLoadDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private VideoPlayerGLSurfaceView.PlayCompletionCallback playCompletionCallback = new VideoPlayerGLSurfaceView.PlayCompletionCallback() {
        @Override
        public void playComplete(MediaPlayer player) {
            Log.i(Common.LOG_TAG, "The video playing is over, restart...");
            player.start();
        }

        @Override
        public boolean playFailed(MediaPlayer player, final int what, final int extra) {
            MsgUtil.toastMsg(VideoActivity.this, String.format("Error occured! Stop playing, Err code: %d, %d", what, extra));
            return true;
        }
    };

    protected void showMsg(final String msg) {

        VideoActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MsgUtil.toastMsg(VideoActivity.this, msg, Toast.LENGTH_SHORT);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        initView();
        initData();
        initEvent();

    }

    public void initView() {
        summit = (Button) findViewById(R.id.bt_sunmit);
        recyclerView = (RecyclerView) findViewById(R.id.filter_listView);
        layout_filter = findViewById(R.id.layout_filter);
        mPlayerView = (VideoPlayerGLSurfaceView) findViewById(R.id.videoGLSurfaceView);
        mPlayerView.setZOrderOnTop(false);
        mPlayerView.setZOrderMediaOverlay(true);
        lastVideoFileName= getIntent().getStringExtra("outputPath");
        if (lastVideoFileName == null) lastVideoFileName = FileUtil.getTextContent(RecordActivity.lastVideoPathFileName);
        if (lastVideoFileName == null) {
            MsgUtil.toastMsg(VideoActivity.this, "No video is recorded, please record one in the 2nd case.");
            return;
        }
        mPlayerView.setFitFullView(true);
        Uri lastVideoUri = Uri.parse(lastVideoFileName);
        mPlayerView.setVideoUri(lastVideoUri, new VideoPlayerGLSurfaceView.PlayPreparedCallback() {
            @Override
            public void playPrepared(MediaPlayer player) {
                Log.i(Common.LOG_TAG, "The video is prepared to play");
                player.start();
            }
        }, playCompletionCallback);
    }

    public void initData() {
        initRecyclerView();
    }

    public void initEvent() {
        summit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPlayerView.getPlayer().stop();
                testCaseOffscreenVideoRendering();
                MyAsyncTask task=new MyAsyncTask();
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });
    }


    /**
     * Intent intent = new Intent(this, B.class);
     * intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
     * startActivity(intent);
     */
    public void initRecyclerView() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        FilterAdapter adapter = new FilterAdapter(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(adapter);
        adapter.setOnFilterChangeListener(new FilterAdapter.onFilterChangeListener() {
            @Override
            public void onFilterChanged(String filterType) {
                mPlayerView.setFilterWithConfig(filterType);
                mCurrentConfig = filterType;
            }
        });
    }


    public void testCaseOffscreenVideoRendering() {


        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(LOG_TAG, "Test case 1 clicked!\n");
                outputFilename = FileUtil.getPath() + "/blendVideo.mp4";
                String inputFileName = lastVideoFileName;
                if (inputFileName == null) {
                    showMsg("No video is recorded, please record one in the 2nd case.");
                    return;
                }
                Bitmap bmp;
                try {
                    AssetManager am = getAssets();
                    InputStream is;
                    is = am.open(null);
                    bmp = BitmapFactory.decodeStream(is);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Can not open blend image file!");
                    bmp = null;
                }
                CGEFFmpegNativeLibrary.generateVideoWithFilter(outputFilename, inputFileName, mCurrentConfig, 1.0f, bmp, CGENativeLibrary.TextureBlendMode.CGE_BLEND_ADDREV, 1.0f, false);
                showMsg("Done! The file is generated at: " + outputFilename);
                Log.i(LOG_TAG, "Done! The file is generated at: " + outputFilename);
            }
        });
        mThread.start();
    }

    public class MyAsyncTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showLoadDialog();
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            hideLoadDialog();
            if(aBoolean){
                showMsg("成功");
                Intent intent = new Intent(VideoActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("path",outputFilename);
                startActivity(intent);
            }else{
                showMsg("失败");
            }
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            Log.i(LOG_TAG, "Test case 1 clicked!\n");
            outputFilename = FileUtil.getPath() + "/blendVideo.mp4";
            String inputFileName = FileUtil.getTextContent(RecordActivity.lastVideoPathFileName);
            if (inputFileName == null) {
                showMsg("No video is recorded, please record one in the 2nd case.");
                return false;
            }
            Bitmap bmp;
            try {
                AssetManager am = getAssets();
                InputStream is;
               /* is = am.open(null);
                bmp = BitmapFactory.decodeStream(is);*///可添加水印
            } catch (Exception e) {
                Log.e(LOG_TAG, "Can not open blend image file!");
                bmp = null;
            }
            CGEFFmpegNativeLibrary.generateVideoWithFilter(outputFilename, inputFileName, mCurrentConfig, 1.0f,null, CGENativeLibrary.TextureBlendMode.CGE_BLEND_ADDREV, 1.0f, false);
            Log.i(LOG_TAG, "Done! The file is generated at: " + outputFilename);
            return true;
        }
    }

}


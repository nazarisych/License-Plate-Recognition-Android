package com.tcvdev.lpr;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AlertDialog;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.os.Bundle;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.codekidlabs.storagechooser.StorageChooser;
import com.codekidlabs.storagechooser.Content;


import com.frank.ffmpeg.VideoPlayer;
import com.tcvdev.lpr.common.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SurfaceHolder.Callback,
        VideoPlayer.FFMPEGCallback {
    private static final int STATUS_STOP = 0;
    private static final int STATUS_PAUSE = 1;
    private static final int STATUS_PLAY = 2;
    private static final String TAG = "LPR";
    private EditText m_etVideoPath, m_etUSBPath;
    private Button m_btnVideoOpen, m_btnUSBOpen, m_btnPlay, m_btnPause, m_btnStop;
    private Button m_btnRewind30, m_btnRewind10, m_btnForward30, m_btnForward10;
    private RadioButton m_rbVideo, m_rbUSB;
    private Spinner m_spinCountry;
    private SurfaceView m_sfImageView;
    private SurfaceHolder m_surfaceHolder;
    private TextView m_tvPlayingTime;
    private SeekBar m_sbTime;
    private VideoPlayer m_videoPlayer;
    private RelativeLayout m_rlImageView;
    private MediaMetadataRetriever mediaMetadataRetriever;
    private int m_nVideoWidth = 0;
    private int m_nVideoHeight = 0;
    private boolean m_bVideoPaused = false;
    private long mDuration;
    private boolean m_bFirstOpen = true;
    private byte[] byteVideoFrame;
    private int m_nPlayStatus;
    private final StorageChooser.Builder builder = new StorageChooser.Builder();
    private final ArrayList<String> mCountryList = new ArrayList<>();

    private static final String[] perms = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();
        initVariable();
        m_videoPlayer = new VideoPlayer();
        m_videoPlayer.setFFMPEGCallback(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 444) {
            boolean granted = true;
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    granted = false;
                    break;
                }
            }
            if (!granted) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.str_title_permission_denied);
                builder.setMessage(R.string.str_msg_permission_denied);
                builder.setNegativeButton(R.string.str_close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
                AlertDialog dlg = builder.create();
                dlg.setCanceledOnTouchOutside(false);
                dlg.show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_video_open) {
            onBtnVideoOpen();
        }
        if (v.getId() == R.id.btn_video_play) {
            onBtnVideoPlay();
        }

        if (v.getId() == R.id.btn_video_pause) {
            onBtnVideoPause();
        }

        if (v.getId() == R.id.btn_video_stop) {
            onBtnVideoStop();
        }
    }

    private void onBtnVideoStop() {
        m_nPlayStatus = STATUS_STOP;

        m_videoPlayer.stop();

        m_btnPlay.setEnabled(true);
        m_btnPause.setEnabled(false);
        m_btnStop.setEnabled(false);

        setPlayingTime(0, 0);
    }

    private void setPlayingTime(int cur_time, int duration) {
        final String strTime = Util.getTextFromSecond(cur_time) + " / " + Util.getTextFromSecond(duration);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                m_tvPlayingTime.setText(strTime);
                m_sbTime.setMax(duration);
                m_sbTime.setProgress(cur_time);
            }
        });
    }

    private void onBtnVideoPause() {
        m_nPlayStatus = STATUS_PAUSE;

        m_btnPlay.setEnabled(true);
        m_btnPause.setEnabled(false);
        m_btnStop.setEnabled(true);

        m_videoPlayer.pause();
    }

    private void onBtnVideoPlay() {
        m_btnPlay.setEnabled(false);
        m_btnPause.setEnabled(true);
        m_btnStop.setEnabled(true);
        if (m_nPlayStatus == STATUS_STOP) {
            try {
                loadVideo();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        m_videoPlayer.play();
        m_nPlayStatus = STATUS_PLAY;
    }

    void onBtnVideoOpen() {

        Content c = new Content();
        c.setCreateLabel("Create");
        c.setInternalStorageText("My Storage");
        c.setCancelLabel("Cancel");
        c.setSelectLabel("Select");
        c.setOverviewHeading("Choose Drive");

        builder.withActivity(this)
                .withFragmentManager(getFragmentManager())
                .setMemoryBarHeight(1.5f)
                .disableMultiSelect()
                .withContent(c);

        ArrayList<String> formats = new ArrayList<>();
        formats.add("mp4");
        formats.add("avi");
        builder.customFilter(formats);

        StorageChooser chooser = builder.build();

        chooser.setOnSelectListener(new StorageChooser.OnSelectListener() {
            @Override
            public void onSelect(String path) {
                m_etVideoPath.setText(path);
                m_btnPlay.setEnabled(true);
                m_btnPause.setEnabled(false);
                m_btnStop.setEnabled(false);
                try {
                    loadVideo();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        chooser.show();
    }

    void initUI() {

        m_rbVideo = findViewById(R.id.rb_video);
        m_rbUSB = findViewById(R.id.rb_usb);
        m_spinCountry = findViewById(R.id.spin_country);
        m_etVideoPath = findViewById(R.id.et_video_path);
        m_btnVideoOpen = findViewById(R.id.btn_video_open);
        m_etUSBPath = findViewById(R.id.et_usb_path);
        m_btnUSBOpen = findViewById(R.id.btn_usb_open);
        m_sfImageView = findViewById(R.id.sf_imageview);
        m_rlImageView = findViewById(R.id.rl_preview);
        m_tvPlayingTime = findViewById(R.id.tv_playing_time);
        m_sbTime = findViewById(R.id.sb_time);
        m_btnPlay = findViewById(R.id.btn_video_play);
        m_btnPause = findViewById(R.id.btn_video_pause);
        m_btnStop = findViewById(R.id.btn_video_stop);
        m_btnRewind10 = findViewById(R.id.btn_rewind_10);
        m_btnRewind30 = findViewById(R.id.btn_rewind_30);
        m_btnForward10 = findViewById(R.id.btn_forward_10);
        m_btnForward30 = findViewById(R.id.btn_forward_30);

        m_sbTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                setSeekSec(seekBar.getProgress());
            }
        });

        m_surfaceHolder = m_sfImageView.getHolder();
        m_surfaceHolder.addCallback(this);
        m_rbVideo.setChecked(true);
        m_rbUSB.setChecked(false);

        m_rbVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableViews(1);
            }
        });

        m_rbUSB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableViews(2);
            }
        });

        m_btnVideoOpen.setOnClickListener(this);
        m_btnUSBOpen.setOnClickListener(this);
        m_btnPlay.setOnClickListener(this);
        m_btnPause.setOnClickListener(this);
        m_btnStop.setOnClickListener(this);

        m_btnRewind10.setOnClickListener(this);
        m_btnRewind30.setOnClickListener(this);
        m_btnForward10.setOnClickListener(this);
        m_btnForward30.setOnClickListener(this);
        m_btnPlay.setEnabled(false);
        m_btnPause.setEnabled(false);
        m_btnStop.setEnabled(false);

        m_spinCountry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        enableViews(1);
    }

    void setSeekSec(float fltSec) {

        if (m_videoPlayer != null)
            m_videoPlayer.setSeekSec(fltSec);
    }

    void initVariable() {
        builder.allowCustomPath(true);
        builder.setType(StorageChooser.FILE_PICKER);
        builder.shouldResumeSession(true);

        Collections.addAll(mCountryList, getResources().getStringArray(R.array.arr_country));
        ArrayAdapter<String> mCountryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mCountryList);
        m_spinCountry.setAdapter(mCountryAdapter);
    }

    void enableViews(int mode) {

        if (mode == 1) {    //video
            m_rbUSB.setChecked(false);
            m_etUSBPath.setEnabled(false);
            m_btnUSBOpen.setEnabled(false);

            m_etVideoPath.setEnabled(true);
            m_btnVideoOpen.setEnabled(true);
        } else if (mode == 2) {     //USB

            m_rbVideo.setChecked(false);
            m_etVideoPath.setEnabled(false);
            m_btnVideoOpen.setEnabled(false);

            m_etUSBPath.setEnabled(true);
            m_btnUSBOpen.setEnabled(true);
        }
    }

    private void checkPermissions() {
        boolean granted = true;
        for (String perm : perms) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, perm) == PackageManager.PERMISSION_DENIED) {
                granted = false;
                break;
            }
        }
        if (!granted) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    perms,
                    444);
        }
    }

    private void loadVideo() throws IOException {

        if (!m_bFirstOpen)
            m_videoPlayer.stop();
        m_bFirstOpen = false;

        final String strVideoPath = m_etVideoPath.getText().toString();
        mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(this, Uri.parse(strVideoPath));
        m_nVideoWidth = Integer.parseInt(Objects.requireNonNull(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)));
        m_nVideoHeight = Integer.parseInt(Objects.requireNonNull(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)));
        //m_vwDetect.setImageSize(m_nVideoWidth, m_nVideoHeight);
        m_sfImageView.post(new Runnable() {
            @Override
            public void run() {

                int containerWidth = m_rlImageView.getWidth();
                int containerHeight = m_rlImageView.getHeight();

                float scaleX = m_nVideoWidth / (float) containerWidth;
                float scaleY = m_nVideoHeight / (float) containerHeight;
                float scale = Math.max(scaleX, scaleY);

                int realWidth = (int) (m_nVideoWidth / scale);
                int realHeight = (int) (m_nVideoHeight / scale);

                setVideoSize(realWidth, realHeight);
            }
        });
        byteVideoFrame = new byte[m_nVideoWidth * m_nVideoHeight * 4];

        new Thread(new Runnable() {
            @Override
            public void run() {
                m_videoPlayer.loadVideo(strVideoPath, m_surfaceHolder.getSurface(), byteVideoFrame);
            }
        }).start();

        String strDuration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        assert strDuration != null;
        mDuration = Long.parseLong(strDuration);
        if(mDuration <= 0) {
            mediaMetadataRetriever.release();
            return;
        }
        mediaMetadataRetriever.release();
    }

    private void setVideoSize(int width, int height) {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) m_sfImageView.getLayoutParams();
        params.height = height;
        params.width = width;
        m_sfImageView.setLayoutParams(params);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        if (m_rbVideo.isChecked() && m_bVideoPaused) {
            m_videoPlayer.setSurface(m_surfaceHolder.getSurface());
            m_videoPlayer.play();
            m_bVideoPaused = false;
        }

//        if (m_bCameraOpened && m_rbUSB.isChecked() && m_bUSBPaused ) {
//
//            openUSBCamera();
//            m_bUSBPaused = false;
//        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        if( m_rbVideo.isChecked()) {
            m_videoPlayer.pause();
            m_bVideoPaused = true;
        }
    }

    @Override
    public void onGrabFrame(int cur_time, int duration) {

    }

    @Override
    public void onPlayStatus(int play_status) {
        if (play_status == 0) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onBtnVideoStop();
                }
            });
        }
    }
}
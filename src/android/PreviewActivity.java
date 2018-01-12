package org.apache.cordova.camera;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import java.io.File;

import static org.apache.cordova.camera.CameraControls.IMAGE_REQUEST;
import static org.apache.cordova.camera.CameraControls.VIDEO_REQUEST;

public class PreviewActivity extends AppCompatActivity {

    ImageView imageView;

    VideoView videoView;
    ImageView playButton;

    private int requestCode;

    File video;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String package_name = getPackageName();
        Resources resources = getResources();

        setContentView(resources.getIdentifier("activity_preview", "layout", package_name));
        imageView = (ImageView) findViewById(resources.getIdentifier("image", "id", package_name));

        videoView = (VideoView) findViewById(resources.getIdentifier("video", "id", package_name));
//        playButton = (ImageView) findViewById(resources.getIdentifier("iv_play", "id", package_name));
//        playButton.setVisibility(View.INVISIBLE);
//
//        playButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                playButton.setVisibility(View.INVISIBLE);
//                videoView.setVideoURI(Uri.parse(video.getAbsolutePath()));
//                MediaController mediaController = new MediaController(PreviewActivity.this);
//                mediaController.setVisibility(View.GONE);
//                videoView.setMediaController(mediaController);
//                videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//                    @Override
//                    public void onPrepared(MediaPlayer mp) {
//                        mp.setLooping(true);
//                        mp.start();

//                        float multiplier = (float) videoView.getWidth() / (float) mp.getVideoWidth();
//                        videoView.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (mp.getVideoHeight() * multiplier)));
//                    }
//                });
//                videoView.start();
//            }
//        });

//        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//            @Override
//            public void onCompletion(MediaPlayer mp) {
//                videoView.stopPlayback();
//                playButton.setVisibility(View.VISIBLE);
//            }
//        });

        setupToolbar();
        requestCode = ((Integer)getIntent().getExtras().get("requestCode"));

        TextView buttonConfirm = (TextView) findViewById(resources.getIdentifier("buttonConfirm", "id", package_name));
        buttonConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);

                finishActivity(requestCode);

                finish();
            }
        });
        TextView buttonRetake = (TextView) findViewById(resources.getIdentifier("buttonRetake", "id", package_name));
        buttonRetake.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                setResult(RESULT_CANCELED, intent);

                finishActivity(requestCode);
                finish();
            }
        });




        if (requestCode == IMAGE_REQUEST) {
            Bitmap bitmap = ResultHolder.getImage();
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageBitmap(bitmap);

        }
        else if (requestCode == VIDEO_REQUEST) {
            video = ResultHolder.getVideo();
            if(video != null && video.exists()) {
//                playButton.setVisibility(View.VISIBLE);
                videoView.setVisibility(View.VISIBLE);
                videoView.setVideoURI(Uri.parse(video.getAbsolutePath()));
                MediaController mediaController = new MediaController(this);
                mediaController.setVisibility(View.GONE);
                videoView.setMediaController(mediaController);
                videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mp.setLooping(true);
                        mp.start();

//                        float multiplier = (float) videoView.getWidth() / (float) mp.getVideoWidth();
//                        videoView.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (mp.getVideoHeight() * multiplier)));
                    }
                });
                videoView.start();
            }else{
                Intent intent = new Intent();
                setResult(RESULT_CANCELED, intent);
                finishActivity(requestCode);
                return;
            }

        }else {
            Intent intent = new Intent();
            setResult(RESULT_CANCELED, intent);
            finishActivity(requestCode);
            return;
        }
    }

    private void setupToolbar() {
        if (getSupportActionBar() != null) {
            String package_name = getPackageName();
            Resources resources = getResources();

            View toolbarView = getLayoutInflater().inflate(resources.getIdentifier("action_bar", "layout", package_name), null, false);
            TextView titleView = (TextView) toolbarView.findViewById(resources.getIdentifier("toolbar_title", "id", package_name));
            titleView.setText(Html.fromHtml("Preview"));

            getSupportActionBar().setCustomView(toolbarView, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            getSupportActionBar().setDisplayShowCustomEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    private static float getApproximateFileMegabytes(Bitmap bitmap) {
        return (bitmap.getRowBytes() * bitmap.getHeight()) / 1024 / 1024;
    }

}

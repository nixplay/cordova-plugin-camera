package org.apache.cordova.camera;

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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import java.io.File;

public class PreviewActivity extends AppCompatActivity {

    ImageView imageView;

    VideoView videoView;

    TextView actualResolution;

    TextView approxUncompressedSize;

    TextView captureLatency;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String package_name = getPackageName();
        Resources resources = getResources();

        setContentView(resources.getIdentifier("activity_preview", "layout", package_name));
        imageView = (ImageView) findViewById(resources.getIdentifier("imageView", "id", package_name));

        videoView = (VideoView) findViewById(resources.getIdentifier("videoView", "id", package_name));

        actualResolution = (TextView) findViewById(resources.getIdentifier("actualResolution", "id", package_name));

        approxUncompressedSize = (TextView) findViewById(resources.getIdentifier("approxUncompressedSize", "id", package_name));

        captureLatency = (TextView) findViewById(resources.getIdentifier("captureLatency", "id", package_name));
        setupToolbar();

        Bitmap bitmap = ResultHolder.getImage();
        File video = ResultHolder.getVideo();

        if (bitmap != null) {
            imageView.setVisibility(View.VISIBLE);

            imageView.setImageBitmap(bitmap);

            actualResolution.setText(bitmap.getWidth() + " x " + bitmap.getHeight());
            approxUncompressedSize.setText(getApproximateFileMegabytes(bitmap) + "MB");
            captureLatency.setText(ResultHolder.getTimeToCallback() + " milliseconds");
        }

        else if (video != null) {
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

                    float multiplier = (float) videoView.getWidth() / (float) mp.getVideoWidth();
                    videoView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (mp.getVideoHeight() * multiplier)));
                }
            });
            //videoView.start();
        }

        else {
            finish();
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

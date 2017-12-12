package org.apache.cordova.camera;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.otaliastudios.cameraview.CameraView;


public class CordovaCameraActivity extends AppCompatActivity {
    CameraView cameraView;
    private CameraControls cameraControl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String package_name = getPackageName();
        Resources resources = getResources();

        int id = resources.getIdentifier("activity_cordova_camera", "layout", package_name);
        setContentView(id);

        Intent indent = getIntent();
        Bundle bundle = indent.getExtras();
        Uri path = (Uri) bundle.getParcelable(MediaStore.EXTRA_OUTPUT);

        Log.d("Path ", path.toString());
        cameraView = (CameraView) findViewById(resources.getIdentifier("camera", "id", package_name));
        cameraControl = (CameraControls) findViewById(resources.getIdentifier("camera_control", "id", package_name));
        cameraControl.video_path = path;

    }
    @Override
    protected void onResume() {
        super.onResume();
        cameraView.start();
    }

    @Override
    protected void onPause() {
        cameraView.stop();
        super.onPause();
    }
}

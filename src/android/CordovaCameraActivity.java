package org.apache.cordova.camera;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.otaliastudios.cameraview.CameraView;


public class CordovaCameraActivity extends AppCompatActivity {
    private static final String TAG = CordovaCameraActivity.class.getSimpleName();
    CameraView cameraView;
    private CameraControls cameraControl;
    private static final int CAMERA = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String package_name = getPackageName();
        Resources resources = getResources();

        setContentView(resources.getIdentifier("activity_cordova_camera", "layout", package_name));

        Intent indent = getIntent();
        cameraView = (CameraView) findViewById(resources.getIdentifier("camera", "id", package_name));
        cameraControl = (CameraControls) findViewById(resources.getIdentifier("camera_control", "id", package_name));


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

    @Override
    public void onBackPressed() {
        ResultHolder.dispose();
        setResult(RESULT_CANCELED, new Intent());
        finishActivity(CAMERA);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean valid = true;
        for (int grantResult : grantResults) {
            valid = valid && grantResult == PackageManager.PERMISSION_GRANTED;
        }
        if (valid && !cameraView.isStarted()) {
            cameraView.start();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == CameraControls.VIDEO_REQUEST) {
            Intent intent = new Intent();
            setResult(RESULT_OK, intent);
            finishActivity(CAMERA);
            finish();

        } else if (resultCode == RESULT_OK && requestCode == CameraControls.IMAGE_REQUEST) {
            Intent intent = new Intent();
            setResult(RESULT_OK, intent);
            finishActivity(CAMERA);
            finish();
        } else  if (resultCode == RESULT_CANCELED) {
            ResultHolder.dispose();
        }
    }
}

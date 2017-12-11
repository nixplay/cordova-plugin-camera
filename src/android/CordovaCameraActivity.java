package org.apache.cordova.camera;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.creedon.Nixplay.R;
import com.wonderkiln.camerakit.CameraView;

public class CordovaCameraActivity extends AppCompatActivity {
    com.wonderkiln.camerakit.CameraView cameraView;
    private CameraControls cameraControl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cordova_camera);
        Intent indent = getIntent();
        Bundle bundle = indent.getExtras();
        Uri path = (Uri) bundle.getParcelable(MediaStore.EXTRA_OUTPUT);

        Log.d("Path ", path.toString());
        cameraView = (CameraView) findViewById(R.id.camera);
        cameraControl = (CameraControls) findViewById(R.id.camera_control);
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

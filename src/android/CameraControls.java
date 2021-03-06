package org.apache.cordova.camera;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraUtils;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.Facing;
import com.otaliastudios.cameraview.Flash;
import com.otaliastudios.cameraview.SessionType;

import java.io.File;
import java.lang.reflect.Field;

import at.markushi.ui.CircleButton;


public class CameraControls extends LinearLayout {
    public static final int IMAGE_REQUEST = 0x112;
    public static final int VIDEO_REQUEST = 0x111;
    private final Context mContext;
    private final CircleButton captureButton;
    private final LinearLayoutManager linearLayoutManager;
//    private final CircleButton videoButton;

    private int cameraViewId = -1;
    private CameraView cameraView;

    private int coverViewId = -1;
    private View coverView;


    ImageView facingButton;


    ImageView flashButton;

    ProgressBar progressBar;
    Animator animator;

    private long captureDownTime;
    private long captureStartTime;
    private boolean pendingVideoCapture;
    private boolean capturingVideo;

    private File videoFile;
    boolean skipVideoCapture;
    byte[] imageTaken;

    public CameraControls(Context context) {
        this(context, null);
    }

    public CameraControls(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraControls(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        String package_name = context.getPackageName();
        Resources resources = context.getResources();

        View view = LayoutInflater.from(getContext()).inflate(resources.getIdentifier("camera_controls", "layout", package_name), this);

        if (attrs != null) {
            TypedArray a = resources.obtainAttributes(attrs, getResourceDeclareStyleableIntArray(context,"CameraControls"));
            try {

                cameraViewId = a.getResourceId(getResourceDeclareStyleableInt(context, "CameraControls_camera"), -1);
                coverViewId = a.getResourceId(getResourceDeclareStyleableInt(context, "CameraControls_cover"), -1);
            } finally {
                a.recycle();
            }
        }

        RecyclerView recyclerView = (RecyclerView) findViewById(resources.getIdentifier("recycleview","id", package_name));
        linearLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);

        captureButton = (CircleButton) view.findViewById(resources.getIdentifier("captureButton", "id", package_name));
        captureButton.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return onTouchCapture(v,event);
            }
        });

//        videoButton = (CircleButton) view.findViewById(resources.getIdentifier("videoButton", "id", package_name));
//        videoButton.setOnTouchListener(new OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                return onTouchVideo(v,event);
//            }
//        });

        facingButton = (ImageView) view.findViewById(resources.getIdentifier("facingButton", "id", package_name));
        facingButton.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return onTouchFacing(v,event);
            }
        });
        flashButton = (ImageView) view.findViewById(resources.getIdentifier("flashButton", "id", package_name));
        flashButton.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return onTouchFlash(v,event);
            }
        });

        progressBar = (ProgressBar) view.findViewById(resources.getIdentifier("progressBar", "id", package_name));
        endProgressBar();
    }

    private void startProgressBar() {
        animator = ObjectAnimator.ofInt(progressBar, "progress", 0, 500);
        animator.setInterpolator(new LinearInterpolator());
        animator.setDuration(15000);
        animator.start();
    }

    private void endProgressBar() {
        if (animator != null)
            animator.cancel();

        progressBar.clearAnimation();
        progressBar.setProgress(0);
    }

    /*********************************************************************************
     *   Returns the resource-IDs for all attributes specified in the
     *   given <declare-styleable>-resource tag as an int array.
     *
     *   @param  context     The current application context.
     *   @param  name        The name of the <declare-styleable>-resource-tag to pick.
     *   @return             All resource-IDs of the child-attributes for the given
     *                       <declare-styleable>-resource or <code>null</code> if
     *                       this tag could not be found or an error occured.
     *********************************************************************************/
    public static final int[] getResourceDeclareStyleableIntArray( Context context, String name )
    {
        try
        {
            //use reflection to access the resource class
            Field[] fields2 = Class.forName( context.getPackageName() + ".R$styleable" ).getFields();

            //browse all fields
            for ( Field f : fields2 )
            {
                //pick matching field
                if ( f.getName().equals( name ) )
                {
                    //return as int array
                    int[] ret = (int[])f.get( null );
                    return ret;
                }
            }
        }
        catch ( Throwable t )
        {
        }

        return null;
    }
    public static final int getResourceDeclareStyleableInt( Context context, String name )
    {
        try
        {
            //use reflection to access the resource class
            Field[] fields2 = Class.forName( context.getPackageName() + ".R$styleable" ).getFields();

            //browse all fields
            for ( Field f : fields2 )
            {
                //pick matching field
                if ( f.getName().equals( name ) )
                {
                    //return as int array
                    Integer ret = (Integer) f.get( null );
                    return ret;
                }
            }
        }
        catch ( Throwable t )
        {
        }

        return -1;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (cameraViewId != -1) {
            View view = getRootView().findViewById(cameraViewId);
            if (view instanceof CameraView) {
                cameraView = (CameraView) view;
                cameraView.addCameraListener(new CameraListener() {
                    @Override
                    public void onCameraOpened(CameraOptions options) {
                        super.onCameraOpened(options);
                    }

                    @Override
                    public void onCameraClosed() {
                        super.onCameraClosed();
                    }

                    @Override
                    public void onPictureTaken(byte[] jpeg) {
                        super.onPictureTaken(jpeg);
                        imageTaken = jpeg;
                        imageCaptured(jpeg);
                    }

                    @Override
                    public void onVideoTaken(File video) {
                        super.onVideoTaken(video);
                        Log.d("CameraControls", "skipVideoCapture: " + skipVideoCapture);
                        if (!skipVideoCapture)
                            videoCaptured(video);
                        else {
                            cameraView.setSessionType(SessionType.PICTURE);
                            postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    cameraView.capturePicture();
                                }
                            },250);
                        }
                    }
                });
                setFacingImageBasedOnCamera();

            }
        }

        if (coverViewId != -1) {
            View view = getRootView().findViewById(coverViewId);
            if (view != null) {
                coverView = view;
                coverView.setVisibility(GONE);
            }
        }
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);

    }

    private void setFacingImageBasedOnCamera() {
        String package_name = mContext.getPackageName();
        Resources resources = mContext.getResources();
        if (cameraView.getFacing() == Facing.FRONT) {
            facingButton.setImageResource(resources.getIdentifier("ic_facing_back", "drawable", package_name));
        } else {
            facingButton.setImageResource(resources.getIdentifier("ic_facing_front", "drawable", package_name));
        }
    }

    public void imageCaptured(byte[] b) {

        final long callbackTime = System.currentTimeMillis();
            CameraUtils.decodeBitmap(b, 5000, 5000, new CameraUtils.BitmapCallback() {
            @Override
            public void onBitmapReady(Bitmap bitmap) {
                ResultHolder.dispose();
                ResultHolder.setImage(bitmap);
                ResultHolder.setNativeCaptureSize(cameraView.getPictureSize());
                ResultHolder.setTimeToCallback(callbackTime - captureStartTime);
                Intent intent = new Intent(getContext(), PreviewActivity.class);
                intent.putExtra("requestCode", IMAGE_REQUEST);
                ((Activity) getContext()).startActivityForResult(intent, IMAGE_REQUEST);
            };
        });
    }

    public void videoCaptured(File video) {


        if (video != null && video.exists()) {
            ResultHolder.dispose();
            videoFile = video;
            ResultHolder.setVideo(videoFile);
            ResultHolder.setNativeCaptureSize(cameraView.getPreviewSize());
            Intent intent = new Intent(getContext(), PreviewActivity.class);
            intent.putExtra("requestCode", VIDEO_REQUEST);
            ((Activity)getContext()).startActivityForResult(intent,VIDEO_REQUEST);
        }
    }

    boolean onTouchVideo(View view, MotionEvent motionEvent) {
        handleViewTouchFeedback(view, motionEvent);
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN: {

                captureDownTime = System.currentTimeMillis();
                cameraView.setSessionType(SessionType.VIDEO);
//                pendingVideoCapture = true;
//                postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (pendingVideoCapture) {
//                            capturingVideo = true;
//
//
//                            videoFile  = getAlbumStorageDir();
//                            cameraView.setSessionType(SessionType.VIDEO);
//                            cameraView.startCapturingVideo(videoFile);
//
//                        }
//                    }
//                }, 250);
                break;
            }

            case MotionEvent.ACTION_UP: {
//                pendingVideoCapture = false;
//
//                if (capturingVideo) {
//                    capturingVideo = false;
//                    cameraView.stopCapturingVideo();
//                    cameraView.setSessionType(SessionType.PICTURE);
//                } else {
//                    cameraView.setSessionType(SessionType.PICTURE);
//                    captureStartTime = System.currentTimeMillis();
//                    cameraView.capturePicture();
//                }
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!capturingVideo) {
                            capturingVideo = true;
                            videoFile = getAlbumStorageDir();
                            cameraView.startCapturingVideo(videoFile);
                        } else {
                            capturingVideo = false;
                            cameraView.stopCapturingVideo();
                        }
                    }
                },250);
                break;
            }
        }
        return true;
    }
    boolean onTouchCapture(View view, MotionEvent motionEvent) {
        handleViewTouchFeedback(view, motionEvent);
        skipVideoCapture = false;

        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                pendingVideoCapture = true;

                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (pendingVideoCapture) {
                            captureDownTime = System.currentTimeMillis();
                            cameraView.setSessionType(SessionType.VIDEO);
                            startProgressBar();
                            capturingVideo = true;
//                            videoFile  = getAlbumStorageDir();
                            cameraView.startCapturingVideo(null, 15000);
//
                        }
                    }
                }, 500);
                break;
            }

            case MotionEvent.ACTION_UP: {
                pendingVideoCapture = false;
                endProgressBar();
                captureStartTime = System.currentTimeMillis();

                long totalTimeInMillis = captureStartTime - captureDownTime;
                if (totalTimeInMillis > 1600 && captureDownTime != -1L) {
                    // Toast.makeText(getContext(), "More than 1500 press and hold", Toast.LENGTH_SHORT).show();
                    if (capturingVideo) {
                        capturingVideo = false;
                        cameraView.stopCapturingVideo();
                    }
                } else {

                    if (capturingVideo) {
                        capturingVideo = false;
                        skipVideoCapture = true;
                        cameraView.stopCapturingVideo();
                    } else {

                        cameraView.setSessionType(SessionType.PICTURE);

                        postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                cameraView.capturePicture();
                            }
                        }, 250);
                    }

                }

                captureDownTime = -1L;
//                if (capturingVideo) {
//                    capturingVideo = false;
//                    cameraView.stopCapturingVideo();
//                } else {
//                    cameraView.setSessionType(SessionType.PICTURE);
//
//                    postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            cameraView.capturePicture();
//                        }
//                    },250);
//                }
                break;
            }
        }
        return true;
    }
    public File getAlbumStorageDir() {
        return new File(getTempDirectoryPath(), ".Vid.mp4");
    }

    private String getTempDirectoryPath() {
        File cache = null;

        // SD Card Mounted
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            cache = getContext().getExternalCacheDir();
        }
        // Use internal storage
        else {
            cache = getContext().getCacheDir();
        }

        // Create the cache directory if it doesn't exist
        cache.mkdirs();
        return cache.getAbsolutePath();
    }

    boolean onTouchFacing(final View view, MotionEvent motionEvent) {
        handleViewTouchFeedback(view, motionEvent);
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_UP: {
                coverView.setAlpha(0);
                coverView.setVisibility(VISIBLE);
                coverView.animate()
                        .alpha(1)
                        .setStartDelay(0)
                        .setDuration(300)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);

                                String package_name = mContext.getPackageName();
                                Resources resources = mContext.getResources();


                                if (cameraView.getFacing() == Facing.FRONT) {
                                    cameraView.setFacing(Facing.BACK);
                                    changeViewImageResource((ImageView) view, resources.getIdentifier("ic_facing_front", "drawable", package_name));

                                } else {
                                    cameraView.setFacing(Facing.FRONT);
                                    changeViewImageResource((ImageView) view, resources.getIdentifier("ic_facing_back", "drawable", package_name));

                                }

                                coverView.animate()
                                        .alpha(0)
                                        .setStartDelay(200)
                                        .setDuration(300)
                                        .setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                super.onAnimationEnd(animation);
                                                coverView.setVisibility(GONE);
                                            }
                                        })
                                        .start();
                            }
                        })
                        .start();

                break;
            }
        }
        return true;
    }
    boolean onTouchFlash(View view, MotionEvent motionEvent) {
        handleViewTouchFeedback(view, motionEvent);
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_UP: {
                String package_name = mContext.getPackageName();
                Resources resources = mContext.getResources();

                if (cameraView.getFlash() == Flash.OFF) {
                    cameraView.setFlash( Flash.ON );
                    changeViewImageResource((ImageView) view, resources.getIdentifier("ic_flash_on", "drawable", package_name));
                } else {
                    cameraView.setFlash(Flash.OFF);
                    changeViewImageResource((ImageView) view, resources.getIdentifier("ic_flash_off", "drawable", package_name));
                }

                break;
            }
        }
        return true;
    }

    boolean handleViewTouchFeedback(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                touchDownAnimation(view);
                return true;
            }

            case MotionEvent.ACTION_UP: {
                touchUpAnimation(view);
                return true;
            }

            default: {
                return true;
            }
        }
    }

    void touchDownAnimation(View view) {
        view.animate()
                .scaleX(0.88f)
                .scaleY(0.88f)
                .setDuration(300)
                .setInterpolator(new OvershootInterpolator())
                .start();
    }

    void touchUpAnimation(View view) {
        view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(new OvershootInterpolator())
                .start();
    }

    void changeViewImageResource(final ImageView imageView, @DrawableRes final int resId) {
        imageView.setRotation(0);
        imageView.animate()
                .rotationBy(360)
                .setDuration(400)
                .setInterpolator(new OvershootInterpolator())
                .start();

        imageView.postDelayed(new Runnable() {
            @Override
            public void run() {
                imageView.setImageResource(resId);
            }
        }, 120);
    }

}

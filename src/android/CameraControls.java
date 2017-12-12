package org.apache.cordova.camera;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.Facing;
import com.otaliastudios.cameraview.Flash;
import com.otaliastudios.cameraview.Gesture;
import com.otaliastudios.cameraview.GestureAction;

import java.io.File;
import java.lang.reflect.Field;


public class CameraControls extends LinearLayout {

    private final Context mContext;
    private final ImageView captureButton;
    private int cameraViewId = -1;
    private CameraView cameraView;

    private int coverViewId = -1;
    private View coverView;


    ImageView facingButton;


    ImageView flashButton;

    private long captureDownTime;
    private long captureStartTime;
    private boolean pendingVideoCapture;
    private boolean capturingVideo;
    public Uri video_path;

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

        captureButton = (ImageView) view.findViewById(resources.getIdentifier("captureButton", "id", package_name));
        captureButton.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return onTouchCapture(v,event);
            }
        });
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
                cameraView.mapGesture(Gesture.PINCH, GestureAction.ZOOM); // Pinch to zoom!
                cameraView.mapGesture(Gesture.TAP, GestureAction.FOCUS_WITH_MARKER); // Tap to focus!
                cameraView.mapGesture(Gesture.LONG_TAP, GestureAction.CAPTURE); // Long tap to shoot!
                setFacingImageBasedOnCamera();
                cameraView.addCameraListener(new CameraListener() {
                    @Override
                    public void onPictureTaken(byte[] jpeg) {
                        imageCaptured(jpeg);
                    }

                    @Override
                    public void onVideoTaken(File video) {
                        // The File is the same you passed before.
                        // Now it holds a MP4 video.
                        videoCaptured(video);
                    }
                });

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

    private void setFacingImageBasedOnCamera() {
        String package_name = mContext.getPackageName();
        Resources resources = mContext.getResources();
        if (cameraView.getFacing() == Facing.BACK) {
            facingButton.setImageResource(resources.getIdentifier("ic_facing_back", "drawable", package_name));
        } else {
            facingButton.setImageResource(resources.getIdentifier("ic_facing_front", "drawable", package_name));
        }
    }

    public void imageCaptured(byte[] jpeg) {


        long callbackTime = System.currentTimeMillis();
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
        ResultHolder.dispose();
        ResultHolder.setImage(bitmap);
        ResultHolder.setNativeCaptureSize(cameraView.getPictureSize());
        ResultHolder.setTimeToCallback(callbackTime - captureStartTime);
        Intent intent = new Intent(getContext(), PreviewActivity.class);
        getContext().startActivity(intent);
    }

    public void videoCaptured(File videoFile) {

        if (videoFile != null) {
            ResultHolder.dispose();
            ResultHolder.setVideo(videoFile);
            ResultHolder.setNativeCaptureSize(cameraView.getPreviewSize());
            Intent intent = new Intent(getContext(), PreviewActivity.class);
            getContext().startActivity(intent);
        }
    }


    boolean onTouchCapture(View view, MotionEvent motionEvent) {
        handleViewTouchFeedback(view, motionEvent);
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                captureDownTime = System.currentTimeMillis();
                pendingVideoCapture = true;
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (pendingVideoCapture) {
                            capturingVideo = true;
                            cameraView.startCapturingVideo(new File(video_path.getPath().replace(".jpg", ".mp4")),15000L);
//                            cameraView.captureVideo(new File(video_path.getPath().replace(".jpg", ".mp4")), new CameraKitEventCallback<CameraKitVideo>() {
//                                @Override
//                                public void callback(CameraKitVideo cameraKitVideo) {
//                                    videoCaptured(cameraKitVideo);
//                                }
//                            });
                        }
                    }
                }, 250);
                break;
            }

            case MotionEvent.ACTION_UP: {
                pendingVideoCapture = false;

                if (capturingVideo) {
                    capturingVideo = false;
//                    cameraView.stopVideo();
                    cameraView.stopCapturingVideo();

                } else {
                    captureStartTime = System.currentTimeMillis();
                    cameraView.capturePicture();
//                    cameraView.captureImage(new CameraKitEventCallback<CameraKitImage>() {
//                        @Override
//                        public void callback(CameraKitImage event) {
//                            imageCaptured(event);
//                        }
//                    });
                }
                break;
            }
        }
        return true;
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
                    cameraView.setFlash( Flash.OFF);
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

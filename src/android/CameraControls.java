package org.apache.cordova.camera;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
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

import com.wonderkiln.camerakit.CameraKit;
import com.wonderkiln.camerakit.CameraKitEventCallback;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;
import com.wonderkiln.camerakit.OnCameraKitEvent;

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

    @SuppressLint("ClickableViewAccessibility")
    public CameraControls(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        String package_name = context.getPackageName();
        Resources resources = context.getResources();

        View view = LayoutInflater.from(getContext()).inflate(resources.getIdentifier("camera_controls", "layout", package_name), this);


        if (attrs != null) {


            TypedArray a = resources.obtainAttributes(attrs, getResourceDeclareStyleableIntArray(context,"CameraControls"));

            try {
                cameraViewId = a.getResourceId(resources.getIdentifier("CameraControls_camera", "styleable", package_name), -1);
                coverViewId = a.getResourceId(resources.getIdentifier("CameraControls_cover", "styleable", package_name), -1);
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
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (cameraViewId != -1) {
            View view = getRootView().findViewById(cameraViewId);
            if (view instanceof CameraView) {
                cameraView = (CameraView) view;
                cameraView.bindCameraKitListener(this);
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

    private void setFacingImageBasedOnCamera() {
        String package_name = mContext.getPackageName();
        Resources resources = mContext.getResources();
        if (cameraView.isFacingFront()) {
            facingButton.setImageResource(resources.getIdentifier("ic_facing_back", "drawable", package_name));
        } else {
            facingButton.setImageResource(resources.getIdentifier("ic_facing_front", "drawable", package_name));
        }
    }

    @OnCameraKitEvent(CameraKitImage.class)
    public void imageCaptured(CameraKitImage image) {
        byte[] jpeg = image.getJpeg();

        long callbackTime = System.currentTimeMillis();
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
        ResultHolder.dispose();
        ResultHolder.setImage(bitmap);
        ResultHolder.setNativeCaptureSize(cameraView.getCaptureSize());
        ResultHolder.setTimeToCallback(callbackTime - captureStartTime);
        Intent intent = new Intent(getContext(), PreviewActivity.class);
        getContext().startActivity(intent);
    }

    @OnCameraKitEvent(CameraKitVideo.class)
    public void videoCaptured(CameraKitVideo video) {
        File videoFile = video.getVideoFile();
        if (videoFile != null) {
            ResultHolder.dispose();
            ResultHolder.setVideo(videoFile);
            ResultHolder.setNativeCaptureSize(cameraView.getCaptureSize());
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
                            cameraView.captureVideo(new File(video_path.getPath().replace(".jpg", ".mp4")), new CameraKitEventCallback<CameraKitVideo>() {
                                @Override
                                public void callback(CameraKitVideo cameraKitVideo) {
                                    videoCaptured(cameraKitVideo);
                                }
                            });
                        }
                    }
                }, 250);
                break;
            }

            case MotionEvent.ACTION_UP: {
                pendingVideoCapture = false;

                if (capturingVideo) {
                    capturingVideo = false;
                    cameraView.stopVideo();

                } else {
                    captureStartTime = System.currentTimeMillis();
                    cameraView.captureImage(new CameraKitEventCallback<CameraKitImage>() {
                        @Override
                        public void callback(CameraKitImage event) {
                            imageCaptured(event);
                        }
                    });
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


                                if (cameraView.isFacingFront()) {
                                    cameraView.setFacing(CameraKit.Constants.FACING_BACK);
                                    changeViewImageResource((ImageView) view, resources.getIdentifier("ic_facing_front", "drawable", package_name));
                                } else {
                                    cameraView.setFacing(CameraKit.Constants.FACING_FRONT);
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

                if (cameraView.getFlash() == CameraKit.Constants.FLASH_OFF) {
                    cameraView.setFlash(CameraKit.Constants.FLASH_ON);
                    changeViewImageResource((ImageView) view, resources.getIdentifier("ic_flash_on", "drawable", package_name));
                } else {
                    cameraView.setFlash(CameraKit.Constants.FLASH_OFF);
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

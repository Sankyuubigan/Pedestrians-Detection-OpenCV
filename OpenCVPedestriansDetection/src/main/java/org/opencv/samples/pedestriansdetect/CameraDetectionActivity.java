package org.opencv.samples.pedestriansdetect;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.HOGDescriptor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class CameraDetectionActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "myQ";
    private static final Scalar RECT_COLOR = new Scalar(0, 255, 0, 255);
    public static final int JAVA_DETECTOR = 0;
    public static final int NATIVE_DETECTOR = 1;
//    public static final int HOG_DETECTOR = 2;

    private int minNeighbors = 2;
    private int flags = 2;
    private MenuItem mItem10;
    private MenuItem itemOptimize;
    private MenuItem itemLongAway;
    private MenuItem itemSettings;
    private MenuItem mItem20;
    private MenuItem mItemType;

    private double scaleFactor = 1.1;
    private Mat mRgba;
    private Mat mGray;
    private File mCascadeFile;
    private CascadeClassifier mJavaDetector;
    private DetectionBasedTracker mNativeDetector;

    private int mDetectorType = JAVA_DETECTOR;
    private String[] mDetectorName;

    private float mRelativeSize = 0.2f;
    private int mAbsoluteSize = 0;

    private CameraBridgeViewBase mOpenCvCameraView;

    private HOGDescriptor mHogDetector;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // Load native library after(!) OpenCV initialization
                    System.loadLibrary("detectionBasedTracker");

                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.hogcascade_pedestrians);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "haarcascade_fullbody.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();
//                        mHogDetector = new HOGDescriptor(mCascadeFile.getAbsolutePath());
//                        mHogDetector.setSVMDetector(HOGDescriptor.getDaimlerPeopleDetector());

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

                        mNativeDetector = new DetectionBasedTracker(mCascadeFile.getAbsolutePath(), 0);
                        Log.d("myQ", "crouli");
                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };
    private double max_x = 480;
    private double max_y = 480;
    private double min_x;
    private double min_y;
    private View linear_settings;
    private Button saveBtn;
    private EditText setting_flags;
    private EditText setting_max_y;
    private EditText setting_max_x;
    private EditText setting_min_x;
    private EditText setting_minNeighbors;
    private EditText setting_min_y;
    private EditText setting_scaleFactor;

    public CameraDetectionActivity() {
        mDetectorName = new String[3];
        mDetectorName[JAVA_DETECTOR] = "Java";
        mDetectorName[NATIVE_DETECTOR] = "Native (tracking)";
//        mDetectorName[HOG_DETECTOR] = "HOG";

        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.detect_surface_view);
        setting_flags = (EditText) findViewById(R.id.setting_flags);
        setting_max_x = (EditText) findViewById(R.id.setting_max_x);
        setting_max_y = (EditText) findViewById(R.id.setting_max_y);
        setting_min_x = (EditText) findViewById(R.id.setting_min_x);
        setting_min_y = (EditText) findViewById(R.id.setting_min_y);
        setting_minNeighbors = (EditText) findViewById(R.id.setting_minNeighbors);
        setting_scaleFactor = (EditText) findViewById(R.id.setting_scaleFactor);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        linear_settings = findViewById(R.id.linear_settings);
        saveBtn = (Button) findViewById(R.id.saveBtn);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                linear_settings.setVisibility(View.GONE);
                fillingArgumentsFromSettings();
            }
        });
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
//        mOpenCvCameraView.setMinimumHeight(720);
//        mOpenCvCameraView.setMinimumWidth(1280);
//        mOpenCvCameraView.setMaxFrameSize(1280, 720);
    }

    public void fillingSettingsFromArguments() {
        setting_flags.setText(String.valueOf(flags));
        setting_max_x.setText(String.valueOf(max_x));
        setting_max_y.setText(String.valueOf(max_y));
        setting_min_x.setText(String.valueOf(min_x));
        setting_min_y.setText(String.valueOf(min_y));
        setting_minNeighbors.setText(String.valueOf(minNeighbors));
        setting_scaleFactor.setText(String.valueOf(scaleFactor));
    }

    public void fillingArgumentsFromSettings() {
        flags = Integer.valueOf(setting_flags.getText().toString());
        minNeighbors = Integer.valueOf(setting_minNeighbors.getText().toString());
        max_x = Double.valueOf(setting_max_x.getText().toString());
        max_y = Double.valueOf(setting_max_y.getText().toString());
        min_x = Double.valueOf(setting_min_x.getText().toString());
        min_y = Double.valueOf(setting_min_y.getText().toString());
        scaleFactor = Double.valueOf(setting_scaleFactor.getText().toString());
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        setOptimizeSettings();
    }

    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        if (mAbsoluteSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeSize) > 0) {
                mAbsoluteSize = Math.round(height * mRelativeSize);
                min_x = mAbsoluteSize;
                min_y = mAbsoluteSize;
            }
            mNativeDetector.setMinSize(mAbsoluteSize);
        }

        MatOfRect locations = new MatOfRect();

//        if (mDetectorType == HOG_DETECTOR) {
//            MatOfDouble weights = new MatOfDouble();
//            mHogDetector.detectMultiScale(mGray, locations, weights);
//        } else
            if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector != null)
                mJavaDetector.detectMultiScale(mGray, locations, scaleFactor, minNeighbors, flags, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(min_x, min_y), new Size(max_x, max_y));
        } else if (mDetectorType == NATIVE_DETECTOR) {
            if (mNativeDetector != null)
                mNativeDetector.detect(mGray, locations);
        } else {
            Log.e(TAG, "Detection method is not selected!");
        }

        Rect[] locationsArray = locations.toArray();
        for (int i = 0; i < locationsArray.length; i++)
            Imgproc.rectangle(mRgba, locationsArray[i].tl(), locationsArray[i].br(), RECT_COLOR, 3);

//        Point fontPoint = new Point();
//        fontPoint.x = 15;
//        fontPoint.y = mRgba.height() - 20;
//        Imgproc.putText(mRgba,
//                " width:" + mRgba.width() + " height:" + mRgba.height(),
//                fontPoint, Core.FONT_HERSHEY_PLAIN, 1.5, new Scalar(255, 0, 0),
//                2, Core.LINE_AA, false);

        return mRgba;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        itemOptimize = menu.add("Сбросить параметры");
        mItem10 = menu.add("Вариант настроек 1");
        itemLongAway = menu.add("Вариант настроек 2");
        mItem20 = menu.add("Вариант настроек 3");
        itemSettings = menu.add("Настроить вручную");
        mItemType = menu.add(mDetectorName[mDetectorType]);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item == mItem10)
            setMinSize(0.1f);
        else if (item == itemLongAway)
            setLongAway();
        else if (item == itemSettings) {
            linear_settings.setVisibility(View.VISIBLE);
            fillingSettingsFromArguments();
        } else if (item == mItem20)
            setMinSize(0.2f);
        else if (item == itemOptimize)
            setOptimizeSettings();
        else if (item == mItemType) {
            int tmpDetectorType = (mDetectorType + 1) % mDetectorName.length;
            item.setTitle(mDetectorName[tmpDetectorType]);
            setDetectorType(tmpDetectorType);
        }
        return true;
    }

    private void setMinSize(float size) {
        mRelativeSize = size;
        mAbsoluteSize = 0;
        minNeighbors = 2;
        flags = 2;
        scaleFactor = 1.1;
        max_x = 480;
        max_y = 480;
    }

    private void setLongAway() {
        minNeighbors = 3;
        flags = 3;
        min_x = 5;
        min_y = 13;
        scaleFactor = 1.1;
        max_x = 45;
        mAbsoluteSize = 0;
        max_y = 80;
    }

    private void setOptimizeSettings() {
        mRelativeSize = 0.05f;
        mAbsoluteSize = 0;
        min_y = 50;
        min_x = 30;
        max_x = 170;
        max_y = 300;
        minNeighbors = 1;
        scaleFactor = 1.05;
        flags = 0;
    }

    private void setDetectorType(int type) {
        if (mDetectorType != type) {
            mDetectorType = type;

            if (type == NATIVE_DETECTOR) {
                Log.i(TAG, "Detection Based Tracker enabled");
                mNativeDetector.start();
            } else {
                Log.i(TAG, "Cascade detector enabled");
                mNativeDetector.stop();
            }
        }
    }
}

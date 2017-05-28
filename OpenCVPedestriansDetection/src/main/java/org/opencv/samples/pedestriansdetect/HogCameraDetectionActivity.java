package org.opencv.samples.pedestriansdetect;


import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.HOGDescriptor;

public class HogCameraDetectionActivity extends Activity implements CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";

    private Mat mRgba;
    private static final Scalar RECT_COLOR = new Scalar(0, 255, 0, 255); //Цвет прямоугольников
    private Handler handler;

    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
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
    // Определяем переменные, в которые будут помещены результаты поиска ( locations - прямоугольные
    // области, weights - вес (можно сказать релевантность) соответствующей локации)
    private MatOfRect locations = new MatOfRect();
    private MatOfDouble weights = new MatOfDouble();
    private boolean flag = true;
    private HOGDescriptor hog;

    public HogCameraDetectionActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_hog_camera_detection);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_blob_detection_activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        handler = new Handler();
        hog = new HOGDescriptor();
        //Получаем стандартный определитель людей и устанавливаем его нашему дескриптору
        MatOfFloat descriptors = HOGDescriptor.getDefaultPeopleDetector();
        hog.setSVMDetector(descriptors);
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
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }


    public void analysisPeopleFrame() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Mat mRgbaF = mRgba;
                Imgproc.cvtColor(mRgbaF, mRgbaF, Imgproc.COLOR_RGB2GRAY, 4);
                // Сам анализ фотографий. Результаты запишутся в locations и weights
                hog.detectMultiScale(mRgbaF, locations, weights);
                handler.post(new Runnable() {
                    public void run() {
                        flag = true;
                    }
                });
            }
        }).start();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        if (flag)
            analysisPeopleFrame();
        flag = false;
        // Если есть результат - добавляем на фотографию области и вес каждой из них
        if (locations != null && locations.rows() > 0) {
            Rect[] locationsArray = locations.toArray();
            for (int i = 0; i < locationsArray.length; i++)
                Imgproc.rectangle(mRgba, locationsArray[i].tl(), locationsArray[i].br(), RECT_COLOR, 3);
        }
        return mRgba;
    }
}

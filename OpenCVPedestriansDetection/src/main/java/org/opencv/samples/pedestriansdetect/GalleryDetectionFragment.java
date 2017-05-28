package org.opencv.samples.pedestriansdetect;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.HOGDescriptor;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

public class GalleryDetectionFragment extends Fragment {
    static {
        if (!OpenCVLoader.initDebug()) {
            Log.i("my", "OpenCV initialization failed");
        } else {
            Log.i("my", "OpenCV initialization succeeded");
        }
    }

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private ImageView imageView;
    private Uri selectedImage;
    private ProgressDialog pd;
    private Handler downloadHandler;
    private Runnable downloadCallback;
    private OnFragmentInteractionListener mListener;

    public GalleryDetectionFragment() {
        // Required empty public constructor
    }

    public static GalleryDetectionFragment newInstance(String param1, String param2) {
        GalleryDetectionFragment fragment = new GalleryDetectionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gallery_detection, container, false);
        imageView = (ImageView) view.findViewById(R.id.imageView);
        final Button button = (Button) view.findViewById(R.id.button);
        final Button button2 = (Button) view.findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedImage != null) {
//                    pd = new ProgressDialog(MainActivity.this);
//                    pd.setTitle("Title");
//                    pd.setMessage("Message");
//                    // меняем стиль на индикатор
//                    pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//                    // устанавливаем максимум
//                    pd.setMax(2148);
//                    // включаем анимацию ожидания
//                    pd.setIndeterminate(true);
//                    pd.show();
//                    h = new Handler() {
//                        public void handleMessage(NotificationCompat.MessagingStyle.Message msg) {
//                            // выключаем анимацию ожидания
//                            pd.setIndeterminate(false);
//                            if (pd.getProgress() < pd.getMax()) {
//                                // увеличиваем значения индикаторов
//                                pd.incrementProgressBy(50);
//                                pd.incrementSecondaryProgressBy(75);
//                                h.sendEmptyMessageDelayed(0, 100);
//                            } else {
//                                pd.dismiss();
//                            }
//                        }
//                    };
//                    h.sendEmptyMessageDelayed(0, 2000);
                    pd = new ProgressDialog(getActivity());
                    pd.setTitle("Обработка изображения");
                    pd.setMessage("Пожалуйста, подождите...");
                    pd.show();
                    Thread downloadThread = new Thread(new GalleryDetectionFragment.DownloadVerseThread());
                    downloadHandler = new Handler();
                    downloadCallback = new Runnable() {
                        public void run() {
                            pd.dismiss();
                        }
                    };
                    downloadThread.start();
//                    Handler handler = new Handler();
//                    Thread mThread2 = new Thread() {
//                        @Override
//                        public void run() {
//                            try {
//                                imageView.setImageBitmap(peopleDetect(getContentResolver().openInputStream(selectedImage)));
//                            } catch (FileNotFoundException e) {
//                                e.printStackTrace();
//                                Log.e("my", "error set image: " + e.getMessage());
//                            }
////                            pd.dismiss();
//                        }
//                    };
//                    handler.post(mThread2);
                }
            }
        });
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFullImage(imageView);
            }
        });
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog();
            }
        });
//        imageView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
//            @Override
//            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
//                                       int oldTop, int oldRight, int oldBottom) {
//                if (pd != null)
//                    pd.dismiss();
//            }
//        });
        return view;
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }
//
//    @Override
//    public void onAttach(Context context) {
//        super.onAttach(context);
//        if (context instanceof OnFragmentInteractionListener) {
//            mListener = (OnFragmentInteractionListener) context;
//        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
//    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Выберете способ загрузки фото:")
                .setCancelable(true)
                .setPositiveButton("Камера", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(takePicture, 0);
                    }
                })
                .setNegativeButton("Галерея", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent pickPhoto = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(pickPhoto, 1);
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }


    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        selectedImage = imageReturnedIntent.getData();
        imageView.setImageURI(selectedImage);
    }

    public void showFullImage(View view) {
//        String path = selectedImage.getPath();
        if (selectedImage != null) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
//            Uri imgUri = Uri.parse("file://" + path);
            intent.setDataAndType(selectedImage, "image/*");
            startActivity(intent);
        } else
            Toast.makeText(getActivity(), "Загрузите изображение для просмотра на весь экран", Toast.LENGTH_SHORT).show();
    }

    private class DownloadVerseThread implements Runnable {
        @Override
        public void run() {
            Bitmap bitmap = null;
            try {
                bitmap = peopleDetect(getActivity().getContentResolver().openInputStream(selectedImage));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.e("my", "error set image: " + e.getMessage());
            }
            final Bitmap finalBitmap = bitmap;
            downloadHandler.post(new Runnable() {
                @Override
                public void run() {
                    imageView.setImageBitmap(finalBitmap);
                }
            });
            downloadHandler.post(downloadCallback);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        //Вызываем асинхронный загрузчик библиотеки
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, getActivity(), mLoaderCallback);
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(getActivity()) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    //Мы готовы использовать OpenCV
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public Bitmap peopleDetect(InputStream input) {
        Bitmap bitmap = null;
        float execTime;
        // Закачиваем фотографию
//            URL url = new URL(path);
//            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//            connection.setDoInput(true);
//            connection.connect();
//            InputStream input = connection.getInputStream();
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        bitmap = BitmapFactory.decodeStream(input, null, opts);
        long time = System.currentTimeMillis();
        // Создаем матрицу изображения для OpenCV и помещаем в нее нашу фотографию
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);
        // Переконвертируем матрицу с RGB на градацию серого
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY, 4);
        HOGDescriptor hog = new HOGDescriptor();
        //Получаем стандартный определитель людей и устанавливаем его нашему дескриптору
        MatOfFloat descriptors = HOGDescriptor.getDefaultPeopleDetector();
        hog.setSVMDetector(descriptors);
        // Определяем переменные, в которые будут помещены результаты поиска ( locations - прямоугольные области, weights - вес (можно сказать релевантность) соответствующей локации)
        MatOfRect locations = new MatOfRect();
        MatOfDouble weights = new MatOfDouble();
        // Собственно говоря, сам анализ фотографий. Результаты запишутся в locations и weights
        hog.detectMultiScale(mat, locations, weights);
        execTime = ((float) (System.currentTimeMillis() - time)) / 1000f;
        //Переменные для выделения областей на фотографии
        Point rectPoint1 = new Point();
        Point rectPoint2 = new Point();
        Scalar fontColor = new Scalar(0, 0, 0);//Цвет прямоугольников
        Point fontPoint = new Point();
        // Если есть результат - добавляем на фотографию области и вес каждой из них
        if (locations.rows() > 0) {
            List<Rect> rectangles = locations.toList();
            int i = 0;
            List<Double> weightList = weights.toList();
            for (Rect rect : rectangles) {
                float weigh = weightList.get(i++).floatValue();
                rectPoint1.x = rect.x;
                rectPoint1.y = rect.y;
                fontPoint.x = rect.x;
                fontPoint.y = rect.y - 4;
                rectPoint2.x = rect.x + rect.width;
                rectPoint2.y = rect.y + rect.height;
                final Scalar rectColor = new Scalar(0, 0, 0);
                // Добавляем на изображения найденную информацию
                Imgproc.rectangle(mat, rectPoint1, rectPoint2, rectColor, 2);
                Imgproc.putText(mat,
                        String.format("%1.2f", weigh),
                        fontPoint, Core.FONT_HERSHEY_PLAIN, 1.5, fontColor,
                        2, Core.LINE_AA, false);
            }
        }
        fontPoint.x = 15;
        fontPoint.y = bitmap.getHeight() - 20;
        // Добавляем дополнительную отладочную информацию
        Imgproc.putText(mat,
                "Processing time:" + execTime + " width:" + bitmap.getWidth() + " height:" + bitmap.getHeight(),
                fontPoint, Core.FONT_HERSHEY_PLAIN, 1.5, fontColor,
                2, Core.LINE_AA, false);
        Utils.matToBitmap(mat, bitmap);
        return bitmap;
    }
}

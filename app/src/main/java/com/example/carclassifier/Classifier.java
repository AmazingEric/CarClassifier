package com.example.carclassifier;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.widget.Toast;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class Classifier {

    private static final String TAG = "TfLiteCameraDemo";

    private static final String MODEL_PATH = "newModel.tflite";
    private static final String LABEL_PATH = "class_labels.txt";

    private static final int RESULTS_TO_SHOW = 1;
    private static final int DIM_BATCH_SIZE = 1;
    private static final int DIM_PIXEL_SIZE = 3;

    static final int DIM_IMG_SIZE_X = 224;
    static final int DIM_IMG_SIZE_Y = 224;

    private static final float IMAGE_MEAN = 127.5f;
    private static final float IMAGE_STD = 127.5f;

    private int[] intValues = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];

    private Interpreter tflite;
    private List<String> labelList;
    private ByteBuffer imgData = null;
    private float[][] labelProbArray = null;
    private String textToShow;
    public Integer classNumber;

    private PriorityQueue<Map.Entry<String, Float>> sortedLabels =
            new PriorityQueue<>(
                    RESULTS_TO_SHOW,
                    new Comparator<Map.Entry<String, Float>>() {
                        @Override
                        public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
                            return (o1.getValue()).compareTo(o2.getValue());
                        }
                    });

    Classifier(Activity activity) throws IOException {
        tflite = new Interpreter(loadModelFile(activity));
        labelList = loadLabelList(activity);
        imgData =
                ByteBuffer.allocateDirect(
                        DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE * 4);
        imgData.order(ByteOrder.nativeOrder());
        labelProbArray = new float[1][labelList.size()];
    }

    String classifyFrame(Bitmap bitmap) {
        if (tflite == null) {
            return "Uninitialized Classifier.";
        }
        bitmap = Bitmap.createScaledBitmap(bitmap, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, false);
        convertBitmapToByteBuffer(bitmap);
        tflite.run(imgData, labelProbArray);
        textToShow = printTopKLabels();
        return textToShow;
    }

    public void close() {
        tflite.close();
        tflite = null;
    }

    private List<String> loadLabelList(Activity activity) throws IOException {
        List<String> labelList = new ArrayList<String>();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(activity.getAssets().open(LABEL_PATH)));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (imgData == null) {
            return;
        }
        imgData.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                final int val = intValues[pixel++];
                //imgData.put((byte) ((val >> 16) & 0xFF));
                //imgData.put((byte) ((val >> 8) & 0xFF));
                //imgData.put((byte) (val & 0xFF));
                //imgData.putFloat((((val >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                //imgData.putFloat((((val >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                //imgData.putFloat(((val & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                imgData.putFloat(((val>>16) & 0xFF)/255.0f);
                imgData.putFloat(((val>>8) & 0xFF)/255.0f);
                imgData.putFloat((val & 0xFF)/255.0f);
            }
        }
    }

    private String printTopKLabels() {
        for (int i = 0; i < labelList.size(); ++i) {
            sortedLabels.add(
                    new AbstractMap.SimpleEntry<>(labelList.get(i), labelProbArray[0][i] ));
            if (sortedLabels.size() > RESULTS_TO_SHOW) {
                sortedLabels.poll();
            }
        }
        String textToShow = "";
        final int size = sortedLabels.size();
        for (int i = 0; i < size; ++i) {
            Map.Entry<String, Float> label = sortedLabels.poll();
            textToShow = "\n" + label.getKey() + ":" + Float.toString(label.getValue()) + textToShow;
            if (i == size-1) {
                for (int j = 0; j < labelList.size(); ++j) {
                    if (label.getKey() == labelList.get(j)) {
                        classNumber = j + 1;
                        break;
                    }
                }
            }
        }
        return textToShow;
    }
}

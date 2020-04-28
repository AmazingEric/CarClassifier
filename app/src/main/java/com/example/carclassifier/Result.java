package com.example.carclassifier;

import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;

import androidx.appcompat.app.AppCompatActivity;

public class Result extends AppCompatActivity {

    private static final String LABEL_PATH = "class_labels.txt";
    private static final String IMG_PATH = "imgs/";

    private static final int INPUT_SIZE = 224;

    private Integer classNumber;

    private ImageView imageView;
    private TextView textView;
    private Button button_home;
    private Button button_recommend;

    private Bitmap bitmap;
    private Classifier classifier = null;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.result);


        try{
            classifier = new Classifier(Result.this);
        }catch(IOException e){ }

        imageView = (ImageView)findViewById(R.id.imageView);
        textView = (TextView)findViewById(R.id.textView);
        button_home = (Button)findViewById(R.id.button_home);
        button_recommend = (Button)findViewById(R.id.button_recommend);

        Intent intent = getIntent();
        byte[] bitmapbytes = intent.getByteArrayExtra("bitmapbytes");
        bitmap = BitmapFactory.decodeByteArray(bitmapbytes, 0, bitmapbytes.length);
        //imageView.setImageBitmap(bitmap);
        bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);
        String text = classifier.classifyFrame(bitmap);
        textView.setText(text);
        classNumber = classifier.classNumber;
        String imgPath = IMG_PATH + Integer.toString(classNumber) + ".jpg";
        AssetManager assetManager = this.getApplicationContext().getAssets();
        InputStream inputStream = null;
        try {
            inputStream = assetManager.open(imgPath);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            imageView.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }

        button_home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        button_recommend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Result.this, Recommend.class);
                intent.putExtra("Class_Number", classNumber);
                startActivity(intent);
            }
        });

        }


}

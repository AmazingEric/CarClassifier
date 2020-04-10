package com.example.carclassifier;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import androidx.appcompat.app.AppCompatActivity;

public class Recommend extends AppCompatActivity {

    private static final int INPUT_SIZE = 224;

    private ImageView imageView;
    private TextView textView;
    private Button button_home_recommend;

    private Bitmap bitmap;
    private Classifier classifier = null;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recommend);


        try{
            classifier = new Classifier(Recommend.this);
        }catch(IOException e){ }

        imageView = (ImageView)findViewById(R.id.imageView);
        textView = (TextView)findViewById(R.id.textView);
        button_home_recommend = (Button)findViewById(R.id.button_home_recommend);

        Intent intent = getIntent();
        byte[] bitmapbytes = intent.getByteArrayExtra("bitmapbytes");
        bitmap = BitmapFactory.decodeByteArray(bitmapbytes, 0, bitmapbytes.length);
        imageView.setImageBitmap(bitmap);
        bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);
        String text = classifier.classifyFrame(bitmap);
        textView.setText(text);

        button_home_recommend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        }

}

package com.example.carclassifier;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import android.database.sqlite.SQLiteDatabase;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

public class Recommend extends AppCompatActivity {

    private static final String LABEL_PATH = "class_labels.txt";
    private static final String IMG_PATH = "imgs/";

    private InputStream inputStream;
    private List<String> labelList;

    private Integer class_number;
    private Integer FIRST_SIMILAR_CAR;
    private Integer SECOND_SIMILAR_CAR;

    private TextView textView1;
    private TextView textView2;
    private TextView textView3;
    private ImageView imageView1;
    private ImageView imageView2;
    private ImageView imageView3;
    private Button button_back;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recommend);

        textView1 = (TextView)findViewById(R.id.textView1);
        textView2 = (TextView)findViewById(R.id.textView2);
        textView3 = (TextView)findViewById(R.id.textView3);
        imageView1 = (ImageView) findViewById(R.id.imageView1);
        imageView2 = (ImageView) findViewById(R.id.imageView2);
        imageView3 = (ImageView) findViewById(R.id.imageView3);
        button_back = (Button) findViewById(R.id.button_back);

        Intent intent = getIntent();
        class_number = intent.getIntExtra("Class_Number", 0);

        try {
            labelList = loadLabelList(Recommend.this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        SQLiteDatabase DB = openDatabase("similar_cars.db");

        String selectSql = "select FIRST_SIMILAR_CAR, SECOND_SIMILAR_CAR from SIMILAR_CARS where MAIN_ID == " + Integer.toString(class_number);
        Cursor cursor = DB.rawQuery(selectSql, null);
        cursor.moveToNext();
        FIRST_SIMILAR_CAR = cursor.getInt(0);
        SECOND_SIMILAR_CAR = cursor.getInt(1);
        DB.close();

        AssetManager assetManager = this.getApplicationContext().getAssets();
        try {
            inputStream = assetManager.open(IMG_PATH + Integer.toString(class_number) + ".jpg");
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            imageView1.setImageBitmap(bitmap);
            textView1.setText(labelList.get(class_number-1));

            inputStream = assetManager.open(IMG_PATH + Integer.toString(FIRST_SIMILAR_CAR) + ".jpg");
            bitmap = BitmapFactory.decodeStream(inputStream);
            imageView2.setImageBitmap(bitmap);
            textView2.setText(labelList.get(FIRST_SIMILAR_CAR-1));

            inputStream = assetManager.open(IMG_PATH + Integer.toString(SECOND_SIMILAR_CAR) + ".jpg");
            bitmap = BitmapFactory.decodeStream(inputStream);
            imageView3.setImageBitmap(bitmap);
            textView3.setText(labelList.get(SECOND_SIMILAR_CAR-1));
        } catch (IOException e) {
            e.printStackTrace();
        }

        button_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

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

    private SQLiteDatabase openDatabase(String database)
    {
        String DATABASE_PATH = android.os.Environment
                .getExternalStorageDirectory().getAbsolutePath();

        try
        {
            // 获得dictionary.db文件的绝对路径
            String databaseFilename = DATABASE_PATH + "/" + database;
            File dir = new File(DATABASE_PATH);
            // 如果/sdcard/dictionary目录中存在，创建这个目录
            if (!dir.exists())
                dir.mkdir();
            // 如果在/sdcard/dictionary目录中不存在
            // dictionary.db文件，则从res\raw目录中复制这个文件到
            // SD卡的目录（/sdcard/dictionary）
            if (!(new File(databaseFilename)).exists())
            {
                // 获得封装dictionary.db文件的InputStream对象
                InputStream is = null;
                AssetManager assetManager = this.getApplicationContext().getAssets();
                try {
                    is = assetManager.open("similar_cars.db");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                FileOutputStream fos = new FileOutputStream(databaseFilename);
                byte[] buffer = new byte[8192];
                int count = 0;
                // 开始复制dictionary.db文件
                while ((count = is.read(buffer)) > 0)
                {
                    fos.write(buffer, 0, count);
                }
                //关闭文件流
                fos.close();
                is.close();
            }
            // 打开/sdcard/dictionary目录中的dictionary.db文件
            SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(
                    databaseFilename, null);
            return db;
        }
        catch (Exception e)
        {
        }
        //如果打开出错，则返回null
        return null;
    }
}

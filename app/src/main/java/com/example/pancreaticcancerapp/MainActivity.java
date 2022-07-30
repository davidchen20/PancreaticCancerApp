package com.example.pancreaticcancerapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.example.pancreaticcancerapp.ml.Model;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {

    Button button;
    TextView results;
    ImageView imageView;
    int imageSize = 224;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = findViewById(R.id.camera);
        results = findViewById(R.id.result);
        imageView = findViewById(R.id.image);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "button pressed", Toast.LENGTH_SHORT).show();

                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, 1);
                } else {
                    requestPermissions(new String[] {Manifest.permission.CAMERA}, 100);
                }
            }
        });

    }

    public void classifyImage(Bitmap image) {
//        image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
        try {
            Model model = Model.newInstance(getApplicationContext());

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 549, 727, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * 549 * 727 * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[imageSize * imageSize];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
//            int pixel = 0;

//            for(int i = 0; i < imageSize; i++){
//                for(int j = 0; j < imageSize; j++){
//                    int val = intValues[pixel++];
//                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255f));
//                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255f));
//                    byteBuffer.putFloat((val & 0xFF) * (1.f / 255f));
//                }
//
//            }
            for(int i = 0; i < intValues.length; i++){
                int val = intValues[i];
                byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 1));
                byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 1));
                byteBuffer.putFloat((val & 0xFF) * (1.f / 1));

            }



            inputFeature0.loadBuffer(byteBuffer);
//
//
////             Runs model inference and gets result.
            Model.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
//
            float[] confidences = outputFeature0.getFloatArray();
            int maxPos = 0;
            for(int i = 0; i < confidences.length; i++)
                if(confidences[i] > confidences[maxPos])
                    maxPos = i;
//
//
//
//
            String[] classes = {"Cancerous", "Noncancerous"};
            results.setText("It is " + classes[maxPos]);


////             Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == 1 && resultCode == RESULT_OK){
            //then we know we're returning from the Camera Activity

            Bitmap image = (Bitmap) data.getExtras().get("data");
            int dimension = Math.min(image.getWidth(), image.getHeight());
            image = ThumbnailUtils.extractThumbnail(image, dimension, dimension); //resizing image as a square

            imageView.setImageBitmap(image);
            classifyImage(image);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
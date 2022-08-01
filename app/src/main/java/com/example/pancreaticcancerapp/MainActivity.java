package com.example.pancreaticcancerapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.example.pancreaticcancerapp.ml.Model;


public class MainActivity extends AppCompatActivity {

    Button gallery;
    ImageView imageView;
    TextView result, confidence;

    //// Use for teachable machine model
    int imageHeight = 224;
    int imageWidth = 224;


    // Use for my model
//    int imageHeight = 727;
//    int imageWidth = 549;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gallery = findViewById(R.id.cameraRoll);
        confidence = findViewById(R.id.confidence);
        result = findViewById(R.id.result);
        imageView = findViewById(R.id.imageView);



//      initiates use of camera roll
        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent cameraIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(cameraIntent, 1);
            }
        });
    }

    public void classifyImage(Bitmap image){
        image = Bitmap.createScaledBitmap(image, imageWidth, imageHeight, false);
        try {
            Model model = Model.newInstance(getApplicationContext());

            // Creates inputs for reference.
            //549 727 for my model, 224 224 for teachable machine
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, imageWidth, imageHeight, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageWidth * imageHeight * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[imageWidth * imageHeight];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());



            //iterate over each pixel and extract R, G, and B values. Add those values individually to the byte buffer.
            //Use / 255.f for Teachable Machine. Use / 1 for my model
            for(int i = 0; i < intValues.length; i++){
                int val = intValues[i];
                byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
                byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
                byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));

            }
            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            Model.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidences = outputFeature0.getFloatArray();

            // find the index of the class with the biggest confidence.


            int maxPos = 0;

//            iterates through confidences array, finding which index has the highest confidence
            for(int i = 0; i < confidences.length; i++)
                if(confidences[i] > confidences[maxPos])
                    maxPos = i;

            String[] classes = {"Cancerous", "Noncancerous"};
            result.setText(classes[maxPos]);


//          prints the confidences
            String s = "";
            for(int i = 0; i < classes.length; i++){
                s += String.format("%s: %.1f%%\n", classes[i], confidences[i] * 100);
            }

            confidence.setText(s);

            // closes model
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == 1 && resultCode == RESULT_OK){
            Uri dat = data.getData();
            Bitmap image = null;
            try {
                image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), dat);
            } catch (IOException e) {
                e.printStackTrace();
            }
            imageView.setImageBitmap(image);

            image = Bitmap.createScaledBitmap(image, imageWidth, imageHeight, false);
            classifyImage(image);

        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
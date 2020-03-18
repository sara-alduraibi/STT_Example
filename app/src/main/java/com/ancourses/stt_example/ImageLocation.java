package com.ancourses.stt_example;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import uk.co.senab.photoview.PhotoViewAttacher;


public class ImageLocation extends Activity {
    ImageView iv_llocation;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.location);
        iv_llocation = findViewById(R.id.iv_llocation);
        Intent intent = getIntent();
        Bundle b = intent.getExtras();
        String imageURL = (String) b.get("images");
        Glide.with(ImageLocation.this).load(imageURL).into(iv_llocation);
        PhotoViewAttacher pAttacher;
        pAttacher = new PhotoViewAttacher(iv_llocation);
        pAttacher.update();
    }


  //  Glide.with(ImageLocation.this).load(imageUrl).into(ivLocation);


}

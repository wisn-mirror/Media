package com.wisn.medial.imagelist;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.FrameLayout;

import com.wisn.medial.R;

public class ImageListActivity extends AppCompatActivity {

    private FrameLayout fl_content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_list);
        fl_content = findViewById(R.id.fl_content);
        Fragment imageListFragment = new ImageListFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.fl_content, imageListFragment).commitAllowingStateLoss();

    }
}

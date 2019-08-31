package com.aditya.remembermythings.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.aditya.remembermythings.Common.Common;
import com.aditya.remembermythings.R;

public class FirstStartActivity extends AppCompatActivity {

    String uPhone;
    AppCompatButton btnGetStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_start);


        uPhone = Common.currentUser.getuPhone();

        if (getIntent().hasExtra("uPhone")){
            uPhone = getIntent().getStringExtra("uPhone");
        }

        btnGetStarted = findViewById(R.id.btn_start);
        btnGetStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent homeIntent = new Intent(getApplicationContext(),MainActivity.class);
                homeIntent.putExtra("uPhone",uPhone);
                startActivity(homeIntent);
                finish();
            }
        });

    }


}


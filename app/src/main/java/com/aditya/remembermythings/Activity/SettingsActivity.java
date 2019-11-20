package com.aditya.remembermythings.Activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.aditya.remembermythings.Common.Common;
import com.aditya.remembermythings.Model.User;
import com.aditya.remembermythings.R;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";



    @BindView(R.id.txtOldPass) EditText oldPassword;
    @BindView(R.id.txtNewPass) EditText newPassword;
    @BindView(R.id.txtConfirmPass) EditText confirmPassword;
    @BindView(R.id.btn_changePassword) Button changePassword;
    @BindView(R.id.changePassGrid) GridLayout changePassGrid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);

        //Banner Ads
        AdView mAdView = findViewById(R.id.adViewSettings);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        changePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePassword();
            }
        });

    }

    private void changePassword() {
        Log.d(TAG, "Change Password");

        if (!validate()) {
            //Toast.makeText(this, "Cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (Common.isConnectedToInternet(getBaseContext())) {

            //Init Firebase
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            final DatabaseReference table_user = database.getReference("Users");

            table_user.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    //check if user not exist in database
                    if (dataSnapshot.child(Common.currentUser.getuPhone()).exists()) {
                        User user = dataSnapshot.child(Common.currentUser.getuPhone()).getValue(User.class);
                        //user.setuPhone(phone); //set phone

                        if (user.getuPassword().equals(oldPassword.getText().toString())) {
                            table_user.child(Common.currentUser.getuPhone()).child("uPassword").setValue(newPassword.getText().toString());
                            oldPassword.setText(""); newPassword.setText(""); confirmPassword.setText("");
                            Toast.makeText(SettingsActivity.this, "Password Updated Successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(SettingsActivity.this, "Old Password Does'nt Match !!!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }


    }

    public boolean validate() {
        boolean valid = true;

        String oldPass = oldPassword.getText().toString();
        String newPass = newPassword.getText().toString();
        String confirmPass = confirmPassword.getText().toString();

        if (oldPass.isEmpty() ) {
            oldPassword.setError("Cannot be Empty");
            valid = false;
        } else {
            oldPassword.setError(null);
        }

        if (newPass.isEmpty() || newPass.length() < 4 || newPass.length() > 10) {
            newPassword.setError("between 4 and 10 alphanumeric characters");
            valid = false;
        } else {
            newPassword.setError(null);
        }

        if (!newPass.equals(confirmPass)) {
            confirmPassword.setError("Password Does'nt match");
            valid = false;
        } else {
            confirmPassword.setError(null);
        }
        return valid;
    }
}

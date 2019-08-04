package com.aditya.remembermythings.Activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatCheckBox;

import com.aditya.remembermythings.Common.Common;
import com.aditya.remembermythings.Model.User;
import com.aditya.remembermythings.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.paperdb.Paper;


public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final int REQUEST_SIGNUP = 0;

    @BindView(R.id.input_phone) EditText inputPhone;
    @BindView(R.id.input_password) EditText inputPassword;
    @BindView(R.id.btn_login) AppCompatButton btnLogin;
    @BindView(R.id.link_signup) TextView linkSignup;

    @BindView(R.id.ckbRemember) AppCompatCheckBox btnChkbox;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        //Init Paper to store user pass to android
        Paper.init(this);

        btnLogin.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                login();
            }
        });

        linkSignup.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Start the Signup activity
                Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
                startActivityForResult(intent, REQUEST_SIGNUP);
                finish();
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
            }
        });

        String user = Paper.book().read(Common.USER_KEY);
        String pwd = Paper.book().read(Common.PWD_KEY);
        if(user != null && pwd != null){
            if(!user.isEmpty() && !pwd.isEmpty()){
                loginAuto(user,pwd);
            }
        }
    }

    private void loginAuto(String phone, String pwd) {
        if (Common.isConnectedToInternet(getBaseContext())) {

            // TODO: Implement your own authentication logic here.
            final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this,
                    R.style.AppTheme_Dark_Dialog);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage("Logging in..");
            progressDialog.show();
            //Init Firebase

            FirebaseDatabase database = FirebaseDatabase.getInstance();
            final DatabaseReference table_user = database.getReference("Users");

            table_user.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    //check if user not exist in database
                    if (dataSnapshot.child(phone).exists()) {
                        //get user information
                        //mDialog.dismiss();
                        User user = dataSnapshot.child(phone).getValue(User.class);
                        user.setuPhone(phone); //set phone

                        if (user.getuPassword().equals(pwd)) {

                            Intent intent = new Intent(getBaseContext(), MainActivity.class);
                            startActivity(intent);
                            Common.currentUser = user;
                            finish();

                        } else {
                            Toast.makeText(LoginActivity.this, "Wrong Password !!!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        //mDialog.dismiss();
                        Toast.makeText(LoginActivity.this, "User Does'nt exist..!!", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        } else {
            Toast.makeText(LoginActivity.this, "Please check your connection !!", Toast.LENGTH_SHORT).show();
            return;
        }
    }


    public void login() {
        Log.d(TAG, "Login");

        if (!validate()) {
            onLoginFailed();
            return;
        }

        btnLogin.setEnabled(true);

        if (Common.isConnectedToInternet(getBaseContext())) {
            //save user& password
            if (btnChkbox.isChecked()) {
                Paper.book().write(Common.USER_KEY, inputPhone.getText().toString());
                Paper.book().write(Common.PWD_KEY, inputPassword.getText().toString());
            }

            final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this,
                    R.style.AppTheme_Dark_Dialog);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage("Authenticating...");
            progressDialog.show();

            String phone = inputPhone.getText().toString();
            String password = inputPassword.getText().toString();

            // TODO: Implement your own authentication logic here.

            //Init Firebase

            FirebaseDatabase database = FirebaseDatabase.getInstance();
            final DatabaseReference table_user = database.getReference("Users");

            table_user.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    //check if user not exist in database
                    if (dataSnapshot.child(phone).exists()) {
                        //get user information
                        //mDialog.dismiss();
                        User user = dataSnapshot.child(phone).getValue(User.class);
                        user.setuPhone(phone); //set phone

                        if (user.getuPassword().equals(password)) {

                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            Common.currentUser = user;
                            finish();

                        } else {
                            Toast.makeText(LoginActivity.this, "Wrong Password !!!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        //mDialog.dismiss();
                        Toast.makeText(LoginActivity.this, "User Does'nt exist..!!", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });


            new Handler().postDelayed(
                    new Runnable() {
                        public void run() {
                            // On complete call either onLoginSuccess or onLoginFailed
                            onLoginSuccess();
                            // onLoginFailed();
                            progressDialog.dismiss();
                        }
                    }, 3000);
        } else {
            Toast.makeText(LoginActivity.this, "Please check your connection !!", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    private void firstShow(){

        Intent intent2 = new Intent(getApplicationContext(), FirstStartActivity.class);
        startActivity(intent2);
        finish();
        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);

        SharedPreferences prefs = getSharedPreferences("prefs",MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("first_start",false);
        editor.apply();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SIGNUP) {
            if (resultCode == RESULT_OK) {

                // TODO: Implement successful signup logic here
                Toast.makeText(this, "Logged In", Toast.LENGTH_SHORT).show();
                // By default we just finish the Activity and log them in automatically
                this.finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        // Disable going back to the MainActivity
        moveTaskToBack(true);
    }

    public void onLoginSuccess() {
        btnLogin.setEnabled(true);
        finish();
    }

    public void onLoginFailed() {
        Toast.makeText(getBaseContext(), "Login failed", Toast.LENGTH_LONG).show();

        btnLogin.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        String phone = inputPhone.getText().toString();
        String password = inputPassword.getText().toString();

        if (phone.isEmpty() ) {
            inputPhone.setError("enter a valid phone number");
            valid = false;
        } else {
            inputPhone.setError(null);
        }

        if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
            inputPassword.setError("between 4 and 10 alphanumeric characters");
            valid = false;
        } else {
            inputPassword.setError(null);
        }
        return valid;
    }
}

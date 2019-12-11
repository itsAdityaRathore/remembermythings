package com.aditya.remembermythings.Activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
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
import com.rengwuxian.materialedittext.MaterialEditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.paperdb.Paper;


public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final int REQUEST_SIGNUP = 0;

    @BindView(R.id.input_phone)
    EditText inputPhone;
    @BindView(R.id.input_password)
    EditText inputPassword;
    @BindView(R.id.btn_login)
    AppCompatButton btnLogin;
    @BindView(R.id.link_signup)
    AppCompatButton linkSignup;
    @BindView(R.id.txtForgotPwd)
    TextView textFgtPwd;

    @BindView(R.id.ckbRemember)
    AppCompatCheckBox btnChkbox;

    FirebaseDatabase database;
    DatabaseReference table_user;

    String user, pwd;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        inputPhone.clearFocus();
        inputPhone.setFocusableInTouchMode(true);

        //Init Firebase

        database = FirebaseDatabase.getInstance();
        table_user = database.getReference("Users");

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

        textFgtPwd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showForgotPwdDialog();
            }
        });


        user = Paper.book().read(Common.USER_KEY);
        pwd = Paper.book().read(Common.PWD_KEY);
        if (user != null && pwd != null) {
            if (!user.isEmpty() && !pwd.isEmpty()) {
                loginAuto(user, pwd);
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

            table_user.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

//                    //check if user not exist in database
                    if (dataSnapshot.child(phone).exists()) {
                        //get user information
                        //mDialog.dismiss();
                        User user = dataSnapshot.child(phone).getValue(User.class);
                        user.setuPhone(phone); //set phone

                        if (user.getuPassword().equals(pwd)) {

                            Common.currentUser = user;
                            Intent intent = new Intent(getBaseContext(), MainActivity.class);
                            intent.putExtra("uPhone", phone);
                            startActivity(intent);
                            finish();
                            //progressDialog.dismiss();

                        } else {
                            Toast.makeText(LoginActivity.this, "Wrong Password !!!", Toast.LENGTH_SHORT).show();
                            // progressDialog.dismiss();
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


            final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this,
                    R.style.AppTheme_Dark_Dialog);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage("Authenticating...");
            progressDialog.show();

            String phone = inputPhone.getText().toString();
            String password = inputPassword.getText().toString();

            // TODO: Implement your own authentication logic here.

            table_user.addListenerForSingleValueEvent(new ValueEventListener() {
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
                            intent.putExtra("uPhone", phone);
                            startActivity(intent);
                            Common.currentUser = user;
                            //save user& password
                            if (btnChkbox.isChecked()) {
                                Paper.book().write(Common.USER_KEY, inputPhone.getText().toString());
                                Paper.book().write(Common.PWD_KEY, inputPassword.getText().toString());
                            }
                            progressDialog.dismiss();
                            finish();

                        } else {
                            Toast.makeText(LoginActivity.this, "Wrong Password !!!", Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss();
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

                        }
                    }, 3000);

        } else {
            Toast.makeText(LoginActivity.this, "Please check your connection !!", Toast.LENGTH_SHORT).show();
            return;
        }
    }

//    private void firstShow() {
//
//        Intent intent2 = new Intent(getApplicationContext(), FirstStartActivity.class);
//        startActivity(intent2);
//        finish();
//        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
//
//        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
//        SharedPreferences.Editor editor = prefs.edit();
//        editor.putBoolean("first_start", false);
//        editor.apply();
//    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
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
        //finish();
    }

    public void onLoginFailed() {
        Toast.makeText(getBaseContext(), "Login failed", Toast.LENGTH_LONG).show();

        btnLogin.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        String phone = inputPhone.getText().toString();
        String password = inputPassword.getText().toString();

        if (phone.isEmpty()) {
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


    private void showForgotPwdDialog() {

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference table_user = database.getReference("Users");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Forgot Password");
        builder.setMessage("Enter food you set at the time of signup..");

        LayoutInflater inflater = this.getLayoutInflater();
        View forgot_view = inflater.inflate(R.layout.forgot_password_layout, null);

        builder.setView(forgot_view);
        builder.setIcon(R.drawable.ic_security_black_24dp);

        final MaterialEditText edtPhone = forgot_view.findViewById(R.id.edtPhone);
        final MaterialEditText edtSecureCode = forgot_view.findViewById(R.id.edtSecureCode);

        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Check if user is available
                if (edtPhone.getText() != null || inputPassword.getText() != null) {
                    table_user.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            if (dataSnapshot.child(edtPhone.getText().toString()).exists()) {

                                User user = dataSnapshot.child(edtPhone.getText().toString()).getValue(User.class);

                                if (user.getuSecQues().equals(edtSecureCode.getText().toString()))
                                    Toast.makeText(LoginActivity.this, "Your password : " + user.getuPassword(), Toast.LENGTH_LONG).show();
                                else
                                    Toast.makeText(LoginActivity.this, "Wrong secure code !!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(LoginActivity.this, "User Doesn't exist!!!", Toast.LENGTH_SHORT).show();
                            }


                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }
            }
        });
        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        builder.show();

    }

}

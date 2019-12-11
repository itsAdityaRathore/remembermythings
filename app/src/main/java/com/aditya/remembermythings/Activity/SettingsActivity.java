package com.aditya.remembermythings.Activity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.aditya.remembermythings.Common.Common;
import com.aditya.remembermythings.Model.User;
import com.aditya.remembermythings.R;
import com.applovin.sdk.AppLovinSdk;
import com.google.android.ads.nativetemplates.NativeTemplateStyle;
import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";

    AdView sAdView;

    @BindView(R.id.txtOldPass) EditText oldPassword;
    @BindView(R.id.txtNewPass) EditText newPassword;
    @BindView(R.id.txtConfirmPass) EditText confirmPassword;
    @BindView(R.id.btn_changePassword) Button changePassword;
    @BindView(R.id.changePassGrid) GridLayout changePassGrid;
    @BindView(R.id.btn_share) Button shareAppBtn;
    @BindView(R.id.versionNumber) TextView verNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);


        AppLovinSdk.initializeSdk(getApplicationContext());

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        sAdView = findViewById(R.id.adViewSettings);
        AdRequest adRequest = new AdRequest.Builder().build();
        sAdView.loadAd(adRequest);

        sAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                Log.d("ST Banner Ad Test", "Add Finished Loading");
            }

            @Override
            public void onAdFailedToLoad(int i) {
                Log.d("ST Banner Ad Test", "Add Loading Failed");
            }

            @Override
            public void onAdOpened() {
                Log.d("ST Banner Ad Test", "Add is Visible Now");
            }
        });

        changePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePassword();
            }
        });

        shareAppBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareApp(getApplicationContext());
            }
        });

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            int versionCode = pInfo.versionCode;
            verNumber.setText("Version : " + version);
            Log.d("MyApp", "Version Name : "+version + "\n Version Code : "+versionCode);

        } catch(PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            Log.d("MyApp", "PackageManager Catch : "+e.toString());
        }

        ColorDrawable colorDrawable = new ColorDrawable(ContextCompat.getColor(this, R.color.white));


//        AdLoader adLoader = new AdLoader.Builder(this, "ca-app-pub-5973465911931412/9836918220")
//                .forUnifiedNativeAd(new UnifiedNativeAd.OnUnifiedNativeAdLoadedListener() {
//                    @Override
//                    public void onUnifiedNativeAdLoaded(UnifiedNativeAd unifiedNativeAd) {
//                        NativeTemplateStyle styles = new
//                                NativeTemplateStyle.Builder().withMainBackgroundColor(colorDrawable).build();
//
//                        TemplateView template = findViewById(R.id.my_template);
//                        template.setStyles(styles);
//                        template.setNativeAd(unifiedNativeAd);
//
//                    }
//                })
//                .build();
//
//        adLoader.loadAd(new AdRequest.Builder().build());
        LinearLayout NativlinearLayout = findViewById(R.id.nativad);
        AdLoader adLoader = new AdLoader.Builder(this, "ca-app-pub-5973465911931412/8599610756")
                .forUnifiedNativeAd(new UnifiedNativeAd.OnUnifiedNativeAdLoadedListener() {
                    @Override
                    public void onUnifiedNativeAdLoaded(UnifiedNativeAd unifiedNativeAd) {

                        NativlinearLayout.setVisibility(View.VISIBLE);
                        NativeTemplateStyle styles = new
                                NativeTemplateStyle.Builder().withMainBackgroundColor(colorDrawable).build();

                        TemplateView template = findViewById(R.id.my_template);
                        template.setStyles(styles);
                        template.setNativeAd(unifiedNativeAd);
                    }
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(int errorCode) {
                        // Handle the failure by logging, altering the UI, and so on.
                    }
                })
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        // Methods in the NativeAdOptions.Builder class can be
                        // used here to specify individual options settings.
                        .build())
                .build();

        adLoader.loadAd(new AdRequest.Builder().build());

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

    private void shareApp(Context context) {

        int applicationNameId = context.getApplicationInfo().labelRes;
        final String appPackageName = context.getPackageName();
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, getString(applicationNameId));
        String text = "Install this cool application : ";
        String link = "https://play.google.com/store/apps/details?id=" + appPackageName;
        i.putExtra(Intent.EXTRA_TEXT, text + " " + link);
        startActivity(Intent.createChooser(i, "Share link:"));

    }
}

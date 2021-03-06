package com.aditya.remembermythings.Activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.cardview.widget.CardView;

import com.aditya.remembermythings.Model.Items;
import com.aditya.remembermythings.R;
import com.aditya.remembermythings.UpdateCheck.UpdateHelper;
import com.aditya.remembermythings.ViewHolder.ItemViewHolder;
import com.applovin.sdk.AppLovinSdk;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.vanniktech.rxpermission.Permission;
import com.vanniktech.rxpermission.RealRxPermission;
import com.vanniktech.rxpermission.RxPermission;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

import butterknife.BindView;
import hotchemi.android.rate.AppRate;
import hotchemi.android.rate.OnClickButtonListener;
import id.zelory.compressor.Compressor;
import io.paperdb.Paper;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;


public class MainActivity extends AppCompatActivity implements UpdateHelper.OnUpdateCheckListener {

    Boolean checkFirst;
    SharedPreferences.Editor editor;

    public int counter = 0;

    RxPermission rxPermission;
    final CompositeDisposable compositeDisposable = new CompositeDisposable();

    GridLayout mainGrid;
    String uPhone;

    Items newItem;

    @BindView(R.id.edt_Name)
    EditText edtName;

    AppCompatImageView edtImage;
    AppCompatButton btnUpload, btnSelect;

    //Firebase
    FirebaseDatabase database;
    DatabaseReference items, userItem;
    FirebaseStorage storage;
    StorageReference storageReference;

    Snackbar snackbar;

    FirebaseRecyclerAdapter<Items, ItemViewHolder> adapter;

    File mediaStorageDir;
    Uri picUri;
    private static final int CAPTURE_IMAGE = 0;

    private AdView mAdView;
    private InterstitialAd mInterstitialAd;
    ImageView bgImage, mainLogoImage;
    Animation fromBottom;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        rxPermission = RealRxPermission.getInstance(getApplication());
        compositeDisposable.add(rxPermission.requestEach(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(new Consumer<Permission>() {
                    @Override
                    public void accept(final Permission granted) throws Exception {
                        //  Toast.makeText(MainActivity.this, granted.toString(), Toast.LENGTH_LONG).show();
                    }
                }));

        MyTask myTask = new MyTask();
        myTask.execute();
//        MediationTestSuite.launch(MainActivity.this);
//        MediationTestSuite.addTestDevice("5FD10B823D499740F54DDA97695D27FE");

        getNotificationFirebase();

        AppRate.with(this)
                .setInstallDays(1) // default 10, 0 means install day.
                .setLaunchTimes(3) // default 10
                .setRemindInterval(2) // default 1
                .setShowLaterButton(true) // default true
                .setDebug(false) // default false
                .setOnClickButtonListener(new OnClickButtonListener() { // callback listener.
                    @Override
                    public void onClickButton(int which) {
                        Log.d(MainActivity.class.getName(), Integer.toString(which));
                    }
                })
                .monitor();

        // Show a dialog if meets conditions
        AppRate.showRateDialogIfMeetsConditions(this);

        fromBottom = AnimationUtils.loadAnimation(this, R.anim.from_bottom);
        mainLogoImage = findViewById(R.id.mainLogoImage);
        mainLogoImage.startAnimation(fromBottom);

        UpdateHelper.with(this).onUpdateCheck(this).check();

        AppLovinSdk.initializeSdk(getApplicationContext());

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                Log.d("MA Banner Ad Test", "Ad Finished Loading");
            }

            @Override
            public void onAdFailedToLoad(int i) {
                Log.d("MA Banner Ad Test", "Ad Loading Failed");
            }

            @Override
            public void onAdOpened() {
                Log.d("MA Banner Ad Test", "Ad is Visible Now");
            }
        });

        //InterstitialAds
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-5973465911931412/4652201857");
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener() {
            //.addTestDevice("5FD10B823D499740F54DDA97695D27FE")
            @Override
            public void onAdClosed() {
                // Load the next interstitial.
                //mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }

            @Override
            public void onAdLoaded() {
                Log.d("MA Interstitial Ad Test", "Ad Finished Loading");
            }

            @Override
            public void onAdFailedToLoad(int i) {
                Log.d("MA Interstitial Ad Test", "Ad Loading Failed");
            }

            @Override
            public void onAdOpened() {
                Log.d("MA Interstitial Ad Test", "Ad is Visible Now");
            }
        });

        if (getIntent().hasExtra("uPhone")) {
            uPhone = getIntent().getStringExtra("uPhone");
        }

        //Init Firebase
        database = FirebaseDatabase.getInstance();
        items = database.getReference("Items");
        //userItem = items.child(Common.currentUser.getuPhone());
        userItem = items.child(uPhone);
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        mainGrid = findViewById(R.id.mainGrid);
        //Set Event
        setSingleEvent(mainGrid);
        //setToggleEvent(mainGrid);

        //INIT paper
        Paper.init(this);

        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
        checkFirst = pref.getBoolean("first_start", true);
        editor = pref.edit();

//        if (checkFirst != Boolean.FALSE) {
//            TapTargetView.showFor(this,                 // `this` is an Activity
//                    TapTarget.forView(findViewById(R.id.addProperty), "Add new item", "Click here to add new items you want to remember")
//                            // All options below are optional
//                            .outerCircleColor(R.color.primary)      // Specify a color for the outer circle
//                            .outerCircleAlpha(0.96f)            // Specify the alpha amount for the outer circle
//                            .targetCircleColor(R.color.white)   // Specify a color for the target circle
//                            .titleTextSize(25)                  // Specify the size (in sp) of the title text
//                            .titleTextColor(R.color.white)      // Specify the color of the title text
//                            .descriptionTextSize(18)            // Specify the size (in sp) of the description text
//                            .descriptionTextColor(R.color.white)  // Specify the color of the description text
//                            .textColor(R.color.white)            // Specify a color for both the title and description text
//                            .textTypeface(Typeface.SANS_SERIF)  // Specify a typeface for the text
//                            .dimColor(R.color.black)            // If set, will dim behind the view with 30% opacity of the given color
//                            .drawShadow(true)                   // Whether to draw a drop shadow or not
//                            .cancelable(true)                  // Whether tapping outside the outer circle dismisses the view
//                            .tintTarget(true)                   // Whether to tint the target view's color
//                            .transparentTarget(true)           // Specify whether the target is transparent (displays the content underneath)
//                            // Specify a custom drawable to draw as the target
//                            .targetRadius(65),                  // Specify the target radius (in dp)
//                    new TapTargetView.Listener() {          // The listener can listen for regular clicks, long clicks or cancels
//                        @Override
//                        public void onTargetClick(TapTargetView view) {
//                            super.onTargetClick(view);      // This call is optional
//                            showDialog();
//                            view.dismiss(true);
//                        }
//                    });
//            //editor.putBoolean("first_start", false).commit();
//        }
    }

    private class MyTask extends AsyncTask<String, String, String> {
        String resp;
        Boolean checkFirst;
        SharedPreferences.Editor editor;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... strings) {
            SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
            checkFirst = pref.getBoolean("first_start", true);
            editor = pref.edit();
            if (checkFirst != Boolean.FALSE) {
                TapTargetView.showFor(MainActivity.this,                 // `this` is an Activity
                        TapTarget.forView(findViewById(R.id.addProperty), "Add new item", "Click here to add new items you want to remember")
                                // All options below are optional
                                .outerCircleColor(R.color.primary)      // Specify a color for the outer circle
                                .outerCircleAlpha(0.96f)            // Specify the alpha amount for the outer circle
                                .targetCircleColor(R.color.white)   // Specify a color for the target circle
                                .titleTextSize(25)                  // Specify the size (in sp) of the title text
                                .titleTextColor(R.color.white)      // Specify the color of the title text
                                .descriptionTextSize(18)            // Specify the size (in sp) of the description text
                                .descriptionTextColor(R.color.white)  // Specify the color of the description text
                                .textColor(R.color.white)            // Specify a color for both the title and description text
                                .textTypeface(Typeface.SANS_SERIF)  // Specify a typeface for the text
                                .dimColor(R.color.black)            // If set, will dim behind the view with 30% opacity of the given color
                                .drawShadow(true)                   // Whether to draw a drop shadow or not
                                .cancelable(true)                  // Whether tapping outside the outer circle dismisses the view
                                .tintTarget(true)                   // Whether to tint the target view's color
                                .transparentTarget(true)           // Specify whether the target is transparent (displays the content underneath)
                                // Specify a custom drawable to draw as the target
                                .targetRadius(65),                  // Specify the target radius (in dp)
                        new TapTargetView.Listener() {          // The listener can listen for regular clicks, long clicks or cancels
                            @Override
                            public void onTargetClick(TapTargetView view) {
                                super.onTargetClick(view);      // This call is optional
                                showDialog();
                                view.dismiss(true);
                            }
                        });
                //editor.putBoolean("first_start", false).commit();
            }
            return resp;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }
    }

    private void getNotificationFirebase() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel_1 = new NotificationChannel("MyNotifications", "MyNotifications", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel_1);
        }

        FirebaseMessaging.getInstance().subscribeToTopic("normal")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        String msg = "Successfull";
                        if (!task.isSuccessful()) {
                            msg = "Failed";
                        }
                        //Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public void showInterstitial() {
        if (counter < 1) {
            mInterstitialAd.show();
            counter++;
        } else {
            Log.d("InterstitalAd", "The interstitial wasn't loaded yet.");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    String myAdapter;

    private void setSingleEvent(GridLayout mainGrid) {

        //Loop all child item of Main Grid
        for (int i = 0; i < mainGrid.getChildCount(); i++) {
            //You can see , all child item is CardView , so we just cast object to CardView
            CardView cardView = (CardView) mainGrid.getChildAt(i);
            final int finalI = i;
            cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                   /* Intent intent = new Intent(MainActivity.this,MainActivity.class);
                    intent.putExtra("info",""+finalI);
                    startActivity(intent);*/

                    //Toast.makeText(MainActivity.this, "Clicked = "+finalI, Toast.LENGTH_SHORT).show();
                    if (finalI == 0) {
                        showDialog();
                    } else if (finalI == 1) {

                        adapter = new FirebaseRecyclerAdapter<Items, ItemViewHolder>(
                                Items.class,
                                R.layout.view_item,
                                ItemViewHolder.class,
                                items) {

                            @Override
                            protected void populateViewHolder(ItemViewHolder itemViewHolder, Items items, int i) {
                                itemViewHolder.txtItemName.setText(items.getName());
                                Picasso.get().load(items.getImage()).into(itemViewHolder.imageView);
                                myAdapter = adapter.getRef(i).getKey();
                            }
                        };


                        DatabaseReference table_user = userItem;

                        table_user.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {

                                    int random = ThreadLocalRandom.current().nextInt(15, 20);
                                    int newRandom = random * 1000;

                                    new CountDownTimer(newRandom, 1000) {
                                        @Override
                                        public void onTick(long millisUntilFinished) {

                                        }

                                        @Override
                                        public void onFinish() {
                                            //Toast.makeText(MainActivity.this, "Main View Add", Toast.LENGTH_SHORT).show();
                                            showInterstitial();
                                        }
                                    }.start();

                                    Intent intent = new Intent(getApplicationContext(), ItemViewActivity.class);
                                    intent.putExtra("CategoryId", myAdapter);
                                    startActivity(intent);
                                } else {
                                    Toast.makeText(MainActivity.this, "No Items to View", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });

                    } else if (finalI == 2) {

                        Intent settingIntent = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivity(settingIntent);


                    } else if (finalI == 3) {
                        //delete remember user after logout

                        showLogoutDialog();
                    }
                }
            });
        }
    }

    private void showDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog.setTitle("Add new Category");
        alertDialog.setMessage("Please fill full information");

        LayoutInflater inflater = this.getLayoutInflater();
        View add_item_layout = inflater.inflate(R.layout.add_new_item, null);

        edtName = add_item_layout.findViewById(R.id.edt_Name);
        edtImage = add_item_layout.findViewById(R.id.edtImage);
        btnSelect = add_item_layout.findViewById(R.id.btnSelect);

        ////Event for button
        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage(); // let user to select image from Gallery and save Uri of image
            }
        });

        alertDialog.setView(add_item_layout);
        //alertDialog.setIcon(R.drawable.ic_shopping_cart_black_24dp);

        //Set Button
        alertDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                String name = edtName.getText().toString();
                if (!name.isEmpty()) {
                    uploadImage();
                } else {
                    Toast.makeText(MainActivity.this, "Name cannot be Empty..!!!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        alertDialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        alertDialog.show();
    }

    private void showLogoutDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog.setTitle("Sure want to log out?");

        //Set Button
        alertDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                counter = 0;
                Paper.book().destroy();
                Intent logIn = new Intent(MainActivity.this, LoginActivity.class);
                logIn.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(logIn);

            }
        });
        alertDialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialog.show();
    }

    private void chooseImage() {

        Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

        File file = getOutputMediaFile(1);
        picUri = Uri.fromFile(file); // create
        i.putExtra(MediaStore.EXTRA_OUTPUT, picUri); // set the image file

        startActivityForResult(i, CAPTURE_IMAGE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Uri uri = picUri;
        Bitmap imageBitmap = null;

        try {
            imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), picUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (imageBitmap != null) {
            btnSelect.setText("Image Selected");
            edtImage.setImageBitmap(imageBitmap);
        }

    }

    /**
     * Create a File for saving an image
     */
    private File getOutputMediaFile(int type) {
        mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "CameraApp");

        /**Create the storage directory if it does not exist*/
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }

        /**Create a media file name*/
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == 1) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".png");
        } else {
            return null;
        }
        return mediaFile;
    }

    private void uploadImage() {
        //Toast.makeText(this, "Im in Upload", Toast.LENGTH_SHORT).show();
        if (picUri != null) {
            final ProgressDialog mDialog = new ProgressDialog(this);
            mDialog.setMessage("Uploading...");
            mDialog.show();
            Uri uri = picUri;
            //String imageName = UUID.randomUUID().toString();
            //final StorageReference imageFolder = storageReference.child("images/"+imageName);
            //Compress Image before upload
            File actualImage = new File(uri.getPath());

            try {
                Bitmap compressedImage = new Compressor(this)
                        .setMaxWidth(420)
                        .setMaxHeight(240)
                        .setQuality(50)
                        .setCompressFormat(Bitmap.CompressFormat.WEBP)
                        .setDestinationDirectoryPath(Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_PICTURES).getAbsolutePath())
                        .compressToBitmap(actualImage);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                compressedImage.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                byte[] final_image = baos.toByteArray();

                Bitmap imageBitmap = null;
                try {
                    imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), picUri);

                } catch (IOException e) {
                    e.printStackTrace();
                }


                final StorageReference imageFolder = storageReference.child("images/").child(uPhone + "_" + uri.getLastPathSegment());

                UploadTask uploadTask = imageFolder.putBytes(final_image);


                //imageFolder.putFile(picUri)
                uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        mDialog.dismiss();
                        //Toast.makeText(MainActivity.this, "Uploaded succesfully", Toast.LENGTH_SHORT).show();
                        imageFolder.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                // set value for newCategory and we can get download link
                                newItem = new Items(edtName.getText().toString(), uri.toString());
                                //we create new category
                                if (newItem != null) {
                                    userItem.push().setValue(newItem);
                                    //Snackbar.make(drawer,"New Item "+newItem.getName()+" was added", Snackbar.LENGTH_SHORT).show();
                                    Snackbar.make(findViewById(R.id.root_layout), "New Item " + newItem.getName() + " was added", Snackbar.LENGTH_LONG).show();
                                    actualImage.delete();

                                    //Toast.makeText(MainActivity.this, "New Item " + newItem.getName() + " was added", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(Exception e) {
                                mDialog.dismiss();
                                Toast.makeText(MainActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "Please Click the image first..!!!", Toast.LENGTH_LONG).show();
        }
        if (checkFirst != Boolean.FALSE) {
            TapTargetView.showFor(this,                 // `this` is an Activity
                    TapTarget.forView(findViewById(R.id.viewProperty), "View Items", "Click here to view items you added.")
                            // All options below are optional
                            .outerCircleColor(R.color.primary)      // Specify a color for the outer circle
                            .outerCircleAlpha(0.96f)            // Specify the alpha amount for the outer circle
                            .targetCircleColor(R.color.white)   // Specify a color for the target circle
                            .titleTextSize(25)                  // Specify the size (in sp) of the title text
                            .titleTextColor(R.color.white)      // Specify the color of the title text
                            .descriptionTextSize(18)            // Specify the size (in sp) of the description text
                            .descriptionTextColor(R.color.white)  // Specify the color of the description text
                            .textColor(R.color.white)            // Specify a color for both the title and description text
                            .textTypeface(Typeface.SANS_SERIF)  // Specify a typeface for the text
                            .dimColor(R.color.black)            // If set, will dim behind the view with 30% opacity of the given color
                            .drawShadow(true)                   // Whether to draw a drop shadow or not
                            .cancelable(true)                  // Whether tapping outside the outer circle dismisses the view
                            .tintTarget(true)                   // Whether to tint the target view's color
                            .transparentTarget(true)           // Specify whether the target is transparent (displays the content underneath)
                            // Specify a custom drawable to draw as the target
                            .targetRadius(65),                  // Specify the target radius (in dp)
                    new TapTargetView.Listener() {          // The listener can listen for regular clicks, long clicks or cancels
                        @Override
                        public void onTargetClick(TapTargetView view) {
                            super.onTargetClick(view);      // This call is optional
                            adapter = new FirebaseRecyclerAdapter<Items, ItemViewHolder>(
                                    Items.class,
                                    R.layout.view_item,
                                    ItemViewHolder.class,
                                    items) {

                                @Override
                                protected void populateViewHolder(ItemViewHolder itemViewHolder, Items items, int i) {
                                    itemViewHolder.txtItemName.setText(items.getName());
                                    Picasso.get().load(items.getImage()).into(itemViewHolder.imageView);
                                    myAdapter = adapter.getRef(i).getKey();
                                }
                            };


                            DatabaseReference table_user = userItem;

                            table_user.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        Intent intent = new Intent(getApplicationContext(), ItemViewActivity.class);
                                        intent.putExtra("CategoryId", myAdapter);

                                        startActivity(intent);
                                    } else {
                                        Toast.makeText(MainActivity.this, "No Items to View", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                            view.dismiss(true);
                        }
                    });
            editor.putBoolean("first_start", false).commit();
        }
    }

    @Override
    public void onUpdateCheckListener(String urlApp) {
        //create alert dialog
        androidx.appcompat.app.AlertDialog alertDialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("New Version Available")
                .setMessage("Please update to latest vesion to get new features")
                .setPositiveButton("UPDATE", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(urlApp));
                        startActivity(intent);
                        //Toast.makeText(MainActivity.this, ""+urlApp, Toast.LENGTH_SHORT).show();
                    }
                }).setNegativeButton("CANCLE", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).create();
        alertDialog.show();
    }


}

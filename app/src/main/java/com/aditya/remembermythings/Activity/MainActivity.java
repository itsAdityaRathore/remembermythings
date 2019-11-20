package com.aditya.remembermythings.Activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.cardview.widget.CardView;

import com.aditya.remembermythings.Common.Common;
import com.aditya.remembermythings.Model.Items;
import com.aditya.remembermythings.Model.User;
import com.aditya.remembermythings.R;
import com.aditya.remembermythings.ViewHolder.ItemViewHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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
import id.zelory.compressor.Compressor;
import io.paperdb.Paper;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;


public class MainActivity extends AppCompatActivity {

    InterstitialAd mInterstitialAd;
    public int counter = 0;

    RxPermission rxPermission;
    final CompositeDisposable compositeDisposable = new CompositeDisposable();

    GridLayout mainGrid;
    String uPhone;

    Items newItem;

    //Add new menu Layout
    //AppCompatEditText edtName;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        rxPermission = RealRxPermission.getInstance(getApplication());

        int random = ThreadLocalRandom.current().nextInt(10, 20);
        int newRandom = random * 1000;


        new CountDownTimer(newRandom, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {

                if (counter < 2)
                    showInterstitial();
            }
        }.start();

        //Banner Ads
        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        //InterstitialAds
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-3940256099942544/1033173712");
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                super.onAdClosed();
                //finish();
            }
        });

        compositeDisposable.add(rxPermission.requestEach(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(new Consumer<Permission>() {
                    @Override
                    public void accept(final Permission granted) throws Exception {
                        //  Toast.makeText(MainActivity.this, granted.toString(), Toast.LENGTH_LONG).show();
                    }
                }));

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
    }

    public void showInterstitial() {
        if (mInterstitialAd.isLoaded()) {
            counter++;
            mInterstitialAd.show();
        } else {
            //finish();
        }
    }

//    @Override
//    protected void onDestroy() {
//        compositeDisposable.clear();
//        //super.onDestroy();
//    }

    private void login(String phone, String pwd) {
        if (Common.isConnectedToInternet(getBaseContext())) {
            //save user& password

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

                        if (user.getuPassword().equals(pwd)) {

                            Intent intent = new Intent(getBaseContext(), MainActivity.class);
                            startActivity(intent);
                            Common.currentUser = user;
                            finish();

                        } else {
                            Toast.makeText(MainActivity.this, "Wrong Password !!!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        //mDialog.dismiss();
                        Toast.makeText(MainActivity.this, "User Does'nt exist..!!", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        } else {
            Toast.makeText(MainActivity.this, "Please check your connection !!", Toast.LENGTH_SHORT).show();
            return;
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
                        Paper.book().destroy();

                        //show ads
                        showInterstitial();

                        Intent logIn = new Intent(MainActivity.this, LoginActivity.class);
                        logIn.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(logIn);
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


                final StorageReference imageFolder = storageReference.child("images/").child(uri.getLastPathSegment());

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
    }

}

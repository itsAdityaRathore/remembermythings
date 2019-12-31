package com.aditya.remembermythings.Activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aditya.remembermythings.Common.Common;
import com.aditya.remembermythings.Model.Items;
import com.aditya.remembermythings.R;
import com.aditya.remembermythings.ViewHolder.ItemClickListener;
import com.aditya.remembermythings.ViewHolder.ItemViewHolder;
import com.applovin.sdk.AppLovinSdk;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import butterknife.BindView;
import id.zelory.compressor.Compressor;

public class ItemViewActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    InterstitialAd iInterstitialAd;
    int counter = 0;

    //TextView txtFullName;
    AppCompatImageView edtImage, viewFullImage;
    AppCompatButton btnSelect;

    String categoryId = "";
    Items newItem, imageviewItem;
    @BindView(R.id.edt_Name)
    EditText edtName;

    File mediaStorageDir;
    Uri picUri, saveUri;

    private static final int CAPTURE_IMAGE = 0;

    //Firebase
    FirebaseDatabase database;
    DatabaseReference items, userItem;
    FirebaseStorage storage;
    StorageReference storageReference;

    FirebaseRecyclerAdapter<Items, ItemViewHolder> adapter;

    MenuItem myItem;
    //View

    RecyclerView recycler_menu;
    RecyclerView.LayoutManager layoutManager;

    //Search Bar Functionality
    FirebaseRecyclerAdapter<Items, ItemViewHolder> searchAdapter;
    List<String> suggestList = new ArrayList<>();
    MaterialSearchBar materialSearchBar;

    private AdView iAdView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_item_view);

        AppLovinSdk.initializeSdk(getApplicationContext());

//        AdColonyAppOptions appOptions = AdColonyMediationAdapter.getAppOptions();
//        appOptions.setGDPRConsentString("1");
//        appOptions.setGDPRRequired(true);
//
//        AdColony.configure(this,           // activity context
//                "app0c5f24e2a6fa46abb1",
//                "vza5fbba0f0486428ca2", "vz45785b98ca264fd1be"); // list of all your zones set up on the AdColony Dashboard

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        iAdView = findViewById(R.id.adViewItemVIew);
        AdRequest adRequest = new AdRequest.Builder().build();
        iAdView.loadAd(adRequest);

        iAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                Log.d("IV Banner Ad Test", "Add Finished Loading");
            }

            @Override
            public void onAdFailedToLoad(int i) {
                Log.d("IV Banner Ad Test", "Add Loading Failed");
            }

            @Override
            public void onAdOpened() {
                Log.d("IV Banner Ad Test", "Add is Visible Now");
            }
        });

        //InterstitialAds
        iInterstitialAd = new InterstitialAd(this);
        iInterstitialAd.setAdUnitId("ca-app-pub-5973465911931412/9703635404");
        iInterstitialAd.loadAd(new AdRequest.Builder().build());
        iInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                // Load the next interstitial.
               // mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }

            @Override
            public void onAdLoaded() {
                Log.d("IV Interstitial Ad Test", "Add Finished Loading");
            }

            @Override
            public void onAdFailedToLoad(int i) {
                Log.d("IV Interstitial Ad Test", "Add Loading Failed");
            }

            @Override
            public void onAdOpened() {
                Log.d("IV Interstitial Ad Test", "Add is Visible Now");
            }
        });

//        int random = ThreadLocalRandom.current().nextInt(5, 15);
//        int newRandom = random * 1000;
//
//        new CountDownTimer(newRandom, 1000) {
//            @Override
//            public void onTick(long millisUntilFinished) {
//                counter++;
//            }
//
//            @Override
//            public void onFinish() {
//                showInterstitial();
//            }
//        }.start();

        //Init Firebase
        database = FirebaseDatabase.getInstance();
        items = database.getReference("Items").child(Common.currentUser.getuPhone());
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        //Init View
        recycler_menu = findViewById(R.id.recycler_menu);
        recycler_menu.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getApplicationContext());
        recycler_menu.setLayoutManager(layoutManager);

        if (items != null) {
            loadMenuNew();
        } else
            Toast.makeText(getApplicationContext(), "You Have No Items To View", Toast.LENGTH_SHORT).show();


        if (getIntent() != null) {
            categoryId = getIntent().getStringExtra("CategoryId");
        }

        //SearchBar
        materialSearchBar = findViewById(R.id.searchBar);
        materialSearchBar.setHint("Enter item name");
        materialSearchBar.setTextColor(R.color.black);
        //materialSearchBar.setSpeechMode(false);
        loadSuggest();
        //Function to load suggestion form firebase
        materialSearchBar.setLastSuggestions(suggestList);
        materialSearchBar.setCardViewElevation(10);
        materialSearchBar.setPlaceHolder("Search items..");
        materialSearchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //when user type , it will change suggestion list

                List<String> suggest = new ArrayList<>();
                for (String search : suggestList) {
                    if (search.toLowerCase().contains(materialSearchBar.getText().toLowerCase())) {
                        suggest.add(search);

                    }
                    materialSearchBar.setLastSuggestions(suggest);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        materialSearchBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {
                //when search bar close , restore orignal adapter
                if (!enabled)
                    recycler_menu.setAdapter(adapter);
            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
                // when search finish, show result of search adapter
                startSearch(text);
            }

            @Override
            public void onButtonClicked(int buttonCode) {

            }
        });
    }

//    public void showInterstitial() {
//        if (mInterstitialAd.isLoaded()) {
//            mInterstitialAd.show();
//        } else {
//            //finish();
//        }
//    }

    private void startSearch(CharSequence text) {
        adapter = new FirebaseRecyclerAdapter<Items, ItemViewHolder>(
                Items.class,
                R.layout.view_item,
                ItemViewHolder.class,
                items.orderByChild("name").equalTo(text.toString()) //compare name
        ) {


            @Override
            protected void populateViewHolder(ItemViewHolder viewHolder, Items model, int position) {
                viewHolder.txtItemName.setText(model.getName());
                Picasso.get().load(model.getImage()).into(viewHolder.imageView);

                viewHolder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                        //Start new Activity
                        viewImage(model);
                    }
                });
            }
        };
        recycler_menu.setAdapter(adapter); //set adapter for recycle view is Search result
    }

    private void loadSuggest() {

        items.orderByChild("menuId").equalTo(categoryId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                            Items item = postSnapshot.getValue(Items.class);
                            suggestList.add(item.getName());  //Add names of food to suggest list
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    private void loadMenuNew() {

        adapter = new FirebaseRecyclerAdapter<Items, ItemViewHolder>(
                Items.class,
                R.layout.view_item,
                ItemViewHolder.class,
                items
        ) {
            @Override
            protected void populateViewHolder(ItemViewHolder viewHolder, Items model, int position) {

                viewHolder.txtItemName.setText(model.getName());
                Picasso.get().load(model.getImage()).into(viewHolder.imageView);
                categoryId = adapter.getItem(position).getName();

                viewHolder.setItemClickListener(new ItemClickListener() {

                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                        //Toast.makeText(ItemViewActivity.this, "Long Press On The Item To Update/Delete", Toast.LENGTH_SHORT).show();
                        viewImage(model);
                    }
                });
            }
        };
        adapter.notifyDataSetChanged();
        recycler_menu.setAdapter(adapter);
    }

    public void viewImage(Items item) {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(ItemViewActivity.this);
        alertDialog.setTitle("Search this place");
        alertDialog.setMessage("Long Press On The Item in the list To Update or Delete");
        //alertDialog.setIcon(R.drawable.view_property);
        LayoutInflater inflater = this.getLayoutInflater();
        View add_image_layout = inflater.inflate(R.layout.view_full_image, null);
        viewFullImage = add_image_layout.findViewById(R.id.viewFullImage);
        Picasso.get().load(item.getImage()).into(viewFullImage);
        alertDialog.setView(add_image_layout);
        alertDialog.show();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        myItem = item;


        if (item.getTitle().equals(Common.UPDATE)) {
            showUpdateFoodDialog(adapter.getRef(item.getOrder()).getKey(), adapter.getItem(item.getOrder()));
        } else if (item.getTitle().equals(Common.DELETE)) {
            deleteFood(adapter.getRef(item.getOrder()).getKey());
        }

        return super.onContextItemSelected(item);
    }

    private void deleteFood(String key) {
        final String[] delURL = new String[1];

        items.child(key).child("image").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    if (dataSnapshot.getValue() != null) {
                        try {
                            Log.e("TAG", "" + dataSnapshot.getValue()); // your name values you will get here
                            delURL[0] = (String) dataSnapshot.getValue();
                            StorageReference photoRef = storage.getReferenceFromUrl(delURL[0]);
                            photoRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    // File deleted successfully
                                    //Toast.makeText(ItemViewActivity.this, "File deleted successfully", Toast.LENGTH_SHORT).show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    // Uh-oh, an error occurred!
                                    //Toast.makeText(ItemViewActivity.this, "Uh-oh, an error occurred!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        Log.e("TAG", " it's null.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        items.child(key).removeValue();
    }

    private void showUpdateFoodDialog(final String key, final Items item) {

        imageviewItem = item;
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(ItemViewActivity.this);
        alertDialog.setTitle("Update Item");
        alertDialog.setMessage("Please fill full information");

        LayoutInflater inflater = this.getLayoutInflater();
        View add_menu_layout = inflater.inflate(R.layout.add_new_item, null);

        edtName = add_menu_layout.findViewById(R.id.edt_Name);
        edtImage = add_menu_layout.findViewById(R.id.edtImage);
        btnSelect = add_menu_layout.findViewById(R.id.btnSelect);


        //Set defalut values for view
        edtName.setText(item.getName());
        Picasso.get().load(item.getImage()).into(edtImage);

        ////Event for button
        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage(); // let user to select image from Gallery and save Uri of image
            }
        });


        alertDialog.setView(add_menu_layout);
        //alertDialog.setIcon(R.drawable.ic_shopping_cart_black_24dp);

        //Set Button
        alertDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                String name = edtName.getText().toString();
                if (!name.isEmpty()) {

//                    item.setName(name);
//                    items.child(key).setValue(item);
                    uploadImage();

                } else {
                    Toast.makeText(ItemViewActivity.this, "Name cannot be Empty..!!!", Toast.LENGTH_SHORT).show();
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


                final StorageReference imageFolder = storageReference.child("images/").child(Common.currentUser.getuPhone() + "_" + uri.getLastPathSegment());

                UploadTask uploadTask = imageFolder.putBytes(final_image);


                //imageFolder.putFile(picUri)
                uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        mDialog.dismiss();
                        //Toast.makeText(ItemViewActivity.this, "Uploaded succesfully", Toast.LENGTH_SHORT).show();
                        imageFolder.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {

                                // set value for newCategory and we can get download link
                                newItem = new Items(edtName.getText().toString(), uri.toString());
                                //we create new category
                                if (newItem != null) {
                                    items.push().setValue(newItem);
                                    // deleteFood(adapter.getRef(myItem.getOrder()).getKey());
                                    actualImage.delete();
                                    Snackbar.make(findViewById(R.id.itemview), "New Item " + newItem.getName() + " was added", Snackbar.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                mDialog.dismiss();
                                Toast.makeText(ItemViewActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        })
                ;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "Please Click the image first..!!!", Toast.LENGTH_LONG).show();
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

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        return false;
    }

    @Override
    public void onBackPressed() {
        int random = ThreadLocalRandom.current().nextInt(20, 40);
        int newRandom = random * 1000;
        //Toast.makeText(ItemViewActivity.this, "Item View Add Back pressed", Toast.LENGTH_SHORT).show();
        new CountDownTimer(newRandom, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                //Toast.makeText(ItemViewActivity.this, "Main View Add", Toast.LENGTH_SHORT).show();

                showInterstitial();
            }
        }.start();
        super.onBackPressed();

    }

    public void showInterstitial() {
        if (iInterstitialAd.isLoaded()) {
            if (counter < 1) {
                iInterstitialAd.show();
                counter++;
            }

        } else {
            Log.d("InterstitalAd", "The interstitial wasn't loaded yet.");
        }
    }
}

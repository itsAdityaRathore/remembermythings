package com.aditya.remembermythings.Activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.cardview.widget.CardView;

import com.aditya.remembermythings.Common.Common;
import com.aditya.remembermythings.Model.Items;
import com.aditya.remembermythings.Model.User;
import com.aditya.remembermythings.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.paperdb.Paper;


public class MainActivity extends AppCompatActivity {

    GridLayout mainGrid;
    String uPhone;

    Items newItem;

    //Add new menu Layout
    AppCompatEditText edtName;
    AppCompatImageView edtImage;
    AppCompatButton btnUpload,btnSelect;

    //Firebase
    FirebaseDatabase database;
    DatabaseReference items,userItem;
    FirebaseStorage storage;
    StorageReference storageReference;

    Uri picUri;
    private static final int CAPTURE_IMAGE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        if (getIntent().hasExtra("uPhone")){
            uPhone = getIntent().getStringExtra("uPhone");
            Toast.makeText(this, "User = "+uPhone, Toast.LENGTH_SHORT).show();
        }

        //Init Firebase
        database = FirebaseDatabase.getInstance();
        items = database.getReference("Items");
        userItem = items.child(""+uPhone);
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        mainGrid = (GridLayout) findViewById(R.id.mainGrid);
        //Set Event
        setSingleEvent(mainGrid);
        //setToggleEvent(mainGrid);

        //INIT paper
        Paper.init(this);
    }

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

                    Toast.makeText(MainActivity.this, "Clicked = "+finalI, Toast.LENGTH_SHORT).show();
                    if(finalI == 0){
                        showDialog();
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
        View add_item_layout = inflater.inflate(R.layout.add_new_item,null);

        edtName = add_item_layout.findViewById(R.id.edtName);
        edtImage = add_item_layout.findViewById(R.id.edtImage);
        btnSelect = add_item_layout.findViewById(R.id.btnSelect);

        ////Event for button
        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage(); // let user to select image from Gallery and save Uri of image
            }
        });

//        btnUpload.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//               // uploadImage();
//            }
//        });

        alertDialog.setView(add_item_layout);
        //alertDialog.setIcon(R.drawable.ic_shopping_cart_black_24dp);

        //Set Button
        alertDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                String name = edtName.getText().toString();
                if(!name.isEmpty()){
                    uploadImage();
//                    //we create new category
//                    if(newItem != null)
//                    {
//                        userItem.push().setValue(newItem);
//                        // Snackbar.make(drawer,"New Item "+newItem.getName()+" was added",Snackbar.LENGTH_SHORT).show();
//                        Toast.makeText(MainActivity.this, "New Item "+newItem.getName()+" was added", Toast.LENGTH_SHORT).show();
//                    }else{
//                        Toast.makeText(MainActivity.this, "Empty..!!!", Toast.LENGTH_SHORT).show();
//                    }
                }
                else{
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
        i.putExtra(MediaStore.EXTRA_OUTPUT,picUri); // set the image file

        startActivityForResult(i, CAPTURE_IMAGE);

    }

    @Override
    protected  void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);

        Uri uri = picUri;
        Bitmap imageBitmap=null;

        try {
            imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), picUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(imageBitmap!=null) {
            btnSelect.setText("Image Selected");
            edtImage.setImageBitmap(imageBitmap);
        }

    }

    /** Create a File for saving an image */
    private File getOutputMediaFile(int type){
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "CameraApp");

        /**Create the storage directory if it does not exist*/
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }

        /**Create a media file name*/
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == 1){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".png");
        } else {
            return null;
        }
        return mediaFile;
    }



   /* @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == Common.PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null)
        {
            saveUri = data.getData();
            btnSelect.setText("Image Selected");
        }
    }
*/
    private void uploadImage() {
        //Toast.makeText(this, "Im in Upload", Toast.LENGTH_SHORT).show();
        if(picUri != null)
        {
            final ProgressDialog mDialog = new ProgressDialog(this);
            mDialog.setMessage("Uploading...");
            mDialog.show();
            Uri uri = picUri;
            //String imageName = UUID.randomUUID().toString();
            //final StorageReference imageFolder = storageReference.child("images/"+imageName);
            final StorageReference imageFolder = storageReference.child("images/").child(uri.getLastPathSegment());
            imageFolder.putFile(picUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            mDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Uploaded succesfully", Toast.LENGTH_SHORT).show();
                            imageFolder.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    // set value for newCategory and we can get download link
                                    newItem = new Items(edtName.getText().toString(),uri.toString());
                                    //we create new category
                                    if(newItem != null)
                                    {
                                        userItem.push().setValue(newItem);
                                        // Snackbar.make(drawer,"New Item "+newItem.getName()+" was added",Snackbar.LENGTH_SHORT).show();
                                        Toast.makeText(MainActivity.this, "New Item "+newItem.getName()+" was added", Toast.LENGTH_SHORT).show();
                                    }else{
                                        Toast.makeText(MainActivity.this, "Empty..!!!", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            mDialog.dismiss();
                            Toast.makeText(MainActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            int progress = (100 * (int)taskSnapshot.getBytesTransferred() / (int)taskSnapshot.getTotalByteCount());
                            mDialog.setMessage("Uploaded "+progress+" %");
                        }
                    });
        }
        else{
            Toast.makeText(this, "Please Click the image first..!!!", Toast.LENGTH_LONG).show();
        }

    }

}

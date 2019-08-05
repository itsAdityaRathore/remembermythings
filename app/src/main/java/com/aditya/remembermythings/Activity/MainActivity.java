package com.aditya.remembermythings.Activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.cardview.widget.CardView;
import androidx.drawerlayout.widget.DrawerLayout;

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

import java.util.UUID;

import io.paperdb.Paper;


public class MainActivity extends AppCompatActivity {

    GridLayout mainGrid;
    TextView category;

    Items newItem;

    //Add new menu Layout
    AppCompatEditText edtName;
    AppCompatButton btnUpload,btnSelect;

    //Firebase
    FirebaseDatabase database;
    DatabaseReference items,userItem;
    FirebaseStorage storage;
    StorageReference storageReference;
   // FirebaseRecyclerAdapter<Items,MenuViewHolder> adapter;



    Uri saveUri;
    DrawerLayout drawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Init Firebase
        database = FirebaseDatabase.getInstance();
        items = database.getReference("Items");
        userItem = items.child(User.class.getName());
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        mainGrid = (GridLayout) findViewById(R.id.mainGrid);
        //Set Event
        setSingleEvent(mainGrid);
        //setToggleEvent(mainGrid);

//       Intent intent = new Intent(this, LoginActivity.class);
//       startActivity(intent);
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
        btnSelect = add_item_layout.findViewById(R.id.btnSelect);
        btnUpload = add_item_layout.findViewById(R.id.btnUpload);

        ////Event for button
        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage(); // let user to select image from Gallery and save Uri of image
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadImage();
            }
        });

        alertDialog.setView(add_item_layout);
        //alertDialog.setIcon(R.drawable.ic_shopping_cart_black_24dp);

        //Set Button
        alertDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                //we create new category
                if(newItem != null)
                {
                    userItem.push().setValue(newItem);
                    //Snackbar.make(drawer,"New Item "+newItem.getName()+" was added",Snackbar.LENGTH_SHORT).show();
                    Toast.makeText(MainActivity.this, "New Item "+newItem.getName()+" was added", Toast.LENGTH_SHORT).show();
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

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent.createChooser(intent,"Select Image"),Common.PICK_IMAGE_REQUEST);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == Common.PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null)
        {
            saveUri = data.getData();
            btnSelect.setText("Image Selected");
        }
    }

    private void uploadImage() {
        Toast.makeText(this, "Im in Upload", Toast.LENGTH_SHORT).show();
        if(saveUri != null)
        {
            final ProgressDialog mDialog = new ProgressDialog(this);
            mDialog.setMessage("Uploading...");
            mDialog.show();

            String imageName = UUID.randomUUID().toString();
            final StorageReference imageFolder = storageReference.child("images/"+imageName);
            imageFolder.putFile(saveUri)
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
                            double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                            mDialog.setMessage("Uploaded "+progress+" %");
                        }
                    });
        }

    }

}

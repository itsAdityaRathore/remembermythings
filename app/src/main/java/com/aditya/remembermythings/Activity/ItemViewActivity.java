package com.aditya.remembermythings.Activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
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
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.BindView;
import id.zelory.compressor.Compressor;

public class ItemViewActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    TextView txtFullName;
    AppCompatImageView edtImage,viewFullImage;
    AppCompatButton btnSelect;

    Items newItem,imageviewItem;
    @BindView(R.id.edt_Name) EditText edtName;

    File mediaStorageDir;
    Uri picUri, saveUri;;
    private static final int CAPTURE_IMAGE = 0;


    //Firebase
    FirebaseDatabase database;
    DatabaseReference items,userItem;
    FirebaseStorage storage;
    StorageReference storageReference;
    FirebaseRecyclerAdapter<Items, ItemViewHolder> adapter;

    MenuItem myItem;
    //View
    RecyclerView recycler_menu;
    RecyclerView.LayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_view);

        //Init Firebase
        database = FirebaseDatabase.getInstance();
        items = database.getReference("Items").child(Common.currentUser.getuPhone());
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        //Init View
        recycler_menu = (RecyclerView)findViewById(R.id.recycler_menu);
        recycler_menu.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getApplicationContext());
        recycler_menu.setLayoutManager(layoutManager);

        if(items!=null)
            loadMenuNew();
        else
            Toast.makeText(getApplicationContext(), "You Have No Items To View", Toast.LENGTH_SHORT).show();
    }

    private void loadMenuNew() {
        adapter = new FirebaseRecyclerAdapter<Items, ItemViewHolder>(
                Items.class,
                R.layout.view_item,
                ItemViewHolder.class,
                items
        )   {
            @Override
            protected void populateViewHolder(ItemViewHolder viewHolder, Items model, int position) {
                viewHolder.txtItemName.setText(model.getName());
                Picasso.get().load(model.getImage()).into(viewHolder.imageView);

                viewHolder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                        Toast.makeText(ItemViewActivity.this, "Long Press On The Item To Update/Delete", Toast.LENGTH_SHORT).show();
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
        alertDialog.setTitle("You can find your item here.");
        alertDialog.setIcon(R.drawable.view_property);
        LayoutInflater inflater = this.getLayoutInflater();
        View add_image_layout = inflater.inflate(R.layout.view_full_image,null);

        viewFullImage = add_image_layout.findViewById(R.id.viewFullImage);

        Picasso.get().load(item.getImage()).into(viewFullImage);

        alertDialog.setView(add_image_layout);

        alertDialog.show();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        myItem = item;

        if(item.getTitle().equals(Common.UPDATE))
        {
            showUpdateFoodDialog(adapter.getRef(item.getOrder()).getKey(),adapter.getItem(item.getOrder()));
        }
        else if (item.getTitle().equals(Common.DELETE))
        {
            deleteFood(adapter.getRef(item.getOrder()).getKey());
        }

        return super.onContextItemSelected(item);
    }

    private void deleteFood(String key) {
        items.child(key).removeValue();
    }

    private void showUpdateFoodDialog(final String key, final Items item) {

        imageviewItem = item;
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(ItemViewActivity.this);
        alertDialog.setTitle("Update Item");
        alertDialog.setMessage("Please fill full information");

        LayoutInflater inflater = this.getLayoutInflater();
        View add_menu_layout = inflater.inflate(R.layout.add_new_item,null);

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
                if(!name.isEmpty()){

//                    item.setName(name);
//                    items.child(key).setValue(item);
                    uploadImage();

                }
                else{
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

    private void uploadImage() {
        //Toast.makeText(this, "Im in Upload", Toast.LENGTH_SHORT).show();
        if(picUri != null) {
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
                        Toast.makeText(ItemViewActivity.this, "Uploaded succesfully", Toast.LENGTH_SHORT).show();
                        imageFolder.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {



                                // set value for newCategory and we can get download link
                                newItem = new Items(edtName.getText().toString(), uri.toString());
                                //we create new category
                                if (newItem != null) {
                                   items.push().setValue(newItem);
                                    deleteFood(adapter.getRef(myItem.getOrder()).getKey());
                                    //Snackbar.make(drawer,"New Item "+newItem.getName()+" was added", Snackbar.LENGTH_SHORT).show();
                                    Toast.makeText(ItemViewActivity.this, "New Item " + newItem.getName() + " was added", Toast.LENGTH_SHORT).show();
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
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                int progress = (100 * (int) taskSnapshot.getBytesTransferred() / (int) taskSnapshot.getTotalByteCount());
                                mDialog.setMessage("Uploaded " + progress + " %");
                            }
                        });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }   else{
            Toast.makeText(this, "Please Click the image first..!!!", Toast.LENGTH_LONG).show();
        }
    }

    /** Create a File for saving an image */
    private File getOutputMediaFile(int type){
        mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
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

  /*  private void loadMenu() {

//        Query query = FirebaseDatabase.getInstance()
//                .getReference("Items")
//                .limitToLast(50);

        FirebaseRecyclerOptions<Items> options =
                new FirebaseRecyclerOptions.Builder<Items>()
                        .setQuery(items, Items.class)
                        .build();

        adapter = new FirebaseRecyclerAdapter<Items, ItemViewHolder>(options){



            @Override
            public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.view_item, parent, false);

                return new ItemViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(ItemViewHolder itemViewHolder, int i, Items items) {
                itemViewHolder.txtItemName.setText(items.getName());
                Picasso.get().load(items.getImage()).into(itemViewHolder.imageView);
                //itemViewHolder.imageView.setImageURI();
            }
        };
        adapter.notifyDataSetChanged();
        recycler_menu.setAdapter(adapter);
    }
*/

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        return false;
    }
}

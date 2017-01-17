package com.example.melanieh.inventoryapp;

import android.Manifest;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.melanieh.inventoryapp.data.ProductContract;
import com.example.melanieh.inventoryapp.data.ProductDBHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * Created by melanieh on 10/30/16.
 */

public class EditProductActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener {

    /**
     * log tag
     */
    private static final String LOG_TAG = EditProductActivity.class.getSimpleName();
    /**
     * intent chooser strings
     */
    private static final int SELECT_PICTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 1;
    private static final String[] PERMISSIONS = {Manifest.permission.MANAGE_DOCUMENTS,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    SQLiteDatabase db;
    ProductDBHelper dbHelper;
    Cursor cursor;

    /**
     * projection for cursorloader calls
     */
    String[] projection = {ProductContract.ProductEntry.COLUMN_ID,
            ProductContract.ProductEntry.COLUMN_NAME,
            ProductContract.ProductEntry.COLUMN_QTY,
            ProductContract.ProductEntry.COLUMN_PRICE,
            ProductContract.ProductEntry.COLUMN_IMAGE_URI,
            ProductContract.ProductEntry.COLUMN_SUPPLIER_EMAIL};
    /**
     * UI views for displaying product data
     */
    EditText nameEditText;
    EditText qtyEditText;
    EditText priceEditText;
    EditText supplierEmailEditText;
    Button imageUploadBtn;
    ImageView uploadedImage;
    Uri selectedImageUri;
    private String selectedImageUriString;
    Bitmap bitmap;
    String decodedURI;

    String name;
    Integer qty;
    Double price;
    String suppEmail;
    ContentValues values;
    Uri currentProdUri;
    Uri productImageUri;
    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.v(LOG_TAG, "onCreate called...");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_product);

        // interactive UI views

        nameEditText = (EditText) findViewById(R.id.edit_name);
        qtyEditText = (EditText) findViewById(R.id.edit_quantity);
        priceEditText = (EditText) findViewById(R.id.edit_price);
        imageUploadBtn = (Button) findViewById(R.id.image_prompt);
        supplierEmailEditText = (EditText) findViewById(R.id.edit_supplier_email);
        uploadedImage = (ImageView) findViewById(R.id.uploaded_image);
        imageUploadBtn.setOnClickListener(this);
        dismissKeyboard(nameEditText);
        dismissKeyboard(qtyEditText);
        dismissKeyboard(priceEditText);
        dismissKeyboard(supplierEmailEditText);

        /** inbound intent data **/

        Intent getUri = getIntent();
        currentProdUri = getUri.getData();
        Log.v(LOG_TAG, "currentProdUri = " + currentProdUri);

        if (currentProdUri == null) {
            setTitle(getString(R.string.edit_appbar_add_product));
        } else {
            setTitle(getString(R.string.edit_appbar_edit_product));
            // if product image has been chosen, display it
            getSupportLoaderManager().initLoader(2, null, this);
        }
//        if (bitmap != null) {
//            try {
//                String decodedURI = java.net.URLDecoder.decode(selectedImageUriString, "UTF-8");
//                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), currentProdUri);
//                uploadedImage.setImageBitmap(bitmap);
//                uploadedImage.setImageURI(Uri.parse(decodedURI));
//            } catch (UnsupportedEncodingException e) {
//                Log.e(LOG_TAG, "", e);
//            } catch (IOException e) {
//                Log.e(LOG_TAG, "", e);
//            }
//        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        Log.v(LOG_TAG, "grantResults= " + grantResults);
//        if (requestCode == REQUEST_IMAGE_OPEN) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                Intent openImage = new Intent(Intent.ACTION_GET_CONTENT);
//                startActivityForResult(openImage, REQUEST_IMAGE_OPEN);
////            }
//            } else {
//                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//            }
//        }
//    }


    /**
     * displays the product image selected in the edit form and assigns the uri
     * for use by and storage in the DB
     **/
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            {
                if (requestCode == SELECT_PICTURE) {
                    selectedImageUri = data.getData();
                    Bitmap bitmap = getBitmapFromUri(selectedImageUri);
                    uploadedImage.setImageBitmap(bitmap);
////                    uploadedImage.setImageURI(selectedImageUri);
//                    selectedImageUriString = selectedImageUri.toString();
////                    data.putExtra(Intent.EXTRA_STREAM, selectedImageUri);
                }
            }
        }
    }

    @Override
    public void finish() {
        cursor.close();
    }

    /**
     * options menu
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit_product, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                saveProduct();
                break;
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                break;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        NavUtils.navigateUpFromSameTask(this);
    }

    /**
     * helper methods for inserting a new product or updating an existing product
     */
    private void saveProduct() {
        dbHelper = new ProductDBHelper(this);
        db = dbHelper.getWritableDatabase();

        name = nameEditText.getText().toString().trim();
        String qtyString = qtyEditText.getText().toString().trim();
//        qty = Integer.parseInt(qtyString);
        String priceString = priceEditText.getText().toString().trim();
        suppEmail = supplierEmailEditText.getText().toString().trim();

        /** user input validation */
        // name
        if (TextUtils.isEmpty(name)) {
            dismissKeyboard(nameEditText);
            Toast.makeText(this, getString(R.string.user_input_validation_name), Toast.LENGTH_SHORT);
            return;
        }
        // quantity (string and numerical values)
        if (TextUtils.isEmpty(qtyString)) {
            dismissKeyboard(qtyEditText);
            Toast.makeText(this, getString(R.string.user_input_validation_name), Toast.LENGTH_SHORT);
            return;
        }

        try {
            qty = Integer.parseInt(qtyString);
        } catch (NumberFormatException e) {
            dismissKeyboard(qtyEditText);
            Toast.makeText(this, getString(R.string.user_input_validation_qty), Toast.LENGTH_SHORT);
            return;
        }
        // price (string and numerical values)
        if (TextUtils.isEmpty(priceString)) {
            Toast.makeText(this, getString(R.string.user_input_validation_price), Toast.LENGTH_SHORT);
            return;
        }

        try {
            price = Double.valueOf(priceString);
            Toast.makeText(this, getString(R.string.user_input_validation_price), Toast.LENGTH_SHORT);
        } catch (NumberFormatException e) {
            return;
        }

        // supplier e-mail
        if (TextUtils.isEmpty(suppEmail)) {
            Toast.makeText(this, getString(R.string.user_input_validation_supplier_email), Toast.LENGTH_SHORT);
            return;
        }
        // no user validation for image as image is not required but user can save anyway

        values = new ContentValues();
        values.put(ProductContract.ProductEntry.COLUMN_NAME, name);
        values.put(ProductContract.ProductEntry.COLUMN_QTY, qty);
        values.put(ProductContract.ProductEntry.COLUMN_PRICE, price);
        values.put(ProductContract.ProductEntry.COLUMN_IMAGE_URI, selectedImageUri.toString());
        values.put(ProductContract.ProductEntry.COLUMN_SUPPLIER_EMAIL, suppEmail);

        if (currentProdUri == null) {
            getContentResolver().insert(ProductContract.ProductEntry.PRODUCTS_CONTENT_URI, values);
        } else {
            getContentResolver().update(currentProdUri, values, null, null);
        }

    }

    private void showDeleteConfirmationDialog() {

        AlertDialog.Builder deleteConfADBuilder = new AlertDialog.Builder(this);
        deleteConfADBuilder.setMessage(getString(R.string.deleteConf_dialog_msg));

        // positive button=yes, delete all products
        DialogInterface.OnClickListener yesButtonListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                deleteProduct();
            }
        };

        // negative button=no, keep editing, dismiss dialog
        DialogInterface.OnClickListener noButtonListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (dialogInterface != null) {
                    dialogInterface.dismiss();
                }
            }
        };

        String yesString = getString(R.string.delete_dialog_yes);
        String noString = getString(R.string.delete_dialog_no);
        deleteConfADBuilder.setPositiveButton(yesString, yesButtonListener);
        deleteConfADBuilder.setNegativeButton(noString, noButtonListener);

    }

    private void deleteProduct() {
        db = dbHelper.getWritableDatabase();
        getContentResolver().delete(currentProdUri, null, null);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, currentProdUri, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data == null || data.getCount() < 1) {
            return;
        }

        data.moveToFirst();
        int idColIndex = data.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_ID);
        int nameColIndex = data.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_NAME);
        Log.v(LOG_TAG, "nameIndex= " + nameColIndex);
        int qtyColIndex = data.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_QTY);
        Log.v(LOG_TAG, "qtyColIndex= " + qtyColIndex);
        int priceColIndex = data.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_PRICE);
        Log.v(LOG_TAG, "priceIndex = " + priceColIndex);
        int imageUriColIndex = data.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_IMAGE_URI);
        Log.v(LOG_TAG, "imageUriIndex= " + imageUriColIndex);
        int suppEmailColIndex = data.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_SUPPLIER_EMAIL);
        Log.v(LOG_TAG, "suppEmail index = " + suppEmailColIndex);

        Integer id = data.getInt(idColIndex);
        String name = data.getString(nameColIndex);
        Integer qty = data.getInt(qtyColIndex);
        String quantityString = String.valueOf(qty);
        Double price = data.getDouble(priceColIndex);
        String priceString = String.valueOf(price);
        String selectedImageUriString = data.getString(imageUriColIndex);
        String suppEmail = data.getString(suppEmailColIndex);

        nameEditText.setText(name);
        qtyEditText.setText(quantityString);
        priceEditText.setText(priceString);
        supplierEmailEditText.setText(suppEmail);
        uploadedImage.setVisibility(View.VISIBLE);
        Bitmap bitmap = getBitmapFromUri(selectedImageUri);
        uploadedImage.setImageBitmap(bitmap);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.v(LOG_TAG, "onLoaderReset:");
        nameEditText.setText("");
        qtyEditText.setText("");
        priceEditText.setText("");
        supplierEmailEditText.setText("");
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);
    }

    public Bitmap getBitmapFromUri(Uri uri) {

        if (uri == null || uri.toString().isEmpty())
            return null;

        // Get the dimensions of the View
        int targetW = uploadedImage.getWidth();
        int targetH = uploadedImage.getHeight();

        InputStream input = null;
        try {
            input = this.getContentResolver().openInputStream(uri);

            // Get the dimensions of the bitmap
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();

            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            // Determine how much to scale down the image
            int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inPurgeable = true;

            input = this.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();
            return bitmap;

        } catch (FileNotFoundException fne) {
            Log.e(LOG_TAG, "Failed to load image.", fne);
            return null;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to load image.", e);
            return null;
        } finally {
            try {
                input.close();
            } catch (IOException ioe) {

            }
        }
    }

    private void dismissKeyboard(EditText view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}









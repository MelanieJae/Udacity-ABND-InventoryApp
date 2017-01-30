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
import android.os.Build;
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
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.melanieh.inventoryapp.data.ProductContract;
import com.example.melanieh.inventoryapp.data.ProductDBHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by melanieh on 10/30/16.
 */

public class EditProductActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener {

    /***
     * log tag
     */
    private static final String LOG_TAG = EditProductActivity.class.getSimpleName();

    /***
     * intent action strings
     */
    private static final int SELECT_PICTURE = 1;
    private static final int REQUEST_IMAGE_OPEN = 2;

    SQLiteDatabase db;
    ProductDBHelper dbHelper;
    Cursor cursor;

    /***
     * projection for cursorloader calls
     */
    String[] projection = {ProductContract.ProductEntry.COLUMN_ID,
            ProductContract.ProductEntry.COLUMN_NAME,
            ProductContract.ProductEntry.COLUMN_QTY,
            ProductContract.ProductEntry.COLUMN_PRICE,
            ProductContract.ProductEntry.COLUMN_IMAGE_URI,
            ProductContract.ProductEntry.COLUMN_SUPPLIER_EMAIL};


    /***
     * UI views for displaying product data
     */

    @BindView(R.id.edit_name)
    EditText nameEditText;
    @BindView(R.id.edit_quantity)
    EditText qtyEditText;
    @BindView(R.id.edit_price)
    EditText priceEditText;
    @BindView(R.id.edit_supplier_email)
    EditText supplierEmailEditText;
    @BindView(R.id.image_prompt)
    Button imageUploadBtn;
    @BindView(R.id.uploaded_image)
    ImageView uploadedImage;

    Uri selectedImageUri;

    String name;
    Integer qty;
    Double price;
    String suppEmail;
    ContentValues values;
    Uri currentProdUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.v(LOG_TAG, "onCreate called...");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_product);

        // interactive UI views
        ButterKnife.bind(this);
        dismissKeyboard(nameEditText);
        dismissKeyboard(qtyEditText);
        dismissKeyboard(priceEditText);
        dismissKeyboard(supplierEmailEditText);

        /** inbound intent data **/

        Intent getUri = getIntent();
        currentProdUri = getUri.getData();
        Log.v(LOG_TAG, "currentProdUri = " + currentProdUri);

        ViewTreeObserver viewTreeObserver = uploadedImage.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                uploadedImage.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                uploadedImage.setImageBitmap(getBitmapFromUri(currentProdUri));
            }
        });

        imageUploadBtn.setOnClickListener(this);

        if (currentProdUri == null) {
            setTitle(getString(R.string.edit_appbar_add_product));
        } else {
            setTitle(getString(R.string.edit_appbar_edit_product));
            openImageSelector();
            // if product image has been chosen, display it
            getSupportLoaderManager().initLoader(2, null, this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    /**
     * displays the product image selected in the edit form and assigns the uri
     * for use by and storage in the DB
     **/
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            {
                if (requestCode == SELECT_PICTURE || requestCode == REQUEST_IMAGE_OPEN) {
                    selectedImageUri = data.getData();
                    Bitmap bitmap = getBitmapFromUri(selectedImageUri);
                    uploadedImage.setImageBitmap(bitmap);
                }
            }
        }
    }

    @Override
    public void finish() {
        cursor.close();
    }

    public void openImageSelector() {
        Intent intent;
        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }

        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_IMAGE_OPEN);
    }

    /***
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
        ButterKnife.bind(this);
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
                    return;
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
        int numRowsDeleted = getContentResolver().delete(currentProdUri, null, null);
        if (numRowsDeleted == 0) {
            Toast.makeText(EditProductActivity.this,
                    getString(R.string.edit_error_deleting_product), Toast.LENGTH_LONG).show();

        } else {
            Toast.makeText(EditProductActivity.this,
                    getString(R.string.edit_product_deletion_successful), Toast.LENGTH_LONG).show();
        }
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
        int nameColIndex = data.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_NAME);
        int qtyColIndex = data.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_QTY);
        int priceColIndex = data.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_PRICE);
        int imageUriColIndex = data.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_IMAGE_URI);
        int suppEmailColIndex = data.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_SUPPLIER_EMAIL);

        String name = data.getString(nameColIndex);
        Integer qty = data.getInt(qtyColIndex);
        String quantityString = String.valueOf(qty);
        Double price = data.getDouble(priceColIndex);
        String priceString = String.valueOf(price);
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

    private void dismissKeyboard(EditText view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public Bitmap getBitmapFromUri(Uri uri) {
        if (uri == null || uri.toString().isEmpty())
            return null;

        // Get the dimensions of the View
        int targetW = uploadedImage.getWidth();
        int targetH = uploadedImage.getHeight();

        InputStream input = new InputStream() {
            @Override
            public int read() throws IOException {
                return 0;
            }
        };
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
}









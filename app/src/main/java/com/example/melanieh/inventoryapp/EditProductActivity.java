package com.example.melanieh.inventoryapp;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.melanieh.inventoryapp.data.ProductContract;
import com.example.melanieh.inventoryapp.data.ProductDBHelper;

/**
 * Created by melanieh on 10/30/16.
 */

public class EditProductActivity extends AppCompatActivity {


    /**
     * log tag
     */
    private static final String LOG_TAG = EditProductActivity.class.getSimpleName();
    /**
     * intent chooser strings
     */
    private static final int SELECT_PICTURE = 1;
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
     * UI views for adding product data or populating from a cursor
     */
    EditText nameEditText;
    EditText qtyEditText;
    EditText priceEditText;
    EditText supplierEmailEditText;
    Button imageUploadBtn;
    ImageView uploadedImage;
    Uri selectedImageUri;
    /****
     * content values for product attributes
     */
    String name;
    Integer qty;
    Double price;
    String suppEmail;
    ContentValues values;
    ContentValues updatedValues;
    Uri currentProdUri;
    /****
     * CRUD method variables
     */
    String productTable = ProductContract.ProductEntry.PRODUCT_TABLE_NAME;
    String shipmentTable = ProductContract.ShipmentEntry.SHIPMENT_TABLE_NAME;
    private String productImagePath;
    private boolean productHasChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.v(LOG_TAG, "onCreate called...");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_product);


        dbHelper = new ProductDBHelper(this);

        // interactive UI views

        nameEditText = (EditText) findViewById(R.id.edit_name);
        qtyEditText = (EditText) findViewById(R.id.edit_quantity);
        priceEditText = (EditText) findViewById(R.id.edit_price);
        imageUploadBtn = (Button) findViewById(R.id.image_prompt);
        supplierEmailEditText = (EditText) findViewById(R.id.edit_supplier_email);
        uploadedImage = (ImageView) findViewById(R.id.uploaded_image);


        /** inbound intent data **/

        Intent getUri = getIntent();
        currentProdUri = getUri.getData();
        Log.v(LOG_TAG, "currentProdUri = " + currentProdUri);

        if (currentProdUri == null) {
            setTitle(getString(R.string.edit_appbar_add_product));
        } else {
            setTitle(getString(R.string.edit_appbar_edit_product));
            updateProduct();

        }

//      /** click listener for image upload button */
        imageUploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);
            }
        });


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
                if (requestCode == SELECT_PICTURE) {
                    Uri selectedImageUri = data.getData();
                    productImagePath = selectedImageUri.getPath();
                    System.out.println("Image Path : " + productImagePath);
                    uploadedImage.setVisibility(View.VISIBLE);
                    uploadedImage.setImageURI(selectedImageUri);
                    data.putExtra(Intent.EXTRA_STREAM, selectedImageUri);

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
                if (currentProdUri == null) {
                    insertProduct();

                }
            case R.id.action_delete:
                showDeleteConfirmationDialog();
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
    private Uri insertProduct() {
        price = 0.0;
        qty = 0;
        ProductDBHelper dbHelper = new ProductDBHelper(this);

        db = dbHelper.getWritableDatabase();

        name = nameEditText.getText().toString().trim();
        qty = Integer.parseInt(qtyEditText.getText().toString().trim());
        price = Double.valueOf(priceEditText.getText().toString().trim());
        suppEmail = supplierEmailEditText.getText().toString().trim();
        imageUploadBtn.setText("Upload New Image");


        // check if any of these values from the input fields are blank
        if (TextUtils.isEmpty(name) && (qty == null)
                && (price == null || TextUtils.isEmpty(suppEmail))
                ) {
            finish();
        }

        ContentValues values = new ContentValues();
        values.put(ProductContract.ProductEntry.COLUMN_NAME, name);
        values.put(ProductContract.ProductEntry.COLUMN_QTY, qty);
        values.put(ProductContract.ProductEntry.COLUMN_PRICE, price);
        values.put(ProductContract.ProductEntry.COLUMN_IMAGE_URI, productImagePath);
        values.put(ProductContract.ProductEntry.COLUMN_SUPPLIER_EMAIL, suppEmail);

        long newRowId = db.insert(ProductContract.ProductEntry.PRODUCT_TABLE_NAME, null, values);

        if (newRowId == -1) {
            Toast.makeText(EditProductActivity.this, getString(R.string.error_inserting_product),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(EditProductActivity.this, getString(R.string.insert_product_successful),
                    Toast.LENGTH_SHORT).show();
        }

        // Return the new URI with the ID (of the newly inserted row) appended at the end
        return ContentUris.withAppendedId(ProductContract.ProductEntry.PRODUCTS_CONTENT_URI, newRowId);
    }


    private int updateProduct() {

        db = dbHelper.getWritableDatabase();
        int numRowsUpdated = 0;

        /** first, populate fields if existing product */
        Log.v(LOG_TAG, "populateEditFields called...");

        String selection = ProductContract.ProductEntry.COLUMN_ID + "=?";
        String[] selectionArgs = new String[]{String.valueOf(ContentUris.parseId(currentProdUri))};
        Log.v(LOG_TAG, "selectionArgs=" + selectionArgs);
        cursor = db.query(productTable, projection, selection, selectionArgs, null, null, null);

        while (cursor.moveToNext()) {
            int nameColIndex = cursor.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_NAME);
            Log.v(LOG_TAG, "nameIndex= " + nameColIndex);
            int qtyColIndex = cursor.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_QTY);
            Log.v(LOG_TAG, "qtyColIndex= " + qtyColIndex);
            int priceColIndex = cursor.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_PRICE);
            Log.v(LOG_TAG, "priceIndex = " + priceColIndex);
            int imageUriColIndex = cursor.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_IMAGE_URI);
            Log.v(LOG_TAG, "imageUriIndex= " + imageUriColIndex);
            int suppEmailColIndex = cursor.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_SUPPLIER_EMAIL);
            Log.v(LOG_TAG, "suppEmail index = " + suppEmailColIndex);

            name = cursor.getString(nameColIndex);
            qty = cursor.getInt(qtyColIndex);
            String quantityString = String.valueOf(qty);
            price = cursor.getDouble(priceColIndex);
            String priceString = String.valueOf(price);
            suppEmail = cursor.getString(suppEmailColIndex);

            nameEditText.setText(name);
            qtyEditText.setText(quantityString);
            priceEditText.setText(priceString);
            supplierEmailEditText.setText(suppEmail);
            uploadedImage.setImageURI(selectedImageUri);


            /** these are fed/populated by the new inputs from the user */

            updatedValues = new ContentValues();

            // check for no values updated case; exit method at this point
            if (updatedValues.size() == 0) {
                return numRowsUpdated;
            } else {

                updatedValues.put(ProductContract.ProductEntry.COLUMN_NAME, name);
                updatedValues.put(ProductContract.ProductEntry.COLUMN_QTY, qty);
                updatedValues.put(ProductContract.ProductEntry.COLUMN_PRICE, price);
                updatedValues.put(ProductContract.ProductEntry.COLUMN_IMAGE_URI, productImagePath);
                updatedValues.put(ProductContract.ProductEntry.COLUMN_SUPPLIER_EMAIL, suppEmail);

                numRowsUpdated = db.update(productTable, updatedValues, null, null);

                if (numRowsUpdated == 0) {
                    Toast.makeText(EditProductActivity.this, getString(R.string.error_updating_product),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(EditProductActivity.this, getString(R.string.product_update_successful),
                            Toast.LENGTH_SHORT).show();
                }

            }
        }
        return numRowsUpdated;
    }


    private void showDeleteConfirmationDialog() {

        AlertDialog.Builder deleteConfADBuilder = new AlertDialog.Builder(this);
        deleteConfADBuilder.setMessage(getString(R.string.deleteConf_dialog_msg));

        // clicklisteners for buttons
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
        int numRowsDeleted = db.delete(productTable, null, null);

        if (numRowsDeleted == 0) {
            Toast.makeText(EditProductActivity.this, getString(R.string.edit_error_deleting_product),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(EditProductActivity.this, getString(R.string.edit_product_deletion_successful),
                    Toast.LENGTH_SHORT).show();
        }
    }

}








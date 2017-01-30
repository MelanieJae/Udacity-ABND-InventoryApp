package com.example.melanieh.inventoryapp;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.melanieh.inventoryapp.data.ProductContract;
import com.example.melanieh.inventoryapp.data.ProductDBHelper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * Created by melanieh on 10/30/16.
 */

public class DetailActivity extends AppCompatActivity {

    Cursor cursor;
    Uri productUri;
    int currentQty;
    ContentValues contentValues;
    SQLiteDatabase db;
    ProductDBHelper dbHelper;
    String name;
    String suppEmail;
    Double price;
    String priceString;
    int delta;
    String selectedImageUriString;
    Integer numRowsUpdated;
    Intent getProductData;

    String productTable = ProductContract.ProductEntry.TABLE_NAME;
    String shipmentTable = ProductContract.ShipmentEntry.TABLE_NAME;

    /****
     * log tag
     */
    private static final String LOG_TAG = DetailActivity.class.getSimpleName();

    /****
     * intent request codes, result codes and strings
     */
    private static final int REQUEST_IMAGE_OPEN = 1;

    /****
     * projection/columns array for cursor
     */
    String[] productsProjection = {
            ProductContract.ProductEntry.COLUMN_ID,
            ProductContract.ProductEntry.COLUMN_NAME,
            ProductContract.ProductEntry.COLUMN_QTY,
            ProductContract.ProductEntry.COLUMN_PRICE,
            ProductContract.ProductEntry.COLUMN_IMAGE_URI,
            ProductContract.ProductEntry.COLUMN_SUPPLIER_EMAIL
    };

    @BindView(R.id.name) TextView nameView;
    @BindView(R.id.price) TextView priceView;
    @BindView(R.id.quantity_heading) TextView qtyHeadingView;
    @BindView(R.id.qty_down_arrow) ImageButton downArrow;
    @BindView(R.id.qty_edit_field) EditText currentQtyView;
    @BindView(R.id.qty_up_arrow) ImageButton upArrow;
    @BindView(R.id.update_qty_shipment) Button updateFromShipment;
    @BindView(R.id.order_more) Button orderMoreBtn;
    @BindView(R.id.product_img) ImageView productImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.product_detail);
        ButterKnife.bind(this);
        dbHelper = new ProductDBHelper(this);

        /** inbound intent data from Catalog Activity **/
        getProductData = getIntent();
        productUri = getProductData.getData();
        if (productUri == null) {
            return;
        } else {
            openImageSelector();
            displayProductDetails();
        }

        ViewTreeObserver viewTreeObserver = productImageView.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                productImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                productImageView.setImageBitmap(getBitmapFromUri(productUri));
            }
        });

        upArrow.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                dismissKeyboard(currentQtyView);
                delta = 1;
                updateQty(currentQty + delta);
            }
        });

        downArrow.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                dismissKeyboard(currentQtyView);
                if (currentQty >= 1) {
                    delta = -1;
                    updateQty(currentQty + delta);
                }
                if (currentQty <= 0) {
                    currentQty = 0;
                    showOrderMoreDialog();
                }
            }
        });

        // click "update qty from shipment" to pull dummy product and quantity data from shipment table
        updateFromShipment.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                dismissKeyboard(currentQtyView);
                delta = addToQtyFromShipment();
                updateQty(currentQty + delta);

            }
        });

        // contact supplier to order more product
        orderMoreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendSupplierEmail();
            }
        });
    }

    @Override
    public void finish() {
        cursor.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_product_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                saveQtyUpdate();
                break;
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                break;
            case R.id.action_edit:
                editProductInfo();
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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            {
                if (requestCode == REQUEST_IMAGE_OPEN) {
                    Uri selectedImageUri = data.getData();
                    productImageView.setImageBitmap(getBitmapFromUri(selectedImageUri));
                }
            }
        }
    }

    private void showDeleteConfirmationDialog() {

        AlertDialog.Builder deleteConfADBuilder = new AlertDialog.Builder(this);
        deleteConfADBuilder.setMessage(getString(R.string.product_deleteConf_dialog_msg));

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
                    return;
                }
            }
        };

        String yesString = getString(R.string.delete_dialog_yes);
        String noString = getString(R.string.delete_dialog_no);
        deleteConfADBuilder.setPositiveButton(yesString, yesButtonListener);
        deleteConfADBuilder.setNegativeButton(noString, noButtonListener);

        deleteConfADBuilder.create();
        deleteConfADBuilder.show();
    }

    private void deleteProduct() {
        db = dbHelper.getWritableDatabase();
        String whereClause = ProductContract.ProductEntry.COLUMN_ID + "=?";
        String[] whereArgs = {String.valueOf(ContentUris.parseId(productUri))};
        Log.v(LOG_TAG, "whereClause= " + whereClause + " ; whereArgs= " + whereArgs);
        int numRowsDeleted = db.delete(productTable, whereClause, whereArgs);

        if (numRowsDeleted == 0) {
            Toast.makeText(DetailActivity.this, getString(R.string.detail_error_deleting_product),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(DetailActivity.this, getString(R.string.detail_product_deletion_successful),
                    Toast.LENGTH_SHORT).show();
        }

    }

    private void editProductInfo() {
        Log.v(LOG_TAG, "editProductInfo: productUri = " + productUri);
        Intent editProduct = new Intent(DetailActivity.this, EditProductActivity.class);
        editProduct.setData(productUri);
        startActivity(editProduct);
    }

    private void showOrderMoreDialog() {
        Log.v(LOG_TAG, "orderMoreDialog:");
        String yesString = getString(R.string.supp_email_dialog_yes);
        String noString = getString(R.string.supp_email_dialog_no);

        DialogInterface.OnClickListener yesButtonListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                sendSupplierEmail();
            }
        };

        DialogInterface.OnClickListener noButtonListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                return;
            }
        };

        AlertDialog.Builder supplierEmailADBuilder = new AlertDialog.Builder(this);
        supplierEmailADBuilder.setMessage(getString(R.string.supp_email_dialog_msg));
        supplierEmailADBuilder.setPositiveButton(yesString, yesButtonListener);
        supplierEmailADBuilder.setNegativeButton(noString, noButtonListener);

        supplierEmailADBuilder.create();
        supplierEmailADBuilder.show();
    }

    private void displayProductDetails() {
        // query DB to retrieve product details
        db = dbHelper.getWritableDatabase();
        // initiate ContentValues object to place updated product info, e.g. update quantity
        Log.v(LOG_TAG, "productUri =" + productUri);
        String prodSelection = ProductContract.ProductEntry.COLUMN_ID + "=?";
        String[] prodSelectionArgs = new String[]{String.valueOf(ContentUris.parseId(productUri))};
        cursor = getContentResolver().query(productUri, productsProjection, prodSelection,
                prodSelectionArgs, null);

        // stops query if cursor is empty and, if not, retrieves attribute values
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }
        cursor.moveToFirst();
        int idColIndex = cursor.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_ID);
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

        int id = cursor.getInt(idColIndex);
        Log.v(LOG_TAG, "id= " + id);
        name = cursor.getString(nameColIndex);
        Log.v(LOG_TAG, "name= " + name);
        int quantity = cursor.getInt(qtyColIndex);
        Log.v(LOG_TAG, "quantity= " + quantity);
        price = cursor.getDouble(priceColIndex);
        priceString = formatPrice(price);
        Log.v(LOG_TAG, "price= " + priceString);
        selectedImageUriString = cursor.getString(imageUriColIndex);
        Log.v(LOG_TAG, "selectedImageUriString= " + selectedImageUriString);

        suppEmail = cursor.getString(suppEmailColIndex);
        Log.v(LOG_TAG, "suppEmail= " + suppEmail);

        nameView.setText(name);
        priceView.setText(priceString);
        currentQtyView.setText(String.valueOf(quantity));
        if (quantity >= 0) {
            updateQty(quantity + delta);
        }

        if (quantity == 0) {
            showOrderMoreDialog();
        }

        if (selectedImageUriString == null) {
            return;
        }
        Bitmap bitmap = getBitmapFromUri(Uri.parse(selectedImageUriString));
        productImageView.setImageBitmap(bitmap);
    }

    private void updateQty(int qty) {
        Log.v(LOG_TAG, "updateQty called...");
        Log.v(LOG_TAG, "ship qty=" + qty);

        currentQtyView = (EditText) findViewById(R.id.qty_edit_field);

        if (qty < 0) {
            qty = 0;
            showOrderMoreDialog();
        }
        currentQtyView.setText(String.valueOf(qty));
        currentQty = qty;
        Log.v(LOG_TAG, "currentQty= " + currentQty);
    }

    public int addToQtyFromShipment() {
        Log.v(LOG_TAG, "pullQtyFromShipment called...");
        // dummy shipment data to illustrate functionality of 'update from shipment' button
        db = dbHelper.getWritableDatabase();
        ContentValues shipValues = new ContentValues();
        shipValues.put(ProductContract.ShipmentEntry.COLUMN_INCOMING_PROD, "seeds");
        shipValues.put(ProductContract.ShipmentEntry.COLUMN_INCOMING_QTY, 5);
        int shipdeltaqty = shipValues.getAsInteger(ProductContract.ShipmentEntry.COLUMN_INCOMING_QTY);
        Log.v(LOG_TAG, "shipdeltaqty=" + shipdeltaqty);
        long newRowId = db.insert(shipmentTable, null, shipValues);
        Log.v(LOG_TAG, "newRowId= " + newRowId);
        return shipdeltaqty;
    }

    private void dismissKeyboard(EditText view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void sendSupplierEmail() {
        String subject = getString(R.string.supplier_email_subject);
        String emailBodyText = "Product Name: " + name +
                "\nProduct Price: " + priceString + "\nQty: ";

        Intent sendEmail = new Intent(Intent.ACTION_SENDTO);
        sendEmail.setType("*/*");
        sendEmail.setData(Uri.parse("mailto:" + suppEmail));
        sendEmail.putExtra(Intent.EXTRA_SUBJECT, subject);
        sendEmail.putExtra(Intent.EXTRA_TEXT, emailBodyText);
        startActivity(sendEmail);
    }

    private String formatPrice(double number) {
        java.text.NumberFormat formatter = java.text.NumberFormat.getCurrencyInstance();
        return formatter.format(number);
    }

    private void saveQtyUpdate() {
        contentValues = new ContentValues();
        contentValues.put(ProductContract.ProductEntry.COLUMN_QTY, currentQty);
        String whereClause = ProductContract.ProductEntry.COLUMN_ID + "=?";
        String[] whereArgs = new String[]{String.valueOf(ContentUris.parseId(productUri))};
        numRowsUpdated = getContentResolver().update(productUri, contentValues, whereClause, whereArgs);
        Log.v(LOG_TAG, "numRowsUpdated=" + numRowsUpdated);
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

    public Bitmap getBitmapFromUri(Uri uri) {

        if (uri == null || uri.toString().isEmpty())
            return null;

        // Get the dimensions of the View
        int targetW = productImageView.getWidth();
        int targetH = productImageView.getHeight();

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














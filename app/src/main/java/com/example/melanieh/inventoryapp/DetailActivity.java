package com.example.melanieh.inventoryapp;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.melanieh.inventoryapp.data.ProductContract;
import com.example.melanieh.inventoryapp.data.ProductDBHelper;


/**
 * Created by melanieh on 10/30/16.
 */

public class DetailActivity extends AppCompatActivity {

    Cursor cursor;
    EditText currentQtyView;
    int currentQty = 0;
    Uri productUri;
    String supplierEmail;
    SQLiteDatabase db;
    ProductDBHelper dbHelper;
    String name;
    Button updateQtyBtn;
    ImageButton upArrow;
    ImageButton downArrow;
    Button updateFromShipment;
    int delta;
    int id;

    /***
     * CRUD method variables
     */
    String productTable = ProductContract.ProductEntry.PRODUCT_TABLE_NAME;
    String shipmentTable = ProductContract.ShipmentEntry.SHIPMENT_TABLE_NAME;


    /**** log tag */
    private static final String LOG_TAG = DetailActivity.class.getSimpleName();

    /***
     * intent chooser strings
     */
    private static final String SELECT_EMAIL_APP = "Send e-mail to Supplier using:";


        /**
         * projection/columns array for cursor
         */

        String[] productsProjection = {
                ProductContract.ProductEntry.COLUMN_ID,
                ProductContract.ProductEntry.COLUMN_NAME,
                ProductContract.ProductEntry.COLUMN_QTY,
                ProductContract.ProductEntry.COLUMN_IMAGE_URI,
                ProductContract.ProductEntry.COLUMN_SUPPLIER_EMAIL
        };

        String[] shipProjection = {
                ProductContract.ShipmentEntry.COLUMN_ID,
                ProductContract.ShipmentEntry.COLUMN_INCOMING_PROD,
                ProductContract.ShipmentEntry.COLUMN_INCOMING_QTY,
        };

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            Log.v(LOG_TAG, "onCreate called...");
            super.onCreate(savedInstanceState);
            setContentView(R.layout.product_detail);

            dbHelper = new ProductDBHelper(this);

            /** interactive UI views */
            TextView nameView = (TextView) findViewById(R.id.name);
            currentQtyView = (EditText) findViewById(R.id.qty_edit_field);

            /** current quantity */
            downArrow = (ImageButton) findViewById(R.id.qty_down_arrow);
            upArrow = (ImageButton) findViewById(R.id.qty_up_arrow);
            updateQtyBtn = (Button) findViewById(R.id.update_quantity);
            updateFromShipment = (Button) findViewById(R.id.update_qty_shipment);

            Log.v(LOG_TAG, "productUri = " + productUri);

            displayProductDetails();


        }


    @Override
        public void finish() {
            cursor.close();
        }

        /***
         * activity options menu
         */

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            getMenuInflater().inflate(R.menu.menu_product_detail, menu);
            return true;
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete:
                    showDeleteConfirmationDialog();
                case R.id.action_edit:
                    editProductInfo();
//            case android.R.id.home:
//                NavUtils.navigateUpFromSameTask(this);

            }
            return true;
        }

        @Override
        public void onBackPressed() {
            NavUtils.navigateUpFromSameTask(this);
        }

        /**
         * delete product confirmation dialog
         */
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
                    dialogInterface.dismiss();
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

            Log.v(LOG_TAG, "deleteProduct() called...");
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
            Log.v(LOG_TAG, "prodUri = " + productUri);
            Intent editProduct = new Intent(DetailActivity.this, EditProductActivity.class);
            editProduct.setData(productUri);
            startActivity(editProduct);

        }

        /**
         * helper method for completing supplier e-mail
         */
        StringBuilder orderString = new StringBuilder("");

        private void addProductToSupplierOrderDialog() {

            String yesString = getString(R.string.supp_email_dialog_yes);
            String noString = getString(R.string.supp_email_dialog_no);

            DialogInterface.OnClickListener yesButtonListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    addProductToSupplierOrder(name, 5);
                }
            };

            DialogInterface.OnClickListener noButtonListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (dialogInterface != null) {
//                        dialogInterface.dismiss();
                    }
                }
            };

            AlertDialog.Builder supplierEmailADBuilder = new AlertDialog.Builder(this);
            supplierEmailADBuilder.setMessage(getString(R.string.supp_email_dialog_msg));
            supplierEmailADBuilder.setPositiveButton(yesString, yesButtonListener);
            supplierEmailADBuilder.setNegativeButton(noString, noButtonListener);

            supplierEmailADBuilder.create();
            supplierEmailADBuilder.show();
        }

        public String addProductToSupplierOrder(String productName, int desiredQty) {
            orderString.append("Product Name: " + productName + "Quantity Requested: " + desiredQty);
            return orderString.toString();
        }

        private void displayProductDetails() {
            Log.v(LOG_TAG, "displayProductDetails called");
            db = dbHelper.getReadableDatabase();


            /** inbound intent passing action and MIME type for product image from EditProduct Activity **/
            Intent intent = getIntent();
            productUri = intent.getData();

            String prodSelection = ProductContract.ProductEntry.COLUMN_ID + "=?";
            String[] prodSelectionArgs = new String[]{String.valueOf(ContentUris.parseId(productUri))};
            Log.v(LOG_TAG, "productUri= " + productUri);

            // pull necessary data from products table
            cursor = db.query(productTable, productsProjection, prodSelection, prodSelectionArgs,
                    null, null, null);

            TextView nameView = (TextView) findViewById(R.id.name);
            currentQtyView = (EditText) findViewById(R.id.qty_edit_field);

            Log.v(LOG_TAG, "data=" + cursor);

            // query products table for product info
            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_NAME));
                nameView.setText(name);
                int qty = cursor.getInt
                        (cursor.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_QTY));
                currentQty = qty - 1;
                String qtyString = String.valueOf(currentQty);
                currentQtyView.setText(qtyString);
                String imageUriString = cursor.getString(cursor.getColumnIndex
                        (ProductContract.ProductEntry.COLUMN_IMAGE_URI));
                supplierEmail = cursor.getString
                        (cursor.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_SUPPLIER_EMAIL));
            }


            // three ways to update quantity:
            // 1. type it in

            downArrow = (ImageButton) findViewById(R.id.qty_down_arrow);
            upArrow = (ImageButton) findViewById(R.id.qty_up_arrow);
            updateQtyBtn = (Button) findViewById(R.id.update_quantity);
            updateFromShipment = (Button) findViewById(R.id.update_qty_shipment);

            /***
             * onClickListener for quantity updates
             */
            View.OnClickListener onClickListener = new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    delta = 0;
                    dismissKeyboard(currentQtyView);
                    switch (view.getId()) {

                        case R.id.qty_down_arrow:
                            int downDelta = -1;
                            updateQty(currentQty + downDelta);

                        case R.id.qty_up_arrow:
                            int upDelta = 1;
                            updateQty(currentQty + upDelta);

                        case R.id.update_qty_shipment:
                            updateQty(currentQty + delta);

                    }


                }
            };

            // 2. use up and down arrows to increment by one

            upArrow.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    dismissKeyboard(currentQtyView);
                    delta = 1;
                    updateQty(currentQty + delta);

                }});


            downArrow.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    dismissKeyboard(currentQtyView);
                    delta = -1;
                    updateQty(currentQty + delta);

                }});


            // 3. click "update qty from shipment" to pull dummy quantity value from shipment table
            updateFromShipment.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    dismissKeyboard(currentQtyView);
                    delta = pullQtyFromShipment();

                    updateQty(currentQty + delta);

                }});

            updateQtyBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int userInputQty = Integer.parseInt(currentQtyView.getText().toString());
                    updateQty(userInputQty);
                }
            });

            /** contact supplier */
            Button orderMore = (Button) findViewById(R.id.order_more);

            orderMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    String subject = getString(R.string.supplier_email_subject);
                    String emailBodyTextTemplate = getString(R.string.email_body_text_template);

                    Intent sendEmail = new Intent(Intent.ACTION_SENDTO);
                    sendEmail.setType("text/html");
                    sendEmail.setData(Uri.parse("mailto:" + supplierEmail)); // only email apps should handle this
                    sendEmail.putExtra(Intent.EXTRA_SUBJECT, subject);

                    sendEmail.putExtra(Intent.EXTRA_TEXT, emailBodyTextTemplate);
                    startActivity(Intent.createChooser(sendEmail,
                            SELECT_EMAIL_APP));
                }
            });

        }

        /*** quantity update helper methods */

        public void updateQty(int qty) {
            Log.v(LOG_TAG, "updateQty called...");
            Log.v(LOG_TAG, "ship qty=" + qty);

            // note: delta can be positive or negative
            // onTouchListener detects where the quantity update is coming from and updates accordingly

            currentQtyView = (EditText) findViewById(R.id.qty_edit_field);

            if (qty < 0) {
                Toast.makeText(DetailActivity.this, getString(R.string.invalid_quantity_reset_zero),
                        Toast.LENGTH_SHORT).show();
                qty = 0;
            }

            db = dbHelper.getWritableDatabase();

            ContentValues updatedValues = new ContentValues();
            updatedValues.put(ProductContract.ProductEntry.COLUMN_QTY, qty);
            String whereClause = ProductContract.ProductEntry.COLUMN_ID + "=?";
            String[] whereArgs = new String[]{String.valueOf(ContentUris.parseId(productUri))};
            int numRowsUpdated = db.update(productTable, updatedValues, whereClause, whereArgs);
            currentQtyView.setText(String.valueOf(qty));
            currentQty = qty;
            Log.v(LOG_TAG, "currentQty= " + currentQty);

            if (numRowsUpdated != 1) {
                Toast.makeText(DetailActivity.this, getString(R.string.error_updating_qty),
                        Toast.LENGTH_SHORT).show();
            }

            if (currentQty == 0) {
                addProductToSupplierOrderDialog();

            }
            finish();
        }

        public int pullQtyFromShipment() {
            Log.v(LOG_TAG, "pullQtyFromShipment called...");
            // dummy shipment data

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
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

        }

}














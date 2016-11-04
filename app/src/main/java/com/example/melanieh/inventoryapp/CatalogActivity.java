package com.example.melanieh.inventoryapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;
import com.example.melanieh.inventoryapp.data.*;

import com.example.melanieh.inventoryapp.data.ProductContract;

public class CatalogActivity extends AppCompatActivity {

    ListView productListView;
    ProductCursorAdapter adapter;
    SQLiteDatabase db;
    ProductDBHelper dbHelper;
    String productTable = ProductContract.ProductEntry.PRODUCT_TABLE_NAME;
    Cursor cursor;
    int quantity;

    /** log tag */
    private static final String LOG_TAG = CatalogActivity.class.getSimpleName();


    /** intent extra for passing decrease in quantity when clicking the sell button */
    public String QTY_DELTA_EXTRA = "qtyDelta";

    /** projection for cursorloader and content provider calls */
    String[] projection = {ProductContract.ProductEntry.COLUMN_ID,
            ProductContract.ProductEntry.COLUMN_NAME,
            ProductContract.ProductEntry.COLUMN_QTY,
            ProductContract.ProductEntry.COLUMN_PRICE,
            ProductContract.ProductEntry.COLUMN_IMAGE_URI,
            ProductContract.ProductEntry.COLUMN_SUPPLIER_EMAIL};

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.catalog);
        dbHelper = new ProductDBHelper(this);

        // interactive UI views
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        productListView = (ListView) findViewById(R.id.list_view);

        // product list empty view
        View emptyView = findViewById(R.id.emptyview);
        productListView.setEmptyView(emptyView);

        // click listeners

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CatalogActivity.this, EditProductActivity.class);
                startActivity(intent);
            }
        });

        displayProducts();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_catalog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;

        }
        return true;
    }

    /** alertdialog for delete all products menu option */

    private void showDeleteConfirmationDialog() {

        AlertDialog.Builder deleteConfADBuilder = new AlertDialog.Builder(this);
        deleteConfADBuilder.setMessage(getString(R.string.deleteConf_dialog_msg));

        //clicklisteners for buttons
        // positive button=yes, delete all products
        DialogInterface.OnClickListener yesButtonListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                deleteAllProducts();
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


    /** delete all products help method */

    private void deleteAllProducts() {

        db = dbHelper.getWritableDatabase();
        /**
         * @param whereClause is null because all rows are being deleted from the product table
         *
         * @param whereArgs[] is also null because the whereClause is null, i.e. there are no values
         *                     for the table data selected in the whereClause because nothing is selected
         *                     for the whereClause;
         */
        int numRowsDeleted = db.delete(ProductContract.ProductEntry.PRODUCT_TABLE_NAME, null, null);

        if (numRowsDeleted == 0) {
            Toast.makeText(CatalogActivity.this,
                    getString(R.string.error_deleting_all_products), Toast.LENGTH_LONG).show();

        } else {
            Toast.makeText(CatalogActivity.this,
                    getString(R.string.delete_all_products_successful), Toast.LENGTH_LONG).show();
            adapter.changeCursor(null);
        }
    }


    public void displayProducts() {

        db = dbHelper.getReadableDatabase();

        String[] projection = {ProductContract.ProductEntry.COLUMN_ID,
                ProductContract.ProductEntry.COLUMN_NAME,
                ProductContract.ProductEntry.COLUMN_QTY,
                ProductContract.ProductEntry.COLUMN_PRICE,
                ProductContract.ProductEntry.COLUMN_IMAGE_URI,
                ProductContract.ProductEntry.COLUMN_SUPPLIER_EMAIL};

        Cursor cursor = db.query(productTable, projection, null,
                null, null, null, null);

        int idColIndex = cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_ID);
        int nameColIndex = cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_NAME);
        int qtyColIndex = cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_QTY);
        int priceColIndex = cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_PRICE);
        int imageUriColIndex = cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_IMAGE_URI);
        int suppEmailColIndex = cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_SUPPLIER_EMAIL);

        while (cursor.moveToNext()) {

            int id = cursor.getInt(idColIndex);
            String name = cursor.getString(nameColIndex);
            int quantity = cursor.getInt(qtyColIndex);
            double price = cursor.getDouble(priceColIndex);
            String imageUri = cursor.getString(imageUriColIndex);
            String suppEmail = cursor.getString(suppEmailColIndex);
        }

        productListView = (ListView) findViewById(R.id.list_view);
        adapter = new ProductCursorAdapter(this, cursor);
        productListView.setAdapter(adapter);
        adapter.notifyDataSetChanged();


    }

}

package com.example.melanieh.inventoryapp;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import com.example.melanieh.inventoryapp.data.*;

import com.example.melanieh.inventoryapp.data.ProductContract;

public class CatalogActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    RecyclerView catalogRecyclerView;
    LinearLayoutManager catalogLLManager;
    ProductCursorRecyclerViewAdapter adapter;
    SQLiteDatabase db;
    ProductDBHelper dbHelper;
    View emptyView;
    Uri currentProdUri;

    /** log tag */
    private static final String LOG_TAG = CatalogActivity.class.getSimpleName();

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
        catalogRecyclerView = (RecyclerView) findViewById(R.id.catalog_recyclerview);
        emptyView = findViewById(R.id.emptyview);
        catalogLLManager = new LinearLayoutManager(this);
        catalogRecyclerView.setLayoutManager(catalogLLManager);
        adapter = new ProductCursorRecyclerViewAdapter(this, null);
        catalogRecyclerView.setAdapter(adapter);

        // click listeners
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CatalogActivity.this, EditProductActivity.class);
                startActivity(intent);
            }
        });

        getLoaderManager().initLoader(1, null, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        getLoaderManager().initLoader(1, null, this);
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
                }
                return true;
            }


    /**
     * alertdialog for delete all products menu option
     */

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

        String yesString = getString(R.string.delete_dialog_yes);
        String noString = getString(R.string.delete_dialog_no);
        deleteConfADBuilder.setPositiveButton(yesString, yesButtonListener);
        deleteConfADBuilder.setNegativeButton(noString, null);

        deleteConfADBuilder.create();
        deleteConfADBuilder.show();
    }

    /*** delete all products */

    private void deleteAllProducts() {

        db = dbHelper.getWritableDatabase();
        /**
         * @param whereClause is null because all rows are being deleted from the product table
         *
         * @param whereArgs[] is also null because the whereClause is null, i.e. there are no values
         *                     for the table data selected in the whereClause because nothing is selected
         *                     for the whereClause;
         */
        int numRowsDeleted = db.delete(ProductContract.ProductEntry.TABLE_NAME, null, null);

        if (numRowsDeleted == 0) {
            Toast.makeText(CatalogActivity.this,
                    getString(R.string.error_deleting_all_products), Toast.LENGTH_LONG).show();

        } else {
            Toast.makeText(CatalogActivity.this,
                    getString(R.string.delete_all_products_successful), Toast.LENGTH_LONG).show();
            adapter.changeCursor(null);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Uri contentUri = ProductContract.ProductEntry.PRODUCTS_CONTENT_URI;
        return new CursorLoader(this, contentUri, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.v(LOG_TAG, "cursor= " + cursor);
        adapter.swapCursor(cursor);
        if (cursor != null) {
            emptyView.setVisibility(View.GONE);
            // recycler view
            catalogLLManager = new LinearLayoutManager(this);
            catalogRecyclerView.setLayoutManager(catalogLLManager);
            adapter = new ProductCursorRecyclerViewAdapter(this, cursor);
            catalogRecyclerView.setAdapter(adapter);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
        getLoaderManager().restartLoader(1, null, this);
    }
}

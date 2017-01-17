package com.example.melanieh.inventoryapp.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import android.view.View;

import com.example.melanieh.inventoryapp.EditProductActivity;
import com.example.melanieh.inventoryapp.R;
import com.example.melanieh.inventoryapp.data.ProductContract.ProductEntry;


/*** Created by melanieh on 11/19/16. */

public class ProductProvider extends ContentProvider {

    // table names
    public static final String productTable = ProductContract.ProductEntry.TABLE_NAME;
    public static final String shipmentTable = ProductContract.ShipmentEntry.TABLE_NAME;
    // This cursor will hold the result of the query
    Cursor cursor;

    /*** projection for cursorloader calls */
    String[] projection = {ProductContract.ProductEntry.COLUMN_ID,
            ProductContract.ProductEntry.COLUMN_NAME,
            ProductContract.ProductEntry.COLUMN_QTY,
            ProductContract.ProductEntry.COLUMN_PRICE,
            ProductContract.ProductEntry.COLUMN_IMAGE_URI,
            ProductContract.ProductEntry.COLUMN_SUPPLIER_EMAIL};

/** {@link ContentProvider} */

    /** Tag for the log messages */
    public final String LOG_TAG = ProductProvider.class.getSimpleName();

    /** URI matcher code for the content URI for the products table */
    private static final int PRODUCTS = 100;

    /** URI matcher code for a single product's content URI */
    private static final int PRODUCT_ID = 101;

    /**
     * UriMatcher object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        // The calls to addURI() go here, for all of the content URI patterns that the provider
        // should recognize. All paths added to the UriMatcher have a corresponding code to return
        // when a match is found.

        // The content URI of the form "content://com.example.android.pets/pets" will map to the
        // integer code {@link #PETS}. This URI is used to provide access to MULTIPLE rows
        // of the pets table.
        sUriMatcher.addURI(ProductContract.CONTENT_AUTHORITY, ProductContract.PATH_PRODUCTS, PRODUCTS);

        // The content URI of the form "content://com.example.android.pets/pets/#" will map to the
        // integer code {@link #PET_ID}. This URI is used to provide access to ONE single row
        // of the pets table.
        //
        // In this case, the "#" wildcard is used where "#" can be substituted for an integer.
        // For example, "content://com.example.android.pets/pets/3" matches, but
        // "content://com.example.android.pets/pets" (without a number at the end) doesn't match.
        sUriMatcher.addURI(ProductContract.CONTENT_AUTHORITY, ProductContract.PATH_PRODUCTS + "/#", PRODUCT_ID);
    }

    SQLiteDatabase db;
    /** Database helper object */
    private ProductDBHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = new ProductDBHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Get readable database
        db = mDbHelper.getReadableDatabase();

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                cursor = db.query(ProductContract.ProductEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case PRODUCT_ID:
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };

                // Cursor containing that row of the table.
                cursor = db.query(ProductEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return insertProduct(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * helper methods for inserting a new product or updating an existing product
     */
    private Uri insertProduct(Uri uri, ContentValues contentValues) {
        Double price = 0.0;
        Integer qty = 0;

        db = mDbHelper.getWritableDatabase();

        long newRowId = db.insert(ProductContract.ProductEntry.TABLE_NAME, null, contentValues);

        if (newRowId == -1) {
            Toast.makeText(getContext(), getContext().getString(R.string.editor_error_inserting_product),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), getContext().getString(R.string.editor_insert_product_successful),
                    Toast.LENGTH_SHORT).show();
        }
        getContext().getContentResolver().notifyChange(uri, null);

        // Return the new URI with the ID (of the newly inserted row) appended at the end
        return ContentUris.withAppendedId(ProductContract.ProductEntry.PRODUCTS_CONTENT_URI, newRowId);
    }

    private int updateProduct(Uri uri, ContentValues contentValues, String selection,
                              String[] selectionArgs) {

        db = mDbHelper.getWritableDatabase();
        int numRowsUpdated = 0;

        if (contentValues.containsKey(ProductEntry.COLUMN_NAME)) {
            String name = contentValues.getAsString(ProductEntry.COLUMN_NAME);
            if (name == null) {
                // returns zero for numRowsUpdated which will prompt the error toast message for
                // updating product due to the empty name field
                throw new IllegalArgumentException(getContext().
                        getString(R.string.product_name_exception));
            }

            // check that the {@link ProductEntry#COLUMN_QTY value is updated and valid.
            if (contentValues.containsKey(ProductEntry.COLUMN_QTY)) {
                Integer qty = contentValues.getAsInteger(ProductEntry.COLUMN_QTY);
                if (qty != null && qty < 0) {
                    throw new IllegalArgumentException(getContext().
                            getString(R.string.product_quantity_exception));
                }
            }

            // check that the @link{ProductEntry#COLUMN_PRICE} value is updated and valid.
            if (contentValues.containsKey(ProductEntry.COLUMN_PRICE)) {
                // Check that the weight is greater than or equal to 0 kg
                Integer price = contentValues.getAsInteger(ProductEntry.COLUMN_PRICE);
                if (price != null && price < 0) {
                    throw new IllegalArgumentException(getContext().
                            getString(R.string.product_price_exception));
                }
            }

            // No need to check the imageUri string, any value is valid (including null).
            // check that the {@link ProductEntry#COLUMN_IMAGE_URI value is updated and valid.
            if (contentValues.containsKey(ProductEntry.COLUMN_IMAGE_URI)) {
                String selectedImageUri = contentValues.getAsString(ProductEntry.COLUMN_IMAGE_URI);
                if (selectedImageUri != null && !selectedImageUri.contains("content")) {
                    throw new IllegalArgumentException(getContext().getString(R.string.product_image_uri_exception));
                }
            }

            // check that the @link{ProductEntry#COLUMN_SUPPLIER_EMAIL} value is updated and valid.
            if (contentValues.containsKey(ProductEntry.COLUMN_SUPPLIER_EMAIL)) {
                String supplierEmail = contentValues.getAsString(ProductEntry.COLUMN_SUPPLIER_EMAIL);
                if (supplierEmail == null && !supplierEmail.contains("@")) {
                    throw new IllegalArgumentException(getContext().getString(R.string.supplier_email_exception));
                }
            }

            if (numRowsUpdated != 0) {
                getContext().getContentResolver().notifyChange(uri, null);
            } else if (numRowsUpdated == -1) {
                Toast.makeText(getContext(), getContext().getString(R.string.error_updating_product),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), getContext().getString(R.string.product_update_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
            // Returns the number of database rows affected by the update statement
            return db.update(ProductEntry.TABLE_NAME, contentValues, selection, selectionArgs);
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection,
                      String[] selectionArgs) {
        Log.v(LOG_TAG, "ProductProvider: update: numRowsUpdated" + updateProduct(uri, contentValues, selection, selectionArgs));
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                Log.v(LOG_TAG, "update: numRowsUpdated= " + updateProduct(uri, contentValues, selection, selectionArgs));
                return updateProduct(uri, contentValues, selection, selectionArgs);
            case PRODUCT_ID:
                // For the PET_ID code, extract out the ID from the URI,
                // so we know which row to update. Selection will be "_id=?" and selection
                // arguments will be a String array containing the actual ID.
                Log.v(LOG_TAG, "ProductProvider: update: numRowsUpdated" + updateProduct(uri, contentValues, selection, selectionArgs));
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                return updateProduct(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Get writeable database
        db = mDbHelper.getWritableDatabase();

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                // Delete all rows that match the selection and selection args
                deleteProduct();
            case PRODUCT_ID:
                // Delete a single row given by the ID in the URI
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                return db.delete(ProductEntry.TABLE_NAME, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }
    }

    private void deleteProduct() {

        db = mDbHelper.getWritableDatabase();
        int numRowsDeleted = db.delete(productTable, null, null);

        if (numRowsDeleted == 0) {
            Toast.makeText(getContext(), getContext().getString(R.string.edit_error_deleting_product),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), getContext().getString(R.string.edit_product_deletion_successful),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return ProductEntry.CONTENT_LIST_TYPE;
            case PRODUCT_ID:
                return ProductEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }
}

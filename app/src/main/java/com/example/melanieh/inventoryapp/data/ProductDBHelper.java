package com.example.melanieh.inventoryapp.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.melanieh.inventoryapp.data.ProductContract;

/**
 * Created by melanieh on 10/30/16.
 */

public class ProductDBHelper extends SQLiteOpenHelper {

    /** database name and version string constants */
    private static final String DATABASE_NAME = "vendor.db";
    private static final int DATABASE_VERSION = 1;

    public ProductDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    /** data type strings for SQL command strings */
    private static final String INT_PRIMARY_KEY_AUTOINC = " INTEGER PRIMARY KEY AUTOINCREMENT";
    private static final String INTEGER_NOT_NULL_DEFAULT = " INTEGER NOT NULL DEFAULT 0";
    private static final String TEXT = " TEXT";
    private static final String TEXT_NOT_NULL = " TEXT NOT NULL";
    private static final String MONEY = " MONEY";


    String[] projection = {ProductContract.ProductEntry.COLUMN_ID,
            ProductContract.ProductEntry.COLUMN_NAME,
            ProductContract.ProductEntry.COLUMN_QTY,
            ProductContract.ProductEntry.COLUMN_PRICE,
            ProductContract.ProductEntry.COLUMN_IMAGE_URI,
            ProductContract.ProductEntry.COLUMN_SUPPLIER_EMAIL};

    /** SQL command strings */
    private static final String SQL_CREATE_PRODUCTS_TABLE = "CREATE TABLE " +
            ProductContract.ProductEntry.PRODUCT_TABLE_NAME + " (" +
            ProductContract.ProductEntry.COLUMN_ID + INT_PRIMARY_KEY_AUTOINC + ", " +
            ProductContract.ProductEntry.COLUMN_NAME + TEXT_NOT_NULL + "," +
            ProductContract.ProductEntry.COLUMN_QTY + INTEGER_NOT_NULL_DEFAULT + ", " +
            ProductContract.ProductEntry.COLUMN_PRICE + MONEY + ", " +
            ProductContract.ProductEntry.COLUMN_IMAGE_URI + TEXT + ", " +
            ProductContract.ProductEntry.COLUMN_SUPPLIER_EMAIL + TEXT + ");";

    private static final String SQL_CREATE_SHIPMENTS_TABLE = "CREATE TABLE " +
            ProductContract.ShipmentEntry.SHIPMENT_TABLE_NAME + " (" +
            ProductContract.ProductEntry.COLUMN_ID + INT_PRIMARY_KEY_AUTOINC + ", " +
            ProductContract.ShipmentEntry.COLUMN_INCOMING_PROD + TEXT_NOT_NULL + ", " +
            ProductContract.ShipmentEntry.COLUMN_INCOMING_QTY + INTEGER_NOT_NULL_DEFAULT + ");";

    private static final String DELETE_PRODUCTS_TABLE = "DROP TABLE IF EXISTS "
            + ProductContract.ProductEntry.PRODUCT_TABLE_NAME + ";";

    private static final String DELETE_SHIPMENTS_TABLE = "DROP TABLE IF EXISTS "
            + ProductContract.ShipmentEntry.SHIPMENT_TABLE_NAME + ";";

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_PRODUCTS_TABLE);
        db.execSQL(SQL_CREATE_SHIPMENTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DELETE_PRODUCTS_TABLE);
        db.execSQL(DELETE_SHIPMENTS_TABLE);
        onCreate(db);
    }
}

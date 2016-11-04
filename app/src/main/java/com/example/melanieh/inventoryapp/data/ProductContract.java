package com.example.melanieh.inventoryapp.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * Created by melanieh on 10/30/16.
 */

public class ProductContract {

    /** log tag */
    private static final String LOG_TAG = ProductContract.class.getName();

    private ProductContract() {

    }

    public static final String CONTENT_AUTHORITY = "com.example.melanieh.inventoryapp";

    public static final Uri BASE_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    private static final String PATH_PRODUCTS = "products";
    private static final String PATH_SHIPMENTS = "shipments";

    /**
     * Created by melanieh on 11/1/16.
     */

    public static class ProductEntry implements BaseColumns {

        public static final String PRODUCT_TABLE_NAME = "products";

        /** content URI */
        public static final Uri PRODUCTS_CONTENT_URI = Uri.withAppendedPath(BASE_URI, PATH_PRODUCTS);

        /*** The MIME type of the {@link #PRODUCTS_CONTENT_URI} for a list of products. */
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PRODUCTS;

        /*** The MIME type of the {@link #PRODUCTS_CONTENT_URI} for a single product. */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PRODUCTS;


        /** table column names */
        public static final String COLUMN_ID = BaseColumns._ID;
        public static final String COLUMN_NAME = "Name";
        public static final String COLUMN_QTY = "Quantity";
        public static final String COLUMN_PRICE = "Price";
        public static final String COLUMN_IMAGE_URI = "imageURI";
        public static final String COLUMN_SUPPLIER_EMAIL = "SupplierEmail";

    }

    /*** Created by melanieh on 11/1/16. */

    public static class ShipmentEntry implements BaseColumns {

        public static final String SHIPMENT_TABLE_NAME = "shipments";

        /** content URI */
        public static final Uri SHIPMENTS_CONTENT_URI = Uri.withAppendedPath(BASE_URI, PATH_SHIPMENTS);

        /*** The MIME type of the {@link #SHIPMENTS_CONTENT_URI} for a list of incoming products. */
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_SHIPMENTS;

        /**
         * The MIME type of the {@link #SHIPMENTS_CONTENT_URI} for a single product.
         */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_SHIPMENTS;

        /** table column names */
        public static final String COLUMN_ID = BaseColumns._ID;
        public static final String COLUMN_INCOMING_PROD = "productName";
        public static final String COLUMN_INCOMING_QTY = "prodQty";

    }

    /** for logging/debugging purposes */
    @Override
    public String toString() {
        return "ProductContract content URIs {" +
                "BASE_URI+" + BASE_URI.toString() +
                "PRODUCTS_CONTENT_URI" + ProductEntry.PRODUCTS_CONTENT_URI.toString() +
                "SHIPMENTS_CONTENT_URI" + ShipmentEntry.SHIPMENTS_CONTENT_URI.toString() +
                "}";
    }
}

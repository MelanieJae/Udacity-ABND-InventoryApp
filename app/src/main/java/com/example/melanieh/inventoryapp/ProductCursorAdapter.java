package com.example.melanieh.inventoryapp;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.melanieh.inventoryapp.data.ProductContract;
import com.google.android.gms.analytics.ecommerce.Product;

/**
 * Created by melanieh on 10/30/16.
 */

public class ProductCursorAdapter extends CursorAdapter{

    Context mContext;
    /** log tag */
    private static final String LOG_TAG = ProductCursorAdapter.class.getName();

    public static final String QTY_DELTA = "delta";
    public ProductCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }
    int id;

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View newView = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
        return newView;
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        final int quantity;
        id = cursor.getInt(cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_ID));
        final Uri currentProdUri = ContentUris.withAppendedId(ProductContract.ProductEntry.PRODUCTS_CONTENT_URI, id);
        TextView nameView = (TextView) view.findViewById(R.id.name);
        String name = cursor.getString(cursor.
                getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_NAME));
        nameView.setText(name);
        Log.v(LOG_TAG, "bindView: product name= " + name);

        TextView qtyView = (TextView) view.findViewById(R.id.current_quantity);
        quantity = cursor.getInt
                (cursor.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_QTY));
        qtyView.setText("Qty in Stock: " + String.valueOf(quantity));
        Log.v(LOG_TAG, "bindView: product qty= " + quantity);

        double price = cursor.getDouble
                (cursor.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_PRICE));
        String formattedPriceString = formatPrice(price);
        TextView priceView = (TextView) view.findViewById(R.id.price);
        priceView.setText("Price :" + formattedPriceString);
        Log.v(LOG_TAG, "bindView: price= " + formattedPriceString);

        Button sellBtn = (Button) view.findViewById(R.id.sell_btn);
        sellBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent openDetail = new Intent(context, DetailActivity.class);
                openDetail.setData(currentProdUri);
                int newQuantity;
                if (quantity >= 1) {
                    newQuantity = quantity - 1;
                } else {
                    Toast.makeText(context, context.getString(R.string.invalid_quantity_reset_zero),
                            Toast.LENGTH_SHORT)
                            .show();
                    newQuantity = 0;
                }
                ContentValues contentValues = new ContentValues();
                contentValues.put(ProductContract.ProductEntry.COLUMN_QTY, newQuantity);
                int numRowsUpdated = context.getContentResolver().update(currentProdUri, contentValues, null, null);
                context.startActivity(openDetail);
                }
        });
    }

    private String formatPrice(double number) {
        java.text.NumberFormat formatter = java.text.NumberFormat.getCurrencyInstance();
        return formatter.format(number);
    }

}

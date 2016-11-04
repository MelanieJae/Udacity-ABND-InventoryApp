package com.example.melanieh.inventoryapp;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.icu.math.BigDecimal;
import android.icu.text.NumberFormat;
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

/**
 * Created by melanieh on 10/30/16.
 */

public class ProductCursorAdapter extends CursorAdapter {

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
            final Intent openDetail;
            id = cursor.getInt(cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_ID));
            final Uri currentProdUri = ContentUris.withAppendedId
                (ProductContract.ProductEntry.PRODUCTS_CONTENT_URI, id);

            // for the sell product button; reduces quantity by 1 when clicked
            openDetail = new Intent(context, DetailActivity.class);
            openDetail.setData(currentProdUri);
            openDetail.putExtra(QTY_DELTA, -1);

            TextView nameView = (TextView)view.findViewById(R.id.name);
            nameView.setText(cursor.getString(cursor.
                    getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_NAME)));
            TextView qtyView = (TextView)view.findViewById(R.id.current_quantity);
            final int quantity = cursor.getInt
                    (cursor.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_QTY));
            qtyView.setText("Qty: " + String.valueOf(quantity));

            double price = cursor.getDouble
                    (cursor.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_PRICE));
            String formattedPriceString = formatPrice(price);
            TextView priceView = (TextView)view.findViewById(R.id.price);
            priceView.setText("Price :" + formattedPriceString);

            Button sellBtn = (Button)view.findViewById(R.id.sale);

            sellBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openDetail.setData(currentProdUri);
                    context.startActivity(openDetail);

                }
            });

    }

    public String formatPrice(double number) {

        java.text.NumberFormat formatter = java.text.NumberFormat.getCurrencyInstance();
        return formatter.format(number);
    }

}

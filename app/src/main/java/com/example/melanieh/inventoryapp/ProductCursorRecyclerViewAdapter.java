package com.example.melanieh.inventoryapp;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.melanieh.inventoryapp.data.ProductContract;
import com.example.melanieh.inventoryapp.data.ProductContract.ProductEntry;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by melanieh on 1/27/17.
 */

public class ProductCursorRecyclerViewAdapter extends
        RecyclerView.Adapter<ProductCursorRecyclerViewAdapter.CatalogViewHolder> {

    Context context;
    Cursor cursor;
    CursorDataSetObserver dataSetObserver;
    boolean mDataValid;
    Uri productUri;
    int idColIndex;

    String[] projection = {ProductEntry.COLUMN_ID,
            ProductEntry.COLUMN_NAME,
            ProductEntry.COLUMN_QTY,
            ProductEntry.COLUMN_PRICE,
            ProductEntry.COLUMN_IMAGE_URI,
            ProductEntry.COLUMN_SUPPLIER_EMAIL};

    public ProductCursorRecyclerViewAdapter(Context context, Cursor cursor) {
        this.context = context;
        this.cursor = cursor;
        mDataValid = cursor != null;
        int idColIndex = mDataValid ? cursor.getColumnIndex("_id") : -1;
        dataSetObserver = new CursorDataSetObserver();
        if (cursor != null) {
            cursor.registerDataSetObserver(dataSetObserver);
        }
    }

    @Override
    public int getItemCount () {
        if (mDataValid && cursor != null) {
            return cursor.getCount();
        } else {
            return 0;
        }
    }

    @Override
    public CatalogViewHolder onCreateViewHolder (ViewGroup parent,int viewType){
        View inflatedView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.catalog_recyclerview_item, parent, false);
        return new CatalogViewHolder(inflatedView);
    }

    @Override
    public void onBindViewHolder (CatalogViewHolder holder, int position){
        if (!mDataValid) {
            throw new IllegalStateException(context.getString(R.string.is_exception_cursor_invalid));
        }
        if (!cursor.moveToPosition(position)) {
            throw new IllegalStateException(context.getString(R.string.is_exception_cannot_move_cursor + position));
        }

        // read cursor row; convert to movie item, then pass movie as itemView to bind method
        int rowId = cursor.getInt(cursor.getColumnIndexOrThrow(ProductEntry.COLUMN_ID));
        productUri = ContentUris.withAppendedId(ProductEntry.PRODUCTS_CONTENT_URI, rowId);
        Cursor itemView = context.getContentResolver().query(productUri, projection, null, null, null);

        holder.bind(itemView);
    }

    public void changeCursor(Cursor cursor) {
        Cursor old = swapCursor(cursor);
        if (old != null) {
            old.close();
        }
    }

    public Cursor swapCursor(Cursor newCursor) {
        if (newCursor == cursor) {
            return null;
        }
        final Cursor oldCursor = cursor;
        if (oldCursor != null && dataSetObserver != null) {
            oldCursor.unregisterDataSetObserver(dataSetObserver);
        }
        cursor = newCursor;
        if (cursor != null) {
            if (dataSetObserver != null) {
                cursor.registerDataSetObserver(dataSetObserver);
            }
            idColIndex = newCursor.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_ID);
            mDataValid = true;
            notifyDataSetChanged();
        } else {
            idColIndex = -1;
            mDataValid = false;
            notifyDataSetChanged();
            //There is no notifyDataSetInvalidated() method in RecyclerView.Adapter
        }
        return oldCursor;
    }


    /*** Created by melanieh on 1/27/17. */

    public static class CatalogViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        private static final String LOG_TAG = CatalogViewHolder.class.getSimpleName();

        @BindView(R.id.product_cardview) CardView productCardView;
        @BindView(R.id.name) TextView nameView;
        @BindView(R.id.current_quantity) TextView quantityView;
        @BindView(R.id.price) TextView priceView;
        @BindView(R.id.sell_btn) Button sellBtn;

        int productId;
        int qty;
        Context context = itemView.getContext();

        public CatalogViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void bind(final Cursor cursor) {
            while (cursor.moveToNext()) {
                int productIdColIndex = cursor.getColumnIndexOrThrow
                        (ProductEntry.COLUMN_ID);
                productId = cursor.getInt(productIdColIndex);
                int nameColIndex = cursor.getColumnIndexOrThrow(ProductEntry.COLUMN_NAME);
                String name = cursor.getString(nameColIndex);
                int qtyColIndex = cursor.getColumnIndexOrThrow
                        (ProductEntry.COLUMN_QTY);
                int qty = cursor.getInt(qtyColIndex);
                String qtyString = String.valueOf(qty);
                int priceColIndex = cursor.getColumnIndexOrThrow(ProductEntry.COLUMN_PRICE);
                double price = cursor.getDouble(priceColIndex);
                productCardView.setOnClickListener(this);
                sellBtn.setOnClickListener(this);

                nameView.setText("Name: " + name);
                quantityView.setText("Qty in Stock: " + qtyString);
                priceView.setText("Price: " + String.valueOf(price));
            }
        }

        @Override
        public void onClick(View view) {
            Uri productUri = ContentUris.withAppendedId(ProductEntry.PRODUCTS_CONTENT_URI, productId);
            Intent openDetail = new Intent(context, DetailActivity.class);
            openDetail.setData(productUri);
            ContentValues contentValues = new ContentValues();
            switch (view.getId()) {
                case R.id.product_cardview:
                    Log.v(LOG_TAG, "onClick: productUri= " + productUri);
                    context.startActivity(openDetail);
                    break;
                case R.id.sell_btn:
                    int newQuantity;
                    if (qty >= 1) {
                        newQuantity = qty - 1;
                        Log.v(LOG_TAG, "newQty= " + newQuantity);
                        contentValues.put(ProductContract.ProductEntry.COLUMN_QTY, newQuantity);
                    } else {
                        Toast.makeText(context, context.getString(R.string.invalid_quantity_reset_zero),
                                Toast.LENGTH_SHORT)
                                .show();
                        newQuantity = 0;
                        contentValues.put(ProductContract.ProductEntry.COLUMN_QTY, newQuantity);
                    }
                    int numRowsUpdated = context.getContentResolver().update(productUri, contentValues, null, null);
                    context.startActivity(openDetail);
            }
        }
    }

    /*** Created by melanieh on 1/27/17. */

    private class CursorDataSetObserver extends DataSetObserver {

        public CursorDataSetObserver() {
            super();
        }

        @Override
        public void onChanged() {
            super.onChanged();
            mDataValid = true;
            notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            mDataValid = false;
            notifyDataSetChanged();
        }
    }
}

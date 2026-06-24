package com.app.myfriend.Ads;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.app.myfriend.R;

public class AdsPlansActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ads_plans);

        final CharSequence[] items = {"PayPal", "UPI/Debit Card/Credit Card", "Flutterwave", "SSLCommerz"};
        final int[] icons = {R.drawable.paypal, R.drawable.razorpay, R.drawable.flutterwave, R.drawable.sslcommerz};

        findViewById(R.id.basic).setOnClickListener(view -> {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Choose payment method");
            builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();
                }
            });
            for (int i = 0; i < items.length; i++) {
                builder.setIcon(icons[i]);
            }
            AlertDialog alert = builder.create();
            alert.show();

        });


        findViewById(R.id.medium).setOnClickListener(view -> {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Choose payment method");
            builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();
                }
            });
            for (int i = 0; i < items.length; i++) {
                builder.setIcon(icons[i]);
            }
            AlertDialog alert = builder.create();
            alert.show();

        });

        findViewById(R.id.advance).setOnClickListener(view -> {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Choose payment method");
            builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();
                }
            });
            for (int i = 0; i < items.length; i++) {
                builder.setIcon(icons[i]);
            }
            AlertDialog alert = builder.create();
            alert.show();

        });

    }
}
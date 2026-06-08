package com.app.myfriend.pages;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.app.myfriend.NightMode;
import com.app.myfriend.R;

import java.util.HashMap;

public class CreatePageActivity extends AppCompatActivity {

    String selectedItem = "";

    NightMode sharedPref;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPref = new NightMode(this);
        if (sharedPref.loadNightModeState().equals("night")){
            setTheme(R.style.DarkTheme);
        }else if (sharedPref.loadNightModeState().equals("dim")){
            setTheme(R.style.DimTheme);
        }else setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_page);

        findViewById(R.id.imageView).setOnClickListener(view -> onBackPressed());

        TextView code = findViewById(R.id.code);

        findViewById(R.id.showCat).setOnClickListener(view -> {
            final String[] items = {"Business/Industry", "Entertainment", "News/Media", "Travel", "Food/Restaurants", "Shopping/Retail", "Health/Wellness", "Sports/Fitness", "Technology/Science", "Personal/Lifestyle"};

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select an category");

            int checkedItem = 0;
            builder.setSingleChoiceItems(items, checkedItem, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    selectedItem = items[selectedPosition];
                    code.setText(selectedItem);
                    dialog.dismiss();
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();

        });

        EditText name = findViewById(R.id.email);
        EditText bio = findViewById(R.id.pass);
        EditText link = findViewById(R.id.name);
        EditText username = findViewById(R.id.username);

        findViewById(R.id.signUp).setOnClickListener(view -> {
            if (name.getText().toString().isEmpty() || username.getText().toString().isEmpty()){
                Snackbar.make(view, "Enter name & username", Snackbar.LENGTH_LONG).show();
            }else {
                String timeStamp = String.valueOf(System.currentTimeMillis());
                HashMap<Object, String> hashMap = new HashMap<>();
                hashMap.put("id", FirebaseAuth.getInstance().getCurrentUser().getUid());
                hashMap.put("pId", timeStamp);
                hashMap.put("name", name.getText().toString());
                hashMap.put("bio", bio.getText().toString());
                hashMap.put("link", link.getText().toString());
                hashMap.put("username", username.getText().toString());
                hashMap.put("photo", "");
                hashMap.put("cover", "");
                hashMap.put("cat", selectedItem);
                FirebaseDatabase.getInstance().getReference().child("Pages").child(timeStamp).setValue(hashMap);
                Toast.makeText(this, "Created", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, PagesProfileActivity.class);
                intent.putExtra("id", timeStamp);
                startActivity(intent);
            }
        });

    }
}
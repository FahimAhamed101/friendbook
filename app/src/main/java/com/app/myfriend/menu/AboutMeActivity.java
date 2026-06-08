package com.app.myfriend.menu;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.webkit.WebView;
import android.widget.TextView;

import com.app.myfriend.R;

public class AboutMeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy);

        TextView textView2 = findViewById(R.id.textView2);
        textView2.setText("About Me");

        WebView webView = findViewById(R.id.webView);
        findViewById(R.id.imageView).setOnClickListener(v -> onBackPressed());
        webView.requestFocus();
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setGeolocationEnabled(true);
        webView.setSoundEffectsEnabled(true);
        webView.loadData("<!DOCTYPE html>\n" +
                        "    <html>\n" +
                        "    <head>\n" +
                        "      <meta charset='utf-8'>\n" +
                        "      <meta name='viewport' content='width=device-width'>\n" +
                        "      <title>Terms &amp; Conditions</title>\n" +
                        "      <style> body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; padding:1em; } </style>\n" +
                        "    </head>\n" +
                        "    <body>\n" +
                        "                     FROM CEO DESK\n" +
                        "Introducing Debangshu Bose visionary owner and founder of Myfriend, a dynamic social networking site that brings people together in a digital realm. \n" +
                        "Hailing from the captivating region of Assam, India, Debangshu encapsulates the spirit of innovation and community-building. \n" +
                        "With a deep passion for technology and a keen understanding of human connection, Debangshu embarked on a mission to create a platform that transcends boundaries, allowing individuals from all walks of life to engage, share, and connect. Through his leadership and entrepreneurial spirit, Myfriend has flourished into a vibrant digital landscape that embraces cultural diversity and fosters meaningful interactions. Debangshu's tireless dedication to building bridges between individuals, even in the remote corners of India, has made Myfriend a revolutionary force in the world of social networking. \n" +
                        "As an advocate for inclusive communication and empowerment, Debangshu's vision continues to shape the way we connect and engage in the ever-evolving digital age.\n",
                "text/html", "UTF-8");

    }
}
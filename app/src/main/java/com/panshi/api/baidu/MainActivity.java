package com.panshi.api.baidu;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by Yangshoule on 2016/8/25.
 */
public class MainActivity extends Activity {
    private TextView info;
    double latitude;
    double longitude;
    String locationAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btn = (Button) findViewById(R.id.btn);
        info = (TextView) findViewById(R.id.text);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BaiduMapActivity.class);
                startActivityForResult(intent, 10001);
            }
        });
        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BaiduMapActivity.class);
                intent.putExtra(BaiduMapActivity.LATITUDE, latitude);
                intent.putExtra(BaiduMapActivity.LONGITUDE, longitude);
                intent.putExtra(BaiduMapActivity.ADDRESS, locationAddress);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 10001:
                    latitude = data.getDoubleExtra("latitude", 0);
                    longitude = data.getDoubleExtra("longitude", 0);
                    locationAddress = data.getStringExtra("address");
                    String locationName = data.getStringExtra("name");
                    String str = "latitude: " + latitude + "\n" + "longitude: " + longitude + "\n" +
                            "locationAddress: " + locationAddress + "\n" +
                            "locationName: " + locationName;
                    info.setText(str);
                    break;
            }
        }
    }
}


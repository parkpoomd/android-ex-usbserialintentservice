package com.example.deer.exappusbserialintentservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.deer.exappusbserialintentservice.service.UsbService;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private UsbService usbService;

    TextView tvDisplayData;
    EditText etSendData;
    Button btnSendData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvDisplayData = (TextView) findViewById(R.id.tvDisplayData);
        etSendData = (EditText) findViewById(R.id.etSendData);
        btnSendData = (Button) findViewById(R.id.btnSendData);

        // Thread Method : IntentService
        LocalBroadcastManager.getInstance(MainActivity.this)
                .registerReceiver(usbServiceBroadcastReceiver, new IntentFilter("UsbServiceIntentServiceUpdate"));

        Intent intent = new Intent(MainActivity.this, UsbService.class);
        startService(intent);

        btnSendData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String data = etSendData.getText().toString();
                try {
                    usbService = new UsbService();
                    usbService.write(data.getBytes(), 1000);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    protected BroadcastReceiver usbServiceBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Works on Main Thread
            String message = intent.getStringExtra("onNewData");
            tvDisplayData.setText(message);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(MainActivity.this)
                .unregisterReceiver(usbServiceBroadcastReceiver);
    }
}

package com.school.wetype;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

public class AirplaneModeBroadcastReceiver extends BroadcastReceiver {
    private final MainActivity mainActivity;

    public AirplaneModeBroadcastReceiver(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == Intent.ACTION_AIRPLANE_MODE_CHANGED)
        {
            boolean state = intent.getBooleanExtra("state", false);
            String messageText;

            if (state) {
                messageText = "Airplane mode is on";
            } else {
                messageText = "Airplane mode is off";
            }

            Toast.makeText(mainActivity, messageText, Toast.LENGTH_SHORT).show();
        }
    }
}

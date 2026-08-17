package dev.r51mt02.captureprobe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class ProbeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ProbeActivity.executeProbe(context.getApplicationContext());
    }
}

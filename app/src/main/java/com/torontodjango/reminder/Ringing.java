package com.torontodjango.reminder;

import java.util.Timer;
import java.util.TimerTask;
import android.app.Activity;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.provider.Settings;


public class Ringing extends Activity {

    private final String TAG = "Ringing";

    private class PlayTimerTask extends TimerTask
    {
        @Override
        public void run()
        {
            Log.d(TAG, "PalyTimerTask.run()");
            finish();
        }
    }

    private Ringtone ringtone;
    private long playTime;
    private Timer timer = null;
    private PlayTimerTask playTimerTask;

    Task task;
    private TextView textView;

    @Override
    protected void onCreate(Bundle bundle)
    {
        Log.i(TAG, "onCreate");

        super.onCreate(bundle);

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.ringing);

        textView = findViewById(R.id.name);

        playTime = (long)30000;
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), Settings.System.DEFAULT_RINGTONE_URI);

        start(getIntent());
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        stop();
    }

    private void start(Intent intent)
    {

        Log.d(TAG, "Start ringing...");


        task = new Task();
        task.fromIntent(intent);

        textView.setText(task.getName());

        playTimerTask = new PlayTimerTask();
        timer = new Timer();
        timer.schedule(playTimerTask, playTime);
        ringtone.play();
    }

    private void stop()
    {
        Log.d(TAG, "Stop ringing...");
        timer.cancel();
        ringtone.stop();
    }

    public void onDismissClick(View view)
    {
        // Intent Intent1 = new Intent(this, TasksFragment.class);
        //task.toIntent(Intent1);
        //String name=task.getName();
        //Intent1.putExtra("id1",0);

       finish();
        moveTaskToBack(true);
        System.exit(0);
        // startActivity(Intent1);



    }


    @Override
    public void onBackPressed()
    {
        finish();
    }

}

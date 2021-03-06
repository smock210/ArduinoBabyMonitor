package ru.antsharapov.arduinobabymonitor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {

    String res, url, time;
    Timer timer = new Timer();
    TextView temp_lvl, humi_lvl, adc, pres_lvl;
    ProgressBar snd_lvl;
    int sound_level;
    View root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final SharedPreferences prefs = this.getSharedPreferences("ru.antsharapov.arduinobabymonitor", Context.MODE_PRIVATE);
        url=prefs.getString("url", "");
        time=prefs.getString("time", "");

        if (url.equals("")) url="http://192.168.1.128/";
        if (time.equals("")) time="2";

        temp_lvl = (TextView) findViewById(R.id.templevelTV);
        humi_lvl = (TextView) findViewById(R.id.humilevelTV);
        adc = (TextView) findViewById(R.id.adcTV);
        snd_lvl = (ProgressBar) findViewById(R.id.progressBar);
        pres_lvl = (TextView) findViewById(R.id.pres_edit_tv);

        snd_lvl.setScaleY(10f);

        start_timer();

        Button exit_btn = (Button) findViewById(R.id.exitbutton);
        exit_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop_timer();
                finish();
                System.exit(0);
            }
        });

        ImageButton pref_btn = (ImageButton) findViewById(R.id.imageButton);
        pref_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                alert.setTitle("Preferences:");

                LinearLayout layout = new LinearLayout(MainActivity.this);
                layout.setOrientation(LinearLayout.VERTICAL);

                final EditText url_textbox = new EditText(MainActivity.this);
                layout.addView(url_textbox);
                url_textbox.setText(url);

                final EditText timer_textbox = new EditText(MainActivity.this);
                layout.addView(timer_textbox);
                timer_textbox.setText(time);

                alert.setView(layout);

                alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        url = url_textbox.getText().toString();
                        time = timer_textbox.getText().toString();

                        prefs.edit().putString("url", url).apply();
                        prefs.edit().putString("time", time).apply();

                        Toast.makeText(MainActivity.this, "Saved Successfully", Toast.LENGTH_SHORT).show();
                    }
                });

                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });

                alert.show();
            }
        });

        }

    public static String getHtml(String url) throws IOException {
        URLConnection connection = (new URL(url)).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.connect();
        InputStream in = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder html = new StringBuilder();
        for (String line; (line = reader.readLine()) != null; ) {
            html.append(line);
        }
        in.close();
        return html.toString();
    }

    public void start_timer (){
        final ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        this.timer = new Timer();
        this.timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    res = getHtml(url);
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        res = Html.fromHtml(res,Html.FROM_HTML_MODE_LEGACY).toString();
                    } else {
                        res = Html.fromHtml(res).toString();
                    }
                    final String[] result = res.split(":");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Toast.makeText(getApplicationContext(),res,Toast.LENGTH_LONG).show();
                            temp_lvl.setText(result[0]+" °C");
                            humi_lvl.setText(result[1]+" %");
                            sound_level = 1023 - Integer.parseInt(result[2]);
                            snd_lvl.setProgress(sound_level);
                            adc.setText((sound_level) + " / 1023");
                            if (sound_level>=510 && sound_level <= 520)
                            {
                                root = findViewById(android.R.id.content);
                                root.setBackgroundColor(Color.parseColor("#f0e5a5"));
                                toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                                try {
                                    Thread.sleep(300);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                toneG.stopTone();
                            }
                            else if (sound_level>520)
                            {
                                root = findViewById(android.R.id.content);
                                root.setBackgroundColor(Color.parseColor("#ff4f00"));
                                toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1000);
                                try {
                                    Thread.sleep(1100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                toneG.stopTone();
                            }
                            else
                            {
                                root = findViewById(android.R.id.content);
                                root.setBackgroundColor(Color.parseColor("#c5edd7"));
                            }
                            pres_lvl.setText(result[3]+" mmHg");
                        }
                    });
                } catch (final IOException e) {
                   /* runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
                        }
                    });*/
                    e.printStackTrace();
                }
            }
        },0,Integer.parseInt(time)*1000);
    }

    public void stop_timer(){
        this.timer.cancel();
    }

}

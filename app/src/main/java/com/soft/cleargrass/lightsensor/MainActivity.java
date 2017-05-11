package com.soft.cleargrass.lightsensor;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.database.ContentObservable;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private static Context context;
    private TextView screenTv, lightTv;
    private Button startBtn, endBtn;
    SensorManager sensorManager;
    FileOutputStream outputStream;
    ArrayList<String> lightData = new ArrayList<>();
    float value;
    private NewTimerTask task;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        screenTv = (TextView)findViewById(R.id.screenTv);
        lightTv = (TextView)findViewById(R.id.lightnessTv);
        startBtn = (Button)findViewById(R.id.startBtn);
        endBtn = (Button)findViewById(R.id.endBtn);

        context = this;
//        startAutoBrightness();


        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        final Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sensorManager.registerListener( listener, sensor, SensorManager.SENSOR_DELAY_NORMAL);

                if (timer != null){
                    if (task!=null){
                        task.cancel();
                    }
                }

                task = new NewTimerTask();

                timer.schedule(task,0,500);
            }
        });

        endBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sensorManager != null) {
                    sensorManager.unregisterListener(listener);
                }
                final EditText et = new EditText(context);
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
                builder.setTitle("请输入文件名").setIcon(android.R.drawable.ic_dialog_info).setView(et)
                        .setNegativeButton("Cancel", null);
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        String data = et.getText().toString();
                        saveData(data);
                    }
                });
                builder.show();

            }
        });

    }

    SensorEventListener listener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {

            value = event.values[0];


            float screenValue = getWindow().getAttributes().screenBrightness;

//            Log.d("brightness", String.valueOf(screenValue));

            getApplicationContext().getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true , contentObserver);


//            Log.d("setBright", String.valueOf(getScreenBrightness()));




        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

    };

    public void saveData(String filename){
//        FileHelper helper = new FileHelper(context);
//        String filename = "light_value";
//        try{
//            helper.save(filename, lightData.toString());
//            Toast.makeText(getApplicationContext(), "数据写入成功", Toast.LENGTH_SHORT).show();
//        } catch (Exception e) {
//            e.printStackTrace();
//            Toast.makeText(getApplicationContext(), "数据写入失败", Toast.LENGTH_SHORT).show();
//        }
        try {
            File file = new File("/sdcard/lightvalue/"+ filename+".txt");
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter("/sdcard/lightvalue/" +  filename+".txt");
            fw.flush();
            fw.write(lightData.toString());
            fw.close();
            Toast.makeText(getApplicationContext(), "数据写入成功", Toast.LENGTH_SHORT).show();
//        } catch (Exception e) {
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "数据写入失败", Toast.LENGTH_SHORT).show();
        }
    }

    public File makeFilePath(String filePath, String fileName) {
        File file = null;
        makeRootDirectory(filePath);
        try {
            file = new File(filePath + fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    public static void makeRootDirectory(String filePath) {
        File file = null;
        try {
            file = new File(filePath);
            if (!file.exists()) {
                file.mkdir();
            }
        } catch (Exception e) {
            Log.i("error:", e + "");
        }
    }


    ContentObserver contentObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
        }
    };

    public int getScreenBrightness() {
        int value = 0;
        try {
            value = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {

        }
        return value;
    }


    private Handler handler = new Handler(){
        public void handleMessage(Message msg){
            super.handleMessage(msg);

            if (msg.what == 1){

                String cmd = "tail /sys/class/leds/lcd-backlight/brightness";
                try {
                    java.lang.Process process = Runtime.getRuntime().exec(cmd);
                    BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()), cmd.length());
                    String data;
                    if ((data = input.readLine())!= null){
                        lightTv.setText("光线传感器数值: " + value);
                        screenTv.setText("屏幕亮度: " + data);
                        Log.d("dataValue","光线传感器:" + value +"  屏幕亮度:" + data );
                        lightData.add("光线传感器:" + value +"  屏幕亮度:" + data +"\r\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private Timer timer = new Timer(true);

    class NewTimerTask extends TimerTask {
        @Override
        public void run() {
            Message message = new Message();
            message.what = 1;
            handler.sendMessage(message);
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
//        if (timer != null){
//            if (task!=null){
//                task.cancel();
//            }
//        }
//
//        task = new NewTimerTask();
//
//        timer.schedule(task,0,1000);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

        if (sensorManager != null) {
            sensorManager.unregisterListener(listener);
        }
        timer.cancel();
    }

    public static void startAutoBrightness() {
        Settings.System.putInt(context.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        Uri uri = android.provider.Settings.System.getUriFor("screen_brightness");
        context.getContentResolver().notifyChange(uri, null);
    }

}

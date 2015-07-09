package com.teamloop.ncumis.loop;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

import java.util.LinkedList;


public class RecordActivity extends Activity {

    public static final String TAG = "recorderActivity";
    private SoundAnalyzer soundAnalyzer;
    private Boolean recordFlag = true;
    private ImageButton recordBtn;

    public ProgressDialog progressDialog;

    protected final int PROGRESS_STOP_NOTIFIER = 0x1008;
    protected final int PROGRESS_THREADING_NOTIFIER = 0x1009;

    private static final String notes[] =
            {"A", "B", "B", "C", "C", "D", "E", "E", "F", "F", "G", "G"};

    private static final String shapes[] =
            {" ", "\u266D", " ", " ", "\u266F", " ", "\u266D", " ", " ", "\u266F", " ", "\u266F"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        recordBtn = (ImageButton) findViewById(R.id.recordBtn);

        recordBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(!recordFlag){    // 代表現在還沒錄音
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        //若壓住錄音鈕要換色
                        recordBtn.setImageResource(R.drawable.rec_feedback);
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        //若放開停止鈕要變回來(沒有按下只有壓)
                        recordBtn.setImageResource(R.drawable.rec_icon);
                    }
                }
                if(recordFlag){     // 代表已經開始錄音
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        //若壓住停止鈕要換色
                        recordBtn.setImageResource(R.drawable.stop_rec_feedback);
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        //若放開停止鈕要變回來(沒有按下只有壓)
                        recordBtn.setImageResource(R.drawable.stop_rec_icon);
                    }
                }
                return false;
            }
        });

        // 錄音鍵Listener
        recordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!recordFlag) {  // 代表現在還沒錄音
                    // click record button
                    recordBtn.setImageResource(R.drawable.stop_rec_icon);
                    startRecord();
                } else {
                    // click stop button
                    stopRecord();
                }
            }
        });

        // new 一個SoundAnalyzer實體
        try {
            soundAnalyzer = new SoundAnalyzer();
        } catch (Exception e) {
            Log.e(TAG, "Exception when instantiating SoundAnalyzer: " + e.getMessage());
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    // Start the sound Recorder
    private void startRecord() {
        recordFlag = true;
        Log.d(TAG, "onStart()");
        if (soundAnalyzer != null) {
            soundAnalyzer.start();
        }
    }

    // Stop the Recorder
    private void stopRecord() {

        // 產生ProgressDialog告訴使用者目前轉換的進度

        final CharSequence strDialogbody = getString(R.string.progressbar_body);

        progressDialog = new ProgressDialog(RecordActivity.this, R.style.dialog);
        progressDialog.setMessage(strDialogbody);
        progressDialog.setMax(100);
        progressDialog.setProgress(0);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setIndeterminate(false);

        progressDialog.show();

        recordFlag = false;     // 停止錄音
        soundAnalyzer.stop();

        final Intent intent = new Intent();         // 產生scoreActivity等下要轉換
        intent.setClass(this, ScoreActivity.class);


        new Thread(new Runnable() {     // 產生progress dialog動畫
            @Override
            public void run() {
                transformSoundData(intent);     // transformSoundData方法透過intent傳送資料
                progressDialog.dismiss();       // 消除progressDialog
            }
        }).start();

        startActivity(intent);          // 切換intent
       // RecordActivity.this.finish();

    }

    /* 處理音訊資料傳遞, 並透過intent傳遞資料
       在最後面還有和progress dialog結合 產生動畫    */

    private void transformSoundData(Intent it) {
        Log.d(TAG,"Enter transformSoundData()");
        LinkedList frequencyList;
        frequencyList = soundAnalyzer.getFrequencyMessage();

        int nodeNum = frequencyList.size();
        it.putExtra("nodeNum", nodeNum);    // 把list的大小傳到新activity

        int initPitch = 0;
        int initKey = 4;
        int cf, pitch, key;
        double f;
        long L;
        for (int num = 0; num < nodeNum; num++) {
            //  transform the frequency number to the pitch and the key

            String s = frequencyList.pop().toString();
            String frqNode[] = s.split(",");
            Long time = Long.parseLong(frqNode[1]);

            f = Double.parseDouble(frqNode[0]);
            L = Math.round(12 * log2(f / 440));     // ***用四捨五入去調整range,但可能要用if去調整range比較好
            cf = (int) L;
            pitch = initPitch + cf % 12;    // update the pitch
            if (pitch < 0)
                pitch += 12;
            key = initKey + cf / 12;        // update the key

            // 透過intent 傳遞資料 (String型態)
            it.putExtra("node" + num, notes[pitch] + shapes[pitch] + "," + key + "," + time);
            //Log.d(TAG,""+notes[pitch]+shapes[pitch]+","+key+","+time);


            // 把進度用setProgress傳出，告訴使用者進度
            double d = (100*num)/nodeNum;
            int sendNum = (int)d;

            try
            {
                progressDialog.setProgress(sendNum);
                Thread.sleep(200);  // 每次停0.2s 製造動畫感
            }
            catch (Exception e)
            {
                Log.e(TAG, "Exception when running thread transformSoundData: " + e.getMessage());
            }

        }

        progressDialog.setProgress(100);    // 全部結束後要回傳進度100%
    }

    // method - 計算log2為底
    private double log2(double num) {
        return Math.log(num) / Math.log(2.0);
    }


    // 處理如何關閉程式
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {

        if ((keyCode == KeyEvent.KEYCODE_BACK)) {   //確定按下退出鍵

            confirmExit(); //呼叫confirmExit()函數

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    public void confirmExit(){
        AlertDialog.Builder dialog = new AlertDialog.Builder(RecordActivity.this); //創建訊息方塊

        dialog.setTitle("離開");
        dialog.setMessage("確定要離開?");
        dialog.setPositiveButton("是", new DialogInterface.OnClickListener() { //按"是",則退出應用程式

            public void onClick(DialogInterface dialog, int i) {

                android.os.Process.killProcess(android.os.Process.myPid());
            }

        });

        dialog.setNegativeButton("否",new DialogInterface.OnClickListener() { //按"否",則不執行任何操作

            public void onClick(DialogInterface dialog, int i) {

            }

        });

        dialog.show();//顯示訊息視窗
    }

}
package com.teamloop.ncumis.loop;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.os.Bundle;

import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import nu.xom.Document;
import nu.xom.Serializer;


public class RecordActivity extends Activity {

    public static final String TAG = "recorderActivity";
    private SoundAnalyzer soundAnalyzer;
    private int recordFlag = 0;
    private ImageButton recordBtn;
    public ProgressDialog progressDialog;
    private double beatCtr = 0.0;   //  用來計算拍子以換小節
    private String outputFileName = "";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);



        /*
        * recordBtn是錄音鍵，用來開始與結束錄音
        * 以下還有onTouchListener與onClickListener
        * */

        recordBtn = (ImageButton) findViewById(R.id.recordBtn);
        recordBtn.setImageResource(R.drawable.rec_icon);

        // recordBtn 的touchListener
        recordBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(recordFlag == 0){    // 代表現在還沒錄音
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        //若壓住錄音鈕要換色
                        recordBtn.setImageResource(R.drawable.rec_feedback);
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        //若放開停止鈕要變回來(沒有按下只有壓)
                        recordBtn.setImageResource(R.drawable.rec_icon);
                    }
                }
                if(recordFlag == 1){     // 代表已經開始錄音
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

        // 錄音鍵Click Listener
        recordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recordFlag == 0) {  // 代表現在還沒錄音
                    // click record button
                    recordBtn.setImageResource(R.drawable.stop_rec_icon);
                    startRecord();
                } else {
                    // click stop button
                    confirmStopRecord();
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

        switch (id)
        {
            case  R.id.action_settings:
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    // Start the sound Recorder
    private void startRecord() {
        recordFlag = 1;
        Log.d(TAG, "recorder onStart()");
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

        recordFlag = 0;     // 停止錄音
        soundAnalyzer.stop();

        final Intent intent = new Intent();         // 產生scoreActivity等下要轉換
        intent.setClass(RecordActivity.this, ScoreActivity.class);


        new Thread(new Runnable() {     // 產生progress dialog動畫
            @Override
            public void run() {
                transformSoundData(intent);     // transformSoundData方法透過intent傳送資料

                try{

                    Thread.sleep(1000);             // 休息一秒
                    progressDialog.dismiss();       // 再消除progressDialog
                }
                catch (Exception e)
                {
                    Log.e(TAG,"Exception when after transforming data sleeping.");
                }
                finally {
                    Bundle bundle = new Bundle();
                    bundle.putString("fileName",getOutputFileName());

                    intent.putExtras(bundle);

                    startActivity(intent);          // 切換intent
                    RecordActivity.this.finish();
                }
            }
        }).start();

    }

    /* 處理音訊資料傳遞, 並透過intent傳遞資料
       在最後面還有和progress dialog結合 產生動畫    */

    private void transformSoundData(Intent it) {
        NoteAnalyzer noteAnalyzer = new NoteAnalyzer();     // 初始化NoteAnalyzer

        LinkedList frequencyList;
        frequencyList = soundAnalyzer.getFrequencyMessage();    // 從SoundAnalyzer接受到頻率訊息

        LinkedList noteList = noteAnalyzer.analyzeNote(frequencyList);      // 將頻率訊息丟進noteAnalyzer處理

        TempleAnalyzer templeAnalyzer = new TempleAnalyzer();   // 初始化TempleAnalyzer

        LinkedList dataList;                                // 將音符訊息丟進TempleAnalyzer處理
        dataList = templeAnalyzer.analyzeTemple(noteList, 80);          //並回傳dataList

        int nodeNum = dataList.size();  // 計算dataList的節點數

        //Bundle bundle = new Bundle();   // new 一個Bundle物件，用來傳遞資料
        //bundle.putInt("nodeNum", nodeNum);  // 把總節點數放入Bundle


        //  製作musicXML
        MusicXmlRenderer mxlRender = new MusicXmlRenderer();
        mxlRender.newVoice();
        mxlRender.measureEvent();
        mxlRender.changeSystemEvent(false);

        int measureCtr = -1; //  用來計算小節以換行

        //  將每一個note轉換成mxl格式
        for( int num=0; num<nodeNum; num++  )
        {
            String data = dataList.pop().toString();
            String node[] = data.split(",");
            String value = node[0];
            int key = Integer.parseInt(node[1]);
            int beat = Integer.parseInt(node[2]);
            //bundle.putString("node"+num,data);  // 把每個節點的資料放入Bundle

            countBeat(beat);

            // Doing Quantization
            // if next voice exceed the measure size then stuffing a rest
            if(beatCtr > 4.0)
            {
                double temp = 0;
                switch (beat)
                {
                    case 4:
                        temp = 1.0;
                        break;
                    case 8:
                        temp = 0.5;
                        break;
                    case 16:
                        temp = 0.25;
                        break;
                }
                double quantizeBeat = (4 - (beatCtr - temp));

                int quantizeBeat2;
                switch (""+quantizeBeat)
                {
                    case "0.25":
                        quantizeBeat2 = 16;
                        mxlRender.noteEvent("rest"+"n\u0020",4,quantizeBeat2);
                        break;
                    case "0.5":
                        quantizeBeat2 = 8;
                        mxlRender.noteEvent("rest"+"n\u0020",4,quantizeBeat2);
                        break;
                    case "0.75":
                        quantizeBeat2 = 8;
                        mxlRender.noteEvent("rest"+"n\u0020",4,quantizeBeat2);
                        quantizeBeat2 = 16;
                        mxlRender.noteEvent("rest"+"n\u0020",4,quantizeBeat2);
                        break;
                }
                mxlRender.measureEvent();
                setBeatCtrZero();   // set beatCtr 0
                measureCtr++;
                if (measureCtr == 0) //  每兩個小節要換行
                {
                    mxlRender.changeSystemEvent(true);
                }else {
                    mxlRender.changeSystemEvent(false);
                    measureCtr = -1;
                }

                mxlRender.noteEvent(value, key, beat);
                countBeat(beat);
            }
            else if(beatCtr == 4.0)    //  the beat counter equal to 4 exactly
            {
                mxlRender.noteEvent(value, key, beat);
                mxlRender.measureEvent();
                setBeatCtrZero();   // set beatCtr 0
                measureCtr++;

                if (measureCtr == 0) //  每兩個小節要換行
                {
                    mxlRender.changeSystemEvent(true);
                }else {
                    mxlRender.changeSystemEvent(false);
                    measureCtr = -1;
                }
            }
            else
            {  // normal situation
                mxlRender.noteEvent(value, key, beat);
            }

            // 把進度用setProgress傳出，告訴使用者進度
            double d = (100*num)/nodeNum;
            int sendNum = (int)d;

            try
            {
                progressDialog.setProgress(sendNum);
                Thread.sleep(100);  // 每次停0.05s 製造動畫感
            }
            catch (Exception e)
            {
                Log.e(TAG, "Exception when running thread transformSoundData: " + e.getMessage());
            }
        }
        //it.putExtras(bundle);   // 把整個Bundle安排給Intent

        OutputFile(mxlRender);
        progressDialog.setProgress(100);    // 全部結束後要回傳進度100%*/

    }

    private void setOutputFileName(String s)
    {
        outputFileName = s;
    }

    private String getOutputFileName()
    {
        return outputFileName;
    }

    /* Output the musicXML temp file to cellPhone internal storage as an asset
    *  This temp file will be process soon
    */
    private void OutputFile(MusicXmlRenderer musicXmlRenderer)
    {
        // set the saving directory
        // default dir is /data/data/packageName/files/
        File savingDir = getFilesDir();

        // generate a xml file named by dateTime
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = simpleDateFormat.format(new java.util.Date());
        File fileXML = new File(savingDir, date+".xml");

        setOutputFileName(date);

        FileOutputStream fosXML;
        try{
            fosXML = new FileOutputStream(fileXML);

            Document mxlDoc = musicXmlRenderer.getMusicXMLDoc();

            //	write the MusicXML file formatted
            Serializer ser = new Serializer(fosXML, "UTF-8");
            ser.setIndent(4);
            ser.write(mxlDoc);

            fosXML.close();
        }
        catch (FileNotFoundException fileNotFoundException)
        {
            Log.e(TAG,"found fileNotFoundException when generate a file.");
        }
        catch (IOException  ioException)
        {
            Log.e(TAG,"found ioException when serialize the musicXML.");
        }

    }


    // Output the musicXML file to cellPhone external storage
    private void OutputFile_To_External_Storage(MusicXmlRenderer musicXmlRenderer)
    {
        File savingDir = null;
        /*
        *  if Loop dir isn't exist in the sdcard
        *  then new a directory
        * */
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
        {
            File sdcard = Environment.getExternalStorageDirectory();
            String path = sdcard.getAbsolutePath() + "/Loop";

            savingDir = new File(path);
            if(!savingDir.exists())   // if directory isn't exist
            {
                savingDir.mkdir();    // make a directory
            }
        }

        // generate a file named by dateTime
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = simpleDateFormat.format(new java.util.Date());
        File fileXML = new File(savingDir, date+".xml");
        try {
            FileOutputStream fosXML = new FileOutputStream(fileXML, false);

            Document mxlDoc = musicXmlRenderer.getMusicXMLDoc();

            //	write the MusicXML file formatted
            Serializer ser = new Serializer(fosXML, "UTF-8");
            ser.setIndent(4);
            ser.write(mxlDoc);

            //Log.d(TAG,""+musicXmlRenderer.getMusicXMLString());

            fosXML.close();
        }
        catch (FileNotFoundException fileNotFoundException)
        {
            Log.e(TAG,"found fileNotFoundException when generate a file.");
        }
        catch (IOException  ioException)
        {
            Log.e(TAG,"found ioException when serialize the musicXML.");
        }
    }

    // 處理按下手機退出鍵會關閉程式

    private static Boolean isExit = false;

    Timer timer = new Timer();

    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {

        if ((keyCode == KeyEvent.KEYCODE_BACK)) {   //確定按下退出鍵

            if(isExit == false)
            {
                isExit = true;
                Toast.makeText(RecordActivity.this, "再按一次返回鍵退出程式", Toast.LENGTH_SHORT).show();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(2000);
                            isExit = false;
                        }
                        catch (InterruptedException e)
                        {
                            Log.e(TAG,"InterruptedException Occurred when onKeyDown method");
                        }
                    }
                }).start();
            }
            else {
                android.os.Process.killProcess(android.os.Process.myPid());
                return true;
            }
        }
        return false;
    }

    private void confirmExit()
    {



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
    }   // end method confirmExit


    // confirm stop record
    private void confirmStopRecord()
    {
        AlertDialog.Builder dialog = new AlertDialog.Builder(RecordActivity.this);
        dialog.setTitle("停止");
        dialog.setMessage("確定停止錄音?");
        dialog.setPositiveButton("是", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                stopRecord();
            }
        });

        dialog.setNegativeButton("否", new DialogInterface.OnClickListener() { //按"否",則不執行任何操作

            public void onClick(DialogInterface dialog, int i) {

            }

        });

        dialog.show();//顯示訊息視窗
    }

    // count the beat to make quantize
    private void countBeat(int beat)
    {
        switch (beat)
        {
            case 4:
                beatCtr += 1.0;
                break;
            case 8:
                beatCtr += 0.5;
                break;
            case 16:
                beatCtr += 0.25;
                break;
        }
    }

    // a set method that set the beat counter as zero
    private void setBeatCtrZero()
    {
        beatCtr = 0;
    }

}
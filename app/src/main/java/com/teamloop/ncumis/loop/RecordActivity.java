package com.teamloop.ncumis.loop;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
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


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

import nu.xom.Document;
import nu.xom.Serializer;


public class RecordActivity extends Activity {

    public static final String TAG = "recorderActivity";
    private SoundAnalyzer soundAnalyzer;
    private int recordFlag = 0;
    private ImageButton recordBtn;

    public ProgressDialog progressDialog;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
/*

        */
/*
        *   設定sliding menu 用來製作測邊功能鍵
        * *//*

        //  set the Behind View
        setBehindContentView(R.layout.menu_frame);

       if (savedInstanceState == null) {
           FragmentTransaction fragmentTransaction = this.getSupportFragmentManager().beginTransaction();
            mFrag = new MenuList();
            fragmentTransaction.replace(R.id.menu_frame, mFrag);
            fragmentTransaction.commit();
        } else {
            mFrag = (MenuList)this.getSupportFragmentManager().findFragmentById(R.id.menu_frame);
        }

        // customize the SlidingMenu
        SlidingMenu sm = getSlidingMenu();
        sm.setShadowWidthRes(R.dimen.shadow_width);
        sm.setShadowDrawable(R.drawable.shadow);
        sm.setBehindOffsetRes(R.dimen.sliding_menu_offset);
        sm.setFadeDegree(0.35f);
        sm.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);


        getActionBar().setDisplayHomeAsUpEnabled(true);

*/

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
                        Log.d(TAG, "locate3 -> Flag = " + recordFlag);
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        //若放開停止鈕要變回來(沒有按下只有壓)
                        recordBtn.setImageResource(R.drawable.stop_rec_icon);
                        Log.d(TAG, "locate4 -> Flag = " + recordFlag);
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
                    startActivity(intent);          // 切換intent
                    RecordActivity.this.finish();
                }
            }
        }).start();

    }

    /* 處理音訊資料傳遞, 並透過intent傳遞資料
       在最後面還有和progress dialog結合 產生動畫    */

    private void transformSoundData(Intent it) {
        Log.d(TAG,"Enter transformSoundData()");
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

        int beatCtr = 0;    //  用來計算拍子以換小節
        int measureCtr = 0; //  用來計算小節以換行

        //  將每一個note轉換成mxl格式
        for( int num=0; num<nodeNum; num++  )
        {
            String data = dataList.pop().toString();
            String node[] = data.split(",");
            String value = node[0];
            int key = Integer.parseInt(node[1]);
            int beat = Integer.parseInt(node[2]);
            //bundle.putString("node"+num,data);  // 把每個節點的資料放入Bundle

            mxlRender.noteEvent(value, key, beat);
            beatCtr = beatCtr + ( 4 * ( 4 / beat ) );

            if(beatCtr == 4)    //  每滿4個拍子要換小節
            {
                mxlRender.measureEvent();
                beatCtr = 0;
                measureCtr++;
            }

            if(measureCtr == 2) //  每兩個小節要換行
                mxlRender.changeSystemEvent(true);
            else
                mxlRender.changeSystemEvent(false);


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

        //Log.d("RRRRRRRRR","now~~"+mxlRender.getMusicXMLString());
        OutputFile(mxlRender);
        progressDialog.setProgress(100);    // 全部結束後要回傳進度100%*/

    }

    private void OutputFile(MusicXmlRenderer musicXmlRenderer)
    {
        /*
        *  if Loop dir isn't exist in the sdcard
        *  then new a directory
        * */
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
        {
            File sdFile = android.os.Environment.getExternalStorageDirectory();
            String path = sdFile.getPath() + File.separator + "Loop";

            File dirFile = new File(path);
            if(!dirFile.exists())   // if directory isn't exist
            {
                dirFile.mkdir();    // make a directory
            }
        }

        File sdcard = Environment.getExternalStorageDirectory();

        // generate a file named by dateTime
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String date = simpleDateFormat.format(new java.util.Date());
        File fileXML = new File(sdcard, date+".xml");
        try {
            FileOutputStream fosXML = new FileOutputStream(fileXML, false);


            Document mxlDoc = musicXmlRenderer.getMusicXMLDoc();
            String p = mxlDoc.toXML();
            fosXML.write(p.getBytes());

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
package com.teamloop.ncumis.loop;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.LinkedList;

public class ScoreActivity extends Activity {

    public static final String TAG = "scoreActivity";
    //TextView monitorLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "ScoreActivity onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.score_layout);

        //monitorLabel = (TextView)findViewById(R.id.monitorLabel);
        Intent it = getIntent();
        showScore(it);
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

    private void showScore(Intent it){
        Log.d(TAG,"show the Score.");

        int nodeNum = it.getIntExtra("nodeNum", 0);
        Log.d(TAG, "recording Result size is :" + nodeNum);

        for(int num=0; num < nodeNum; num++)
        {
            String message = it.getStringExtra("node"+num);
            String node[] = message.split(",");
            String pitch = node[0];
            String key = node[1];
            String time = node[2];
            Log.d(TAG, message);
            //monitorLabel.append(String.format("%-5s%s%5s\t",pitch,key,time));
            if( (num%3) == 2) {
                Log.d(TAG,"\n");
                //monitorLabel.append("\n");
            }
        }

    }

    // 處理如何關閉程式
    /*public boolean onKeyDown(int keyCode, KeyEvent event) {

        if ((keyCode == KeyEvent.KEYCODE_BACK)) {   //確定按下退出鍵

            confirmExit(); //呼叫confirmExit()函數

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }*/

    public void confirmExit(){
        AlertDialog.Builder dialog = new AlertDialog.Builder(ScoreActivity.this); //創建訊息方塊

        dialog.setTitle("離開");
        dialog.setMessage("確定要離開?");
        dialog.setPositiveButton("是", new DialogInterface.OnClickListener() { //按"是",則退出應用程式

            public void onClick(DialogInterface dialog, int i) {

                android.os.Process.killProcess(android.os.Process.myPid());
            }

        });

        dialog.setNegativeButton("否", new DialogInterface.OnClickListener() { //按"否",則不執行任何操作

            public void onClick(DialogInterface dialog, int i) {

            }

        });

        dialog.show();//顯示訊息視窗
    }

}

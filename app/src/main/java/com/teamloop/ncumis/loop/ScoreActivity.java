package com.teamloop.ncumis.loop;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.LinkedList;

public class ScoreActivity extends ActionBarActivity {

    public static final String TAG = "scoreActivity";
    TextView monitorLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.score_layout);

        monitorLabel = (TextView)findViewById(R.id.monitorLabel);
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
        Log.d(TAG,"recording Result size is :"+nodeNum);

        for(int num=0; num < nodeNum; num++)
        {
            String message = it.getStringExtra("node"+num);
            String node[] = message.split(",");
            String pitch = node[0];
            String key = node[1];
            String time = node[2];
            Log.d(TAG, message);
            monitorLabel.append(String.format("%-5s%s%5s\t",pitch,key,time));
            if( (num%3) == 2) {
                Log.d(TAG,"\n");
                monitorLabel.append("\n");
            }
        }

    }

}

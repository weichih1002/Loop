package com.teamloop.ncumis.loop;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;

public class ScoreActivity extends Activity {

    public static final String TAG = "scoreActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "ScoreActivity onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.score_layout);

        LinearLayout lLayout = (LinearLayout)findViewById(R.id.linerLayout_sheet_h);

        Bundle bundle = this.getIntent().getExtras();
        showScore(bundle, lLayout);
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

    private void showScore(Bundle bundle, LinearLayout lLayout)
    {
        ScoreSetting settingScore = new ScoreSetting(ScoreActivity.this);
        int nodeNum = bundle.getInt("nodeNum");

        settingScore.setInitialX(120);
        for(int num=0; num < nodeNum; num++)
        {
            String message = bundle.getString("node"+num);
            if(message != null)
            {
                String node[] = message.split(",");
                String pitch = node[0];
                int key = Integer.parseInt(node[1]);
                int beat = Integer.parseInt(node[2]);
                settingScore.makingScore(pitch, key, beat, lLayout);
            }
            /*if(num == 9)
            {
                Log.d(TAG,"Have enter line");
                settingScore.setBeatCtr(0);
                settingScore.setInitialY(settingScore.getInitialY()+120);
            }*/
        }

    }


    // 處理如何關閉程式
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {

        if ((keyCode == KeyEvent.KEYCODE_BACK)) {   //確定按下退出鍵

            Intent intent = new Intent();         // 產生scoreActivity等下要轉換
            intent.setClass(ScoreActivity.this, RecordActivity.class);

            startActivity(intent);          // 切換intent
            ScoreActivity.this.finish();

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

}

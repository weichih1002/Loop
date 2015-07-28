package com.teamloop.ncumis.loop;


import android.app.Activity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class ScoreSetting {
    final static String TAG = "ScoreSetting";

    Activity activity;
    double beatCtr = 0.0;
    int initialY = 100;
    int initialX = 0;

    public ScoreSetting(Activity activity)
    {
        this.activity = activity;
    }


    public void makingScore(String value, int key, int beat, LinearLayout lLayout) {
        String s[] = value.split("n");
        String pitch = s[0];
        String shape = s[1];
        int deltaY = 4;

        ImageView item_note = new ImageView(activity);
        ImageView item_shape = new ImageView(activity);

        LinearLayout.LayoutParams layoutParams_note
                = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams layoutParams_shape
                = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        if(beatCtr == 0)
        {
            initialX = 120;
        }
        else {
            initialX = 10;
        }

        if (pitch.equals("pause")) {
            switch (beat) {
                case 4:
                    beatCtr = beatCtr + 1.0;
                    item_note.setImageResource(R.drawable.note_4beat_pause);//設置圖片
                    break;
                case 8:
                    beatCtr = beatCtr + 0.5;
                    item_note.setImageResource(R.drawable.note_8beat_pause);
                    break;
                case 16:
                    beatCtr = beatCtr + 0.25;
                    item_note.setImageResource(R.drawable.note_16beat_pause);
                    break;
            }

            deltaY = initialY + (14 * deltaY);
            layoutParams_note.setMargins(initialX, deltaY, 0, 0);
            lLayout.addView(item_note, layoutParams_note);
        } else
        {
            switch (beat) {
                case 4:
                    beatCtr = beatCtr + 1.0;
                    item_note.setImageResource(R.drawable.note_4beat);
                    break;
                case 8:
                    beatCtr = beatCtr + 0.5;
                    item_note.setImageResource(R.drawable.note_8beat);
                    break;
                case 16:
                    beatCtr = beatCtr + 0.25;
                    item_note.setImageResource(R.drawable.note_16beat);
                    break;
            }

            deltaY = initialY + searchNote(pitch,key);
            if(deltaY != initialY)
            {
                if(shape.equals("\u266F")) {
                    int sharpY = deltaY + 32;
                    item_shape.setImageResource(R.drawable.sharp_sign);
                    layoutParams_shape.setMargins(initialX, sharpY, 0, 0);
                    lLayout.addView(item_shape,layoutParams_shape);

                    layoutParams_note.setMargins(0, deltaY, 0, 0);
                    lLayout.addView(item_note, layoutParams_note);

                }
                else {
                    layoutParams_note.setMargins(initialX, deltaY, 0, 0);

                    lLayout.addView(item_note, layoutParams_note);
                }
            }
        }
        /*if (beatCtr == 4) {
            initialY += 100;
            initialX = 120;
        }*/
    }

    private int searchNote(String pitch , int key)
    {
        int deltaY;
        String noteArray[] = {"tooDeepOrHigh","A5","G5","F5","E5","D5","C5","B4","A4","G4","F4","E4","D4","C4","B3","A3"};
        int noteYArray[] = {0,-9,-4,3,10,15,18,23,27,32,36,43,48,53,56,60};

        int noteIndex = 0;

        pitch = pitch+key;

        for( int i=0; i<noteArray.length; i++)
        {
            if(pitch.equals(noteArray[i]))
            {
                noteIndex = i;
                break;
            }
        }

        deltaY = noteYArray[noteIndex];

        Log.d("searching","pitch = "+pitch+"deltaY = "+(deltaY)+100);

        return deltaY;
    }

    public double getBeatCtr()
    {
        return beatCtr;
    }

    public void setBeatCtr( double beatCounter )
    {
        beatCtr = beatCounter;
    }

    public int getInitialY()
    {
        return  initialY;
    }

    public void setInitialX(int x)
    {
        initialX = x;
    }

    public void setInitialY(int y)
    {
        initialY = y;
    }

}

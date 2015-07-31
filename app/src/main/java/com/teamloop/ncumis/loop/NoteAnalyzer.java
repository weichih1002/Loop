package com.teamloop.ncumis.loop;

import android.util.Log;

import java.util.LinkedList;

public class NoteAnalyzer {

    public static final String TAG = "NoteAnalyzer";
    LinkedList noteList = new LinkedList();

    int initPitch = 0;
    int initKey = 4;
    int cf, pitch, key;
    double f;
    long L;


    private static final String notes[] =
            {"C", "C", "D", "D", "E", "F", "F", "G", "G", "A", "A", "B"};

    private static final String shapes[] =
            {"n\u0020", "n\u266F", "n\u0020", "n\u266F", "n\u0020", "n\u0020", "n\u266F", "n\u0020", "n\u266F", "n\u0020", "n\u266F", "n\u0020"};

    public LinkedList analyzeNote(  LinkedList frequencyList )
    {
        int nodeNum = 0;

        try {
            nodeNum = frequencyList.size();

        } catch (Exception e) {
            Log.e(TAG, "Exception when instantiating SoundAnalyzer: " + e.getMessage());
        }

        for (int num = 0; num < nodeNum; num++) {
            //  transform the frequency number to the pitch and the key
            initKey = 4;
            String messageNode = frequencyList.pop().toString();

            if (messageNode.equals("rest")) {
                noteList.add("rest"+"n\u0020"+",4");

                //Log.d(TAG, "rest" + ",4");
            } else {
                f = Double.parseDouble(messageNode);
                L = Math.round(12 * log2(f / 261.63));     // ***用四捨五入去調整range
                cf = (int) L;
                pitch = initPitch + cf % 12;    // update the pitch

                if (pitch < 0)
                {
                    pitch += 12;
                    initKey--;
                }
                key = initKey + cf / 12;        // update the key

                // 透過intent 傳遞資料 (String型態)
                noteList.add(notes[pitch] + shapes[pitch] + "," + key);
                //Log.d(TAG,notes[pitch]+shapes[pitch]+","+key);
            }
        }

        return noteList;       // 把處理完的list回傳
    }


    // method - 計算log2為底
    private double log2(double num) {
        return Math.log(num) / Math.log(2.0);
    }
}

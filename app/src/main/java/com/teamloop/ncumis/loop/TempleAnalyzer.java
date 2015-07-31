package com.teamloop.ncumis.loop;


import android.util.Log;

import java.util.LinkedList;

public class TempleAnalyzer {

    final static String TAG = "TempleAnalyzer";

    LinkedList noteList = new LinkedList();
    LinkedList tempList = new LinkedList();

    public LinkedList analyzeTemple(LinkedList dataList, int dataBpm)
    {

        while(dataList.size()!=0)     // 用來處理如果還沒開始唱歌時產生的空白訊號(休止符)
        {
            String beginCheck = dataList.getFirst().toString();  // beginCheck會是音名或休止符

            if( beginCheck.equals("rest"+"n\u0020"+",4") )    // 如果是休止符要丟棄
            {
                dataList.pop();
            }
            else {
                break;  // list 最前方的休止符刪除完畢
            }
        }


        int beatCounter = 1;
        int beat = 0;
        int i = 0;
        String soundValue;

        int listSize = dataList.size();

        // 開始判斷list中各個節點的拍數
        while( i < listSize )   // 從第一個點開始(index 0~n-1 )
        {
            soundValue = dataList.get(i).toString();    // soundValue是現在的音名或休止符

            while (( (i+beatCounter) < listSize ) && soundValue.equals(dataList.get(i+beatCounter).toString()))
            {
                    beatCounter++;      // 如果目標與現在手上持有的符號相同則counter+1
            }
            tempList.add(soundValue+","+beatCounter);
            i = i + beatCounter;
        }

        // 開始換算目前符號的節奏
        for(int j=0; j<tempList.size(); j++)
        {
            String s = tempList.get(j).toString();
            Log.d(TAG,""+s);
            String message[] = s.split(",");
            soundValue = message[0]+","+message[1];
            int beatNum = Integer.parseInt(message[2]);
            int manyBeat;       // 用來計算有幾個完整的拍子(4分音符)
            int redundantBeat;  // 用來計算有幾個不完整的拍子(8分或16分音符)

            // 拍子可以handle的bpm有兩種
            if (dataBpm >= 60 && dataBpm <= 100) {      // 較慢的歌曲
                manyBeat = beatNum / 4;         // 四個數據為一拍
                redundantBeat = beatNum % 4;
                if (manyBeat > 0) {     // 如果有至少一個完整個拍子
                    for (int m = 0; m < manyBeat; m++) {    //則看有幾個拍直接設為beat = 4
                        beat = 4;
                        noteList.add(soundValue + "," + beat);
                    }

                    if (redundantBeat != 0) {       // 其餘不完整的拍子要做分析
                        switch (redundantBeat) {
                            case 3:
                                beat = 8;
                                break;
                            case 2:
                                beat = 8;
                                break;
                            case 1:
                                beat = 16;
                                break;
                        }
                        noteList.add(soundValue + "," + beat);
                    }
                } else if (manyBeat == 0) {     // 如果都沒有四分音符則直接計算8分或16分
                    switch (redundantBeat) {
                        case 3:
                            beat = 8;
                            break;
                        case 2:
                            beat = 8;
                            break;
                        case 1:
                            beat = 16;
                            break;
                    }
                    noteList.add(soundValue + "," + beat);
                }
            }
            // 比較快的歌曲
            else if (dataBpm > 100 && dataBpm <= 140) {
                manyBeat = beatCounter / 2;         // 兩個數據為一拍
                redundantBeat = beatCounter % 2;
                if (manyBeat > 0) {     // 至少有完整的一拍則回傳beat = 4
                    for (int m = 0; m < manyBeat; m++) {
                        beat = 4;
                        noteList.add(soundValue + "," + beat);
                    }

                    if (redundantBeat != 0) {   // 其餘不完整的拍子回傳beat = 8 且暫時無法處理快歌的16beat
                        beat = 8;
                        noteList.add(soundValue + "," + beat);
                    }
                } else if (manyBeat == 0) {     // 沒有4分音符 直接回傳beat = 8
                    beat = 8;
                    noteList.add(soundValue + "," + beat);
                }
            }
        }

        return noteList;    // 回傳整個list
    }

}

package com.teamloop.ncumis.loop;


import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ScreenShot {

    private String fileName;

    public ScreenShot(String filename)
    {
        fileName = filename;
    }

    // 得到指定Activity的螢幕截圖，存到png
    public static Bitmap takeScreenShot(Activity activity){

        // View 是需要截圖的view
        View view = activity.getWindow().getDecorView();
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap bitmap = view.getDrawingCache();

        // 得到狀態列高度
        Rect frame = new Rect();

        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);

        int statusBarHeight = frame.top;

        // 得到螢幕長寬
        int width = activity.getWindowManager().getDefaultDisplay().getWidth();
        int height = activity.getWindowManager().getDefaultDisplay().getHeight();

        // 去掉狀態列
        Bitmap bitmap2 = Bitmap.createBitmap(bitmap, 0, statusBarHeight, width, height - statusBarHeight);

        view.destroyDrawingCache();

        return bitmap2;
    }

    private static void savePic(Bitmap bitmap, String strFileName)
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
        File file = new File(savingDir, strFileName);
        try {
            FileOutputStream fos = new FileOutputStream(file, false);
            if(fos!=null)
            {
                bitmap.compress(Bitmap.CompressFormat.PNG,90,fos);
                fos.flush();
                fos.close();
            }
        }
        catch (FileNotFoundException e)
        {
            Log.e("ScreenShot", "fileNotFoundException occurred");
        }
        catch (IOException ioe)
        {
            Log.e("ScreenShot", "IOException occurred");
        }
    }

    public void shootPic(Activity activity)
    {
        ScreenShot.savePic(ScreenShot.takeScreenShot(activity), fileName+".png");
    }
}

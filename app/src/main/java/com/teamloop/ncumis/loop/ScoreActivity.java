package com.teamloop.ncumis.loop;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import uk.co.dolphin_com.sscore.LicenceKeyInstance;
import uk.co.dolphin_com.sscore.LoadOptions;
import uk.co.dolphin_com.sscore.SScore;
import uk.co.dolphin_com.sscore.SSystem;
import uk.co.dolphin_com.sscore.Size;

import uk.co.dolphin_com.sscore.ex.ScoreException;
import uk.co.dolphin_com.sscore.ex.XMLValidationException;
import uk.co.dolphin_com.sscore.playdata.BarIterator;
import uk.co.dolphin_com.sscore.playdata.BarIterator.Part;
import uk.co.dolphin_com.sscore.playdata.Note;
import uk.co.dolphin_com.sscore.playdata.PlayData;
import uk.co.dolphin_com.sscore.playdata.UserTempo;

public class ScoreActivity extends Activity {

    public static final String TAG = "scoreActivity";

    /*
    * the parameters about seeScore sdk
    * */
    private static final boolean DisplayPlayData = false;   // set true to print out play data in the console for 2 bars on file load
    private static final boolean reloadAssetsFiles = false; // set true to clear files in internal directory and reload from assets
    private static final String CURRENT_FILE = "currentFile";   // the current file to preserve during a device rotation
    private static final String NEXT_FILE_INDEX = "nextFileIndex";  // the index of the next file to load to preserve during device rotation
    private static final String MAGNIFICATION = "magnification";    // the magnification to preserve
    private int nextFileIndex = 0;  // the index of the next file to load from the internal directory
    private File currentFile;   // the current file which is displayed
    private ScoreView scoreView;    // the View which displays the score

    /**
     * the current viewed score.
     * Preserved to avoid reload on rotate (which causes complete destruction and recreation of this Activity)
     */
    private SScore currentScore;

    /**
     * the current magnification.
     * Preserved to avoid reload on rotate (which causes complete destruction and recreation of this Activity)
     */
    private float magnification;
    private boolean isTransposing;  // set to prevent reentry during transpose

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "ScoreActivity onCreate()");
        super.onCreate(savedInstanceState);

        magnification = 0.5F;
        if (reloadAssetsFiles)
            clearInternalDir();
        scoreView = new ScoreView(this, getAssets(), new ScoreView.ZoomNotification(){
            @Override
            public void zoom(float scale) {
            }
        });

        setContentView(R.layout.score_layout);
        ScrollView sv = (ScrollView) findViewById(R.id.scrollView1);
        sv.addView(scoreView);
        sv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent event) {
                return scoreView.onTouchEvent(event);
            }
        });
    }

    /**
     * update the UI to show the score
     *
     * @param score the score
     */
    private void showScore(SScore score)
    {
        scoreView.setLayoutCompletionHandler(new Runnable() {
            public void run() {
                List<SSystem.BarRange> barRanges = scoreView.getAllBarRanges();
                for (SSystem.BarRange br : barRanges) {
                    System.out.println(" startb:" + br.startBarIndex + " numb:" + br.numBars);
                }
                Size bounds = scoreView.getBounds();
                System.out.println("bounds:(" + bounds.width + "," + bounds.height + ")");
            }
        });
        //showTranspose(score);
        scoreView.setScore(score, magnification); // relayout after transpose
    }


    /**
     * load a .mxl file and return a {@link SScore}
     * We use a ZipInputStream to decompress the .mxl data into a UTF-8 XML byte buffer
     *
     * @param file a file which can be opened with FileInputStream
     * @return a {@link SScore}
     */
    private SScore loadMXLFile(File file)
    {
        if (!file.getName().endsWith(".mxl"))
            return null;

        InputStream is;
        try {
            is = new FileInputStream(file);
            ZipInputStream zis = null;
            try
            {
                zis = new ZipInputStream(new BufferedInputStream(is));
                ZipEntry ze;
                while ((ze = zis.getNextEntry()) != null) {
                    if (!ze.getName().startsWith("META-INF") // ignore META-INF/ and container.xml
                            && !ze.getName().equals("container.xml"))
                    {
                        // read from Zip into buffer and copy into ByteArrayOutputStream which is converted to byte array of whole file
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int count;
                        while ((count = zis.read(buffer)) != -1) { // load in 1K chunks
                            os.write(buffer, 0, count);
                        }
                        try
                        {
                            LoadOptions loadOptions = new LoadOptions(LicenceKeyInstance.SeeScoreLibKey, true);
                            return SScore.loadXMLData(os.toByteArray(), loadOptions);
                        }
                        catch (XMLValidationException e)
                        {
                            Log.w("sscore", "loadfile <" + file + "> xml validation error: " + e.getMessage());
                        }
                        catch (ScoreException e)
                        {
                            Log.w("sscore", "loadfile <" + file + "> error:" + e);
                        }
                    }
                }
            } catch (IOException e) {
                Log.w("Open", "file open error " + file, e);
                e.printStackTrace();
            }
            finally {
                if (zis != null)
                    zis.close();
            }
        } catch (FileNotFoundException e1) {
            Log.w("Open", "file not found error " + file, e1);
            e1.printStackTrace();
        } catch (IOException e) {
            Log.w("Open", "io exception " + file, e);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Load the given xml file and return a SScore.
     *
     * @param file the file
     * @return the score
     */
    private SScore loadXMLFile(File file)
    {
        if (!file.getName().endsWith(".xml"))
            return null;
        try
        {
            LoadOptions loadOptions = new LoadOptions(LicenceKeyInstance.SeeScoreLibKey, true);
            return SScore.loadXMLFile(file, loadOptions);
        }
        catch (XMLValidationException e) {
            Log.w("sscore", "loadfile <" + file + "> xml validation error: " + e.getMessage());
        } catch (ScoreException e) {
            Log.w("sscore", "loadfile <" + file + "> error:" + e);
        }
        return null;
    }

    /**
     * Get all the .xml or .mxl filenames in the assets folder
     *
     * @return the List of filenames
     */
    private List<String> getXMLAssetsFilenames()
    {
        ArrayList<String> rval = new ArrayList<String>();
        // copy files from assets to internal directory where they can be opened as files (assets can only be opened as InputStreams)
        AssetManager am = getAssets ();
        try {
            String[] files = am.list("");
            for (String filename : files)
                if (filename.endsWith(".mxl") || filename.endsWith(".xml"))
                    rval.add(filename);
        } catch (IOException e) {
            Log.e(TAG," Exception when get XML assets.");
        }
        return rval;
    }

    /**
     * Get all the .xml/.mxl files in the internal dir
     *
     * @return the List of files
     */
    private List<File> getXMLFiles()
    {
        File internalDir = getFilesDir();
        String[] files = internalDir.list(new FilenameFilter(){

            @Override
            public boolean accept(File arg0, String filename) {
                return filename.endsWith(".xml") || filename.endsWith(".mxl");
            }});
        ArrayList<File> rval = new ArrayList<File>();
        for (String fname : files)
        {
            rval.add(new File(internalDir, fname));
        }
        return rval;
    }

    /**
     * copy all .xml/.mxl files from assets to the internal directory where they can be opened as files
     *  (assets can only be opened as InputStreams).
     *
     * @return the List of {@link File}.
     */
    private List<File> moveFilesToInternalStorage()
    {
        ArrayList<File> rval = new ArrayList<File>();
        AssetManager am = getAssets();
        try {
            String[] files = am.list("");
            File internalDir = getFilesDir();
            for (String filename : files)
            {
                if (filename.endsWith(".xml") || filename.endsWith(".mxl") )
                {
                    File outfile = new File(internalDir, filename);
                    InputStream is = am.open (filename);
                    OutputStream os = new FileOutputStream(outfile);
                    byte[] buffer = new byte[1024];
                    int read;
                    while((read = is.read(buffer)) != -1){
                        os.write(buffer, 0, read);
                    }
                    is.close();
                    os.close();
                    rval.add(outfile);
                }
            }
        } catch (IOException e) {
            Log.w("FileStorage", "Error copying asset files ", e);
        }
        return rval;
    }

    /**
     * get the list of .xml & .mxl files in the internal directory.
     *
     * @return the List of {@link File}.
     */
    private List<File> sourceXMLFiles()
    {
        List<File> files = getXMLFiles();
        List<String> assetsFiles = getXMLAssetsFilenames();
        if (files.size() >= assetsFiles.size())
        {
            return files;
        }
        else
            return moveFilesToInternalStorage();
    }


    /**
     * Load the next .xml/.mxl file from the assets (copied via the internal dir)
     *
     * @return the score
     */
    private SScore loadNextFile()
    {
        List<File> files = sourceXMLFiles();
        int index = 0;
        for(File file : files)
        {
            if(index == nextFileIndex)
            {
                SScore sc = loadFile(file);
                nextFileIndex = (index + 1) % files.size();

                if(sc != null)
                {
                    currentFile = file;
                    currentScore = sc;
                    if(DisplayPlayData) // setup and display play data
                    {
                        try{
                            PlayData pd = new PlayData(sc, new UserTempoImpl());
                            for (BarIterator.Bar bar : pd)
                            {
                                System.out.println("bar:" + bar.toString());
                                if (bar.index() > 2)
                                    break; // otherwise this is a lot of console noise
                                int partIndex = 0; // just 1st part
                                BarIterator.Part part = bar.part(partIndex);
                                for (Note note : part) {
                                    System.out.println(" note:" + note.toString());
                                }
                            }
                        }
                        catch(ScoreException e)
                        {
                            Log.e(TAG, "Playdata Exception");
                        }
                    }
                }
            }
            ++index;
        }
        return currentScore;
    }


    /**
     * Load the file of type .xml or .mxl
     *
     * @param file load the .mxl or .xml file
     * @return the score
     */
    private SScore loadFile(File file)
    {
        isTransposing = false;
        if (file.getName().endsWith(".mxl"))
        {
            return loadMXLFile(file);
        }
        else if (file.getName().endsWith(".xml"))
        {
            return loadXMLFile(file);
        }
        else
            return null;
    }

    /**
     * delete all xml/mxl files in internal directory so they are reloaded from assets
     */
    private void clearInternalDir()
    {
        File internalDir = getFilesDir();
        File[] files = internalDir.listFiles(new FilenameFilter(){

            @Override
            public boolean accept(File file, String filename) {
                return filename.endsWith(".xml") || filename.endsWith(".mxl");
            }

        });
        for (File file : files)
        {
            file.delete();
        }
    }


    /**
     * load the next file in the directory (transferred at startup from assets) in a background thread
     */
    void backgroundLoadNext()
    {
        magnification = scoreView.getMagnification(); // preserve the magnification
        new Thread(new Runnable(){ // load file on background thread

            public void run() {

                final SScore score = loadNextFile();

                new Handler(Looper.getMainLooper()).post(new Runnable(){

                    public void run() {
                        if (score != null)
                        {
                            showScore(score); // update score in SeeScoreView on foreground thread
                        }
                    }
                });
            }
        }).start();
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

    /**
     * an implementation of the UserTempo interface used by the {@link PlayData}
     * to get a current user-defined tempo, or scaling for the score embedded tempo values
     */
    private class UserTempoImpl implements UserTempo
    {
        /**
         * @return the (should be user-defined) tempo BPM
         */
        public int getUserTempo() {
            return 60;
        }

        /**
         * @return the (should be user-defined) tempo scaling for score embedded tempo values
         */
        public float getUserTempoScaling() {
            return 1;
        }
    }


    // 處理如何關閉程式
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {

        if ((keyCode == KeyEvent.KEYCODE_BACK)) {   //確定按下退出鍵

            Intent intent = new Intent();         // 產生scoreActivity等下要轉換
            intent.setClass(ScoreActivity.this, RecordActivity.class);

            clearInternalDir();             // 把暫存musicMXL檔清除
            startActivity(intent);          // 切換intent
            ScoreActivity.this.finish();

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }


    /**
     * called on start the activity
     */
    @Override
    protected void onStart()
    {
        super.onStart();
        if (currentScore != null) // we can use the saved score if only rotating the display - we don't want the whole reload
        {
            showScore(currentScore);
        }
        else
        {
            new Thread(new Runnable(){ // load file on background thread

                public void run() {
                    final SScore score = (currentFile != null) ? loadFile(currentFile) : loadNextFile();

                    new Handler(Looper.getMainLooper()).post(new Runnable(){

                        public void run() {

                            if (score != null)
                            {
                                showScore(score); // update score in SeeScoreView on foreground thread
                            }
                        }
                    });
                }

            }).start();
        }
    }

    /**
     * load the SeeScoreLib.so library
     */
    static {
        System.loadLibrary("stlport_shared");
        System.loadLibrary("SeeScoreLib");
    }
}
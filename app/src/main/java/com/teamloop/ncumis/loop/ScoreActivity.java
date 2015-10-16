/**
 * SeeScore For Android Sample App
 * Dolphin Computing http://www.dolphin-com.co.uk
 */
package com.teamloop.ncumis.loop;

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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import uk.co.dolphin_com.sscore.Component;
import uk.co.dolphin_com.sscore.Header;
import uk.co.dolphin_com.sscore.LicenceKeyInstance;
import uk.co.dolphin_com.sscore.LoadOptions;
import uk.co.dolphin_com.sscore.SScore;
import uk.co.dolphin_com.sscore.Tempo;
import uk.co.dolphin_com.sscore.Version;
import uk.co.dolphin_com.sscore.ex.ScoreException;
import uk.co.dolphin_com.sscore.ex.XMLValidationException;
import uk.co.dolphin_com.sscore.playdata.Note;
import uk.co.dolphin_com.sscore.playdata.PlayData;
import uk.co.dolphin_com.sscore.playdata.UserTempo;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class ScoreActivity extends Activity {

    private static final boolean PlayUsingMediaPlayer = true;

    private static final boolean UseNoteCursorIfPossible = true; // else bar cursor

    private static final int kMinTempoBPM = 30;
    private static final int kMaxTempoBPM = 240;
    private static final int kDefaultTempoBPM = 80;
    private static final double kMinTempoScaling = 0.5;
    private static final double kMaxTempoScaling = 2.0;
    private static final double kDefaultTempoScaling = 1.0;

    /**
     * set true to clear files in internal directory and reload from assets
     */
    private static final boolean reloadAssetsFiles = false;

    /**
     * the current file to preserve during a device rotation
     */
    private static final String CURRENT_FILE = "currentFile";

    /**
     * the index of the next file to load to preserve during device rotation
     */
    private static final String NEXT_FILE_INDEX = "nextFileIndex";

    /**
     * the magnification to preserve
     */
    private static final String MAGNIFICATION = "magnification";

    /**
     * the index of the next file to load from the internal directory
     */
    private int nextFileIndex = 0;

    /**
     * the current file which is displayed
     */
    private File currentFile;

    /**
     * the View which displays the score
     */
    private ScoreView scoreView;

    /**
     * the current viewed score.
     * <p>Preserved to avoid reload on rotate (which causes complete destruction and recreation of this Activity)
     */
    private SScore currentScore;

    /**
     * the current magnification.
     * <p>Preserved to avoid reload on rotate (which causes complete destruction and recreation of this Activity)
     */
    private float magnification;

    /**
     * set to prevent reentry during transpose
     */
    private boolean isTransposing;

    /**
     * the player plays the music using MediaPlayer and supports handlers for synchronised events on bar start, beat and note start
     */
    private Player player;

    /**
     * the current bar preserved on player stop so it can be restarted in the same place
     */
    private int currentBar;

    /**
     * called on creating this Activity AND ALSO on device rotation (ie portrait/landscape switch)
     */
    private ImageButton backBtn;
    private ImageButton downloadBtn;

    private String fileName = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.score_layout);

        backBtn = (ImageButton)findViewById(R.id.backBtn);
        backBtn.setImageResource(R.drawable.back);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmGoBack();
            }
        });

        Bundle bundle = this.getIntent().getExtras();
        fileName = bundle.getString("fileName");

        downloadBtn = (ImageButton)findViewById(R.id.downloadBtn);
        downloadBtn.setImageResource(R.drawable.download);
        downloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDownloadScreenShot();
            }
        });

        currentScore = null;
        currentBar = 0;
        magnification = 0.5F;
        isTransposing = false;
        if (reloadAssetsFiles)
            clearInternalDir();
        scoreView = new ScoreView(this, getAssets(), new ScoreView.ZoomNotification(){
            public void zoom(float scale) {
            }
        }, new ScoreView.TapNotification(){
            public void tap(int systemIndex, int partIndex, int barIndex, Component[] components)
            {
                currentBar = barIndex;
                if (player != null) {
                    boolean isPlaying = (player.state() == Player.State.Started);
                    if (isPlaying)
                        player.pause();
                    scoreView.setCursorAtBar(barIndex, ScoreView.CursorType.line, 200);
                    if (isPlaying) {
                        player.startAt(barIndex, false/*no countIn*/);
                    }
                }
                else
                    scoreView.setCursorAtBar(barIndex, ScoreView.CursorType.box, 200);
                System.out.println("tap system:" + systemIndex + " bar:" + barIndex);
                for (Component comp : components)
                    System.out.println(comp);
            }
        });
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);

        ScrollView sv = (ScrollView) findViewById(R.id.scrollView1);
        sv.addView(scoreView);
        sv.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View arg0, MotionEvent event) {
                return scoreView.onTouchEvent(event);
            }

        });
        if (savedInstanceState != null) // restore state on device rotation avoiding file reload
        {
            String filePath = savedInstanceState.getString(CURRENT_FILE);
            if (filePath != null && filePath.length() > 0)
                currentFile = new File(filePath);
            nextFileIndex = savedInstanceState.getInt(NEXT_FILE_INDEX);
            magnification = savedInstanceState.getFloat(MAGNIFICATION);

            Object o = getLastNonConfigurationInstance();
            if (o instanceof SScore)
            {
                currentScore = (SScore)o; // onResume updates the ui with this score
            }
        }
    }

    /**
     * called on app quit and device rotation.
     * <p>We save the state and the score so we can restore without reloading the file on device rotation
     */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if (player != null) {
            player.reset();
        }
        if (currentFile != null)
            savedInstanceState.putString(CURRENT_FILE, currentFile.getAbsolutePath());
        savedInstanceState.putInt(NEXT_FILE_INDEX, nextFileIndex);
        savedInstanceState.putFloat(MAGNIFICATION, scoreView.getMagnification());
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * restore the score after device rotation
     */
    public Object onRetainNonConfigurationInstance ()
    {
        return currentScore;
    }

    private enum PlayPause { play, pause};

    /**
     * set the correct image in the Play/Pause button
     * @param playPause play or pause image to use
     */
    private void setPlayButtonImage(PlayPause playPause) {
        ImageButton playButton = (ImageButton) findViewById(R.id.play_btn);
/*        if (playPause == PlayPause.pause)
            playButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_media_pause));
        else
            playButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_media_play));*/
    }

    /** update the play-pause button image according to the player state */
    private void updatePlayPauseButtonImage() {
        if (player != null && player.state() == Player.State.Started)
            setPlayButtonImage(PlayPause.pause);
        else
            setPlayButtonImage(PlayPause.play);
    }

    /** Get all the .xml or .mxl filenames in the assets folder */
    private List<String> getXMLAssetsFilenames()
    {
        ArrayList<String> rval = new ArrayList<String>();
        // copy files from assets to internal directory where they can be opened as files (assets can only be opened as InputStreams)
        AssetManager am = getAssets();
        try {
            String[] files = am.list("");
            for (String filename : files)
                if (filename.endsWith(".mxl") || filename.endsWith(".xml"))
                    rval.add(filename);
        } catch (IOException e) {
        }
        return rval;
    }

    /** Get all the .xml/.mxl files in the internal dir */
    private List<File> getXMLFiles()
    {
        File internalDir = getFilesDir();
        String[] files = internalDir.list(new FilenameFilter() {

            @Override
            public boolean accept(File arg0, String filename) {
                return filename.endsWith(".xml") || filename.endsWith(".mxl");
            }
        });
        ArrayList<File> rval = new ArrayList<File>();
        for (String fname : files)
        {
            rval.add(new File(internalDir, fname));
        }
        return rval;
    }

    /** delete all xml/mxl files in internal directory so they are reloaded from assets */
    private void clearInternalDir()
    {
        File internalDir = getFilesDir();
        File[] files = internalDir.listFiles(new FilenameFilter() {

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

    /** copy all .xml/.mxl files from assets to the internal directory where they can be opened as files
     *  (assets can only be opened as InputStreams).  */
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
     * get a suitable String to use as a title for the score
     *
     * @param score the {@link SScore}
     * @return the title {@link String}
     */
    private String titleText(SScore score)
    {
        Header header = score.getHeader();
        return currentFile.getName() + " - " + header.work_title + " " + header.composer;
    }

    private int scalingToBPM(double scaling, int nominalBPM) {
        return  (int)(nominalBPM * scaling);
    }

    private int scalingToSliderPercent(double scaling) {
        return (int)(0.5+(100 * ((scaling - kMinTempoScaling) / (kMaxTempoScaling - kMinTempoScaling))));
    }

    private double sliderPercentToScaling(int percent) {
        return kMinTempoScaling + (percent/100.0) * (kMaxTempoScaling - kMinTempoScaling);
    }

    private int sliderPercentToBPM(int percent) {
        return kMinTempoBPM + (int)((percent/100.0) * (kMaxTempoBPM - kMinTempoBPM));
    }

    private int bpmToSliderPercent(int bpm) {
        return (int)(100.0 * (bpm - kMinTempoBPM) / (double)(kMaxTempoBPM - kMinTempoBPM));
    }

    /**
     * an implementation of the UserTempo interface used by the {@link PlayData}
     * to get a current user-defined tempo, or scaling for the score embedded tempo values
     * These read the position of the tempo slider and convert that to a suitable tempo value
     */
    private class UserTempoImpl implements UserTempo
    {
        /**
         * @return the user-defined tempo BPM (if not defined by the score)
         */
        public int getUserTempo() {
            return 80;
        }

        /**
         * @return the user-defined tempo scaling for score embedded tempo values (ie 1.0 => use standard tempo)
         */
        public float getUserTempoScaling() {
            return 1;
        }
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
                            && ze.getName() != "container.xml")
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
     * Load the file of type .xml or .mxl
     *
     * @param file
     * @return the score
     */
    private SScore loadFile(File file)
    {
        if (player != null) {
            player.reset();
            player = null; // force a reload with the new score
            currentBar = 0;
        }
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
     * Load the next .xml/.mxl file from the assets (copied via the internal dir)
     *
     * @return the score
     */
    private SScore loadNextFile()
    {
        List<File> files = sourceXMLFiles();
        int index = 0;
        for (File file : files)
        {
            if (index == nextFileIndex)
            {
                SScore sc = loadFile(file);
                nextFileIndex = (index + 1) % files.size();
                if (sc != null)
                {
                    currentFile = file;
                    currentScore = sc;
                    return sc;
                }
            }
            ++index;
        }
        return null;
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
                // we could do something here when the score has finished loading
            }
        });
        setPlayButtonImage(PlayPause.play); // show play in menu
        scoreView.setScore(score, magnification); // relayout after transpose
        // set tempo slider to default tempo
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
    /**
     * called on resuming the activity, including after device rotation
     */
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

    /** called by the system on opening the menu */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }


    /**
     * transpose in a background thread
     *
     * @param transpose +1/-1 to transpose up/down one semitone from the current transpose setting
     */
    private void backgroundTranspose(final int transpose)
    {
        if (!isTransposing)
        {
            isTransposing = true;
            new Thread(new Runnable(){ // load file on background thread
                public void run()
                {
                    if (player != null) {
                        player.reset();
                        player = null;
                    }
                    currentBar = 0;
                    if (currentScore != null)
                    {
                        try
                        {
                            currentScore.setTranspose(currentScore.getTranspose() + transpose);
                            new Handler(Looper.getMainLooper()).post(new Runnable(){
                                public void run()
                                {
                                    showScore(currentScore); // relayout after transpose
                                    isTransposing = false;
                                }
                            });
                        } catch(ScoreException e) {
                            System.out.println(" exception from setTranspose:" + e.toString());
                            isTransposing = false;
                        }
                    }
                    else
                    {
                        isTransposing = false;
                    }

                }
            }).start();
        }
    }

    /**
     * create and setup the Player with dispatch handlers
     * @return the new player
     */
    private Player setupPlayer()
    {
        try {
            final Player pl = new Player(currentScore, new UserTempoImpl(), ScoreActivity.this, PlayUsingMediaPlayer);
            final int autoScrollAnimationTime = pl.bestScrollAnimationTime();
            pl.setBarStartHandler(new Dispatcher.EventHandler() {
                public void event(final int index, final boolean ci) {
                    // use bar cursor if bar time is short
                    final boolean useNoteCursor = UseNoteCursorIfPossible && !pl.needsFastCursor();
                    if (!useNoteCursor) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {

                            public void run() {
                                scoreView.setCursorAtBar(index, ScoreView.CursorType.box, autoScrollAnimationTime);
                            }
                        });
                    }
                }
            }, -50); // anticipate so cursor arrives on time

            if (UseNoteCursorIfPossible) {
                pl.setNoteHandler(new Dispatcher.NoteEventHandler() {
                    public void startNotes(final List<Note> notes) {
                        // disable note cursor if bar time is short
                        final boolean useNoteCursor = !pl.needsFastCursor();
                        if (useNoteCursor) {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                final List<Note> localNotes = notes;

                                public void run() {

                                    scoreView.moveNoteCursor(localNotes, autoScrollAnimationTime);
                                }
                            });
                        }
                    }
                }, -50);
            }
            pl.setEndHandler(new Dispatcher.EventHandler() {
                @Override
                public void event(int index, boolean countIn) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        public void run() {
                            setPlayButtonImage(PlayPause.play);
                            currentBar = 0; // next play will be from start
                        }
                    });
                }
            }, 0);

            return pl;

        } catch (Player.PlayerException ex) {
            System.out.println("Player error: " + ex.getMessage());
        }
        return null;
    }

    /**
     * called on tapping play-pause button
     * @param button the button
     */
    public void play_pause(View button) {
        if (currentScore == null)
            return;

        if (player == null)
            player = setupPlayer();

        if (player != null) {
            switch (player.state()) {
                case NotStarted:
                    break;

                case Started:
                    player.pause();
                    currentBar = player.currentBar();
                    return;

                case Paused:
                    if (currentBar == player.currentBar())
                        player.resume();
                    else
                        player.reset(); // restart at a different bar
                    break;

                case Stopped:
                case Completed:
                    player.reset();
                    currentBar = 0;
                    break;
            }

            if (player.state() == Player.State.NotStarted) {

                // scroll to current bar ready for start
                scoreView.setCursorAtBar(currentBar, ScoreView.CursorType.line, 0);
                player.startAt(currentBar, true/*countIn*/);
            }
            updatePlayPauseButtonImage();
        }
    }

    /**
     * called on tapping the stop button
     * @param button
     */
    public void stop_play(View button)
    {
        if (player != null) {
            switch (player.state()) {
                case Started:
                case Paused:
                case Stopped:
                case Completed:
                    player.reset();
                    currentBar = 0;
                    break;
            }
        }
        updatePlayPauseButtonImage();
    }

    /**
     * called from the system to handle menu selection
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.nextfile:
                backgroundLoadNext();
                return true;

            case R.id.plus:
                backgroundTranspose(+1);
                return true;

            case R.id.minus:
                backgroundTranspose(-1);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    // method to confirm download the score
    private void confirmDownloadScreenShot()
    {
        AlertDialog.Builder dialog = new AlertDialog.Builder(ScoreActivity.this);
        dialog.setTitle("下載");
        dialog.setMessage("是否儲存樂譜?");
        dialog.setPositiveButton("是", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                ScreenShot screenShot = new ScreenShot(fileName);
                screenShot.shootPic(ScoreActivity.this);

                Toast.makeText(ScoreActivity.this, "已儲存於檔案資料夾Loop中", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.setNegativeButton("否", new DialogInterface.OnClickListener() { //按"否",則不執行任何操作

            public void onClick(DialogInterface dialog, int i) {

            }

        });

        dialog.show();//顯示訊息視窗

    }


    // method to confirm go back to record activity
    private void confirmGoBack(){

        AlertDialog.Builder dialog = new AlertDialog.Builder(ScoreActivity.this);
        dialog.setMessage("返回主畫面?");
        dialog.setPositiveButton("是", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();         // 產生RecordActivity等下要轉換
                intent.setClass(ScoreActivity.this, RecordActivity.class);

                clearInternalDir();             // 把暫存musicMXL檔清除
                startActivity(intent);          // 切換intent
                ScoreActivity.this.finish();
            }
        });

        dialog.setNegativeButton("否", new DialogInterface.OnClickListener() { //按"否",則不執行任何操作

            public void onClick(DialogInterface dialog, int i) {

            }

        });

        dialog.show();//顯示訊息視窗
    }

    // 處理如何關閉程式
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if ((keyCode == KeyEvent.KEYCODE_BACK)) {   //確定按下退出鍵
            confirmGoBack();
            return true;
        }

        return false;
    }



    /**
     * load the SeeScoreLib.so library
     */
    static {
        System.loadLibrary("stlport_shared");
        System.loadLibrary("SeeScoreLib");
    }
}

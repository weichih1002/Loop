package com.teamloop.ncumis.loop;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.view.MotionEventCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import uk.co.dolphin_com.sscore.LayoutCallback;
import uk.co.dolphin_com.sscore.LayoutOptions;
import uk.co.dolphin_com.sscore.SScore;
import uk.co.dolphin_com.sscore.SSystem;
import uk.co.dolphin_com.sscore.SSystemList;
import uk.co.dolphin_com.sscore.Size;
import uk.co.dolphin_com.sscore.ex.NoPartsException;
import uk.co.dolphin_com.sscore.ex.ScoreException;


public class ScoreView extends LinearLayout {

    public interface ZoomNotification
    {
        void zoom(float scale);
    }
    /**
     * the minimum magnification
     */
    static final float kMinMag = 0.2F;

    /**
     * the maximum magnification
     */
    static final float kMaxMag = 3.F;

    /**
     * the margin between the edge of the screen and the edge of the layout
     */
    static final float kMargin = 10;

    private SScore score;
    private AssetManager assetManager;
    private int displayDPI;
    private SSystemList systems;
    private float magnification;
    private LayoutThread layoutThread;
    private float screenHeight;
    private boolean isAbortingLayout = false;
    private float startPinchFingerSpacing;
    private boolean isZooming = false;
    private ArrayList<SystemView> views = new ArrayList<SystemView>();
    private ZoomNotification zoomNotify;
    private Runnable layoutCompletionHandler;

    public ScoreView(Activity context, AssetManager am, ZoomNotification zn) {
        super(context);
        setOrientation(VERTICAL);
        this.assetManager = am;
        this.magnification = 1.0F;
        this.zoomNotify = zn;
        DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
        Display display = context.getWindowManager().getDefaultDisplay();
        display.getMetrics(displayMetrics);
        displayDPI = displayMetrics.densityDpi;
        android.graphics.Point screenSize = new android.graphics.Point();
        display.getSize(screenSize);
        screenHeight = screenSize.y;
    }

    public void setLayoutCompletionHandler(Runnable handler)
    {
        layoutCompletionHandler = handler;
    }

    public float getMagnification()
    {
        return magnification;
    }

    public Size getBounds()
    {
        return systems.getBounds(0);
    }

    private void addSystem(final SSystem sys)
    {
        systems.addSystem(sys);
        new Handler(Looper.getMainLooper()).post(new Runnable(){

            public void run() {
                SystemView sv = new SystemView(getContext(), score, sys, ScoreView.this.assetManager);
                addView(sv);
                views.add(sv);
            }
        });
    }

    public List<SSystem.BarRange> getAllBarRanges()
    {
        ArrayList<SSystem.BarRange> rval = new ArrayList<SSystem.BarRange>();
        for (int sysindex = 0; sysindex < systems.getSize(); ++sysindex)
            rval.add(systems.getSystemAt(sysindex).getBarRange());
        return rval;
    }

    protected void onSizeChanged (int w, int h, int oldw, int oldh)
    {
        if (w > 0 && score != null)
        {
            layout();
        }
    }

    /**
     * Set the loaded SScore to be displayed by this.
     * <p>
     * This will initiate an asynchronous layout and the View will be updated as
     * each System completes layout
     *
     * @param score the SScore to be displayed
     */
    public void setScore(final SScore score, final float magnification)
    {
        abortLayout(new Runnable(){
            public void run()
            {
                ScoreView.this.score = score;
                ScoreView.this.magnification = magnification;
                if (score != null && getWidth() > 0)
                    layout();
                    // else layout will be called in OnSizeChanged()
                else
                {
                    systems = null;
                    removeAllViews();
                }
            }
        });
    }

    /**
     * A Thread to perform a complete (abortable) layout of the entire score which
     * may take unlimited time, but periodically updates the UI whenever a new laid-out system
     * is ready to add to the display
     */
    private class LayoutThread extends Thread
    {
        LayoutThread(float displayHeight)
        {
            super("LayoutThread");
            this.displayHeight = displayHeight;
            aborting = false;
            views.clear();
        }

        /**
         *  set the abort flag so that the layout thread will stop ASAP
         */
        public void abort()
        {
            aborting = true;
        }

        public void run()
        {
            if (aborting)
                return;
            Canvas canvas = new Canvas();
            int numParts = score.numParts();
            boolean[] parts = new boolean[numParts];
            for (int i = 0; i < numParts; ++i)
            {
                parts[i] = true;
            }
            LayoutOptions opt = new LayoutOptions();
            if (displayHeight > 100
                    && !aborting)
            {
                try
                {
                    score.layout(canvas, assetManager, displayDPI, getWidth() - 2*kMargin, displayHeight, parts,
                            new LayoutCallback(){
                                public boolean addSystem(SSystem sys)
                                {
                                    if (!aborting)
                                        ScoreView.this.addSystem(sys);
                                    return !aborting; // return false to abort layout
                                }
                            },
                            magnification,opt);
                }
                catch (NoPartsException e)
                {
                    Log.w("sscore", "layout no parts error");
                }
                catch (ScoreException e)
                {
                    Log.w("sscore", "layout error:" + e);
                }
            }
            new Handler(Looper.getMainLooper()).post(new Runnable(){

                public void run() {
                    ScoreView.this.layoutCompletionHandler.run();
                }
            });
        }

        private boolean aborting;
        private float displayHeight;
    }

    /**
     *  abort the layout and notify completion on the main thread through the Runnable argument
     *
     * @param thenRunnable run() is executed when abort is complete on the main thread
     */
    public void abortLayout(final Runnable thenRunnable)
    {
        if (isAbortingLayout)
            return; // already aborting - thenRunnable is DISCARDED!
        if (layoutThread != null)
        {
            isAbortingLayout = true;
            layoutThread.abort();
            new Thread(new Runnable() { // start a thread to await completion of the abort
                public void run()
                {
                    {
                        try {
                            layoutThread.join(); // await completion of abort
                        } catch (InterruptedException e) {
                            // don't care if interrupted during join
                        }
                        layoutThread = null;
                        isAbortingLayout = false;
                        new Handler(Looper.getMainLooper()).post(new Runnable(){

                            public void run() {
                                thenRunnable.run();
                            }
                        });
                    }
                }
            }, "AbortThread").start();
        }
        else
            thenRunnable.run();
    }

    private void layout()
    {
        if (!isAbortingLayout && layoutThread == null)
        {
            systems = new SSystemList();
            removeAllViews();
            layoutThread = new LayoutThread(screenHeight);
            layoutThread.start();
        }
    }

    /**
     * called during active pinching. Do quick rescale of display without relayout
     * Relayout happens on completion of gesture
     *
     * @param zoom the current magnification based on the finger spacing compared
     * to the spacing at the start of the pinch
     */
    private void zooming(final float zoom)
    {
        float mag;
        if (magnification < kMinMag)
            magnification = kMinMag; // ensure no div-by-zero
        if (zoom * magnification < kMinMag)
            mag = kMinMag / magnification;
        else if (zoom * magnification > kMaxMag)
            mag = kMaxMag / magnification;
        else if (Math.abs(zoom * magnification - 1.0) < 0.05) // make easy to set 1.0
            mag = 1.0F / magnification;
        else
            mag = zoom;

        for (View view : views)
        {
            if (view instanceof SystemView
                    && view.isShown())
            {
                Rect r = new Rect();
                boolean vis = view.getGlobalVisibleRect (r);
                if (vis)
                {
                    SystemView sv = (SystemView)view;
                    sv.zooming(mag);
                }
            }
        }
        zoomNotify.zoom(zoom * magnification);
    }

    /**
     * abort any current layout and make a new layout at the new magnification
     *
     * @param zoom the new magnification to use for the layout
     */
    public void zoom(final float zoom)
    {
        abortLayout(new Runnable(){
            public void run()
            {
                magnification = Math.min(Math.max(zoom, kMinMag), kMaxMag);
                if (Math.abs(magnification - 1.0) < 0.05)
                    magnification = 1.0F; // gravitate towards 1.0
                layout();
                zoomNotify.zoom(magnification);
            }
        });
    }

    /**
     *  spacing between fingers during a pinch gesture
     *
     * @param event the MotionEvent from onTouchEvent
     * @return the finger spacing
     */
    private float fingerSpacing(MotionEvent event)
    {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float)java.lang.Math.sqrt(x * x + y * y);
    }

    /**
     * called from the system for a touch notification
     */
    public boolean onTouchEvent (MotionEvent event)
    {
        int action = MotionEventCompat.getActionMasked(event);
        switch(action)
        {
            case MotionEvent.ACTION_POINTER_DOWN:
                startPinchFingerSpacing = fingerSpacing(event);
                if (startPinchFingerSpacing > 10f) {
                    isZooming = true;
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isZooming && startPinchFingerSpacing > 10 ) {
                    float mag = fingerSpacing(event) / startPinchFingerSpacing;
                    zooming(mag);
                    return true;
                }
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
                if (isZooming)
                {
                    float mag = magnification * fingerSpacing(event) / startPinchFingerSpacing;
                    zoom(mag);
                    isZooming = false;
                    return true;
                }
                break;

        }
        return false;
    }
}

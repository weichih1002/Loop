package com.teamloop.ncumis.loop;


import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;

import uk.co.dolphin_com.sscore.BarGroup;
import uk.co.dolphin_com.sscore.Component;
import uk.co.dolphin_com.sscore.CursorRect;
import uk.co.dolphin_com.sscore.Item;
import uk.co.dolphin_com.sscore.PartName;
import uk.co.dolphin_com.sscore.Point;
import uk.co.dolphin_com.sscore.RenderItem;
import uk.co.dolphin_com.sscore.SScore;
import uk.co.dolphin_com.sscore.SSystem;
import uk.co.dolphin_com.sscore.TimedItem;
import uk.co.dolphin_com.sscore.ex.ScoreException;


/**
 * The SystemView is a {@link View} which displays a single {@link SSystem}.
 * <p>{@link ScoreView} manages layout and placement of these into a scrolling View to display the complete {@link SScore}
 */
public class SystemView extends View {

    static final boolean ShowBarCursorOnTap = false;

    /**
     * construct the SystemView
     *
     * @param context the Context
     * @param score the score
     * @param sys the system
     * @param am the AssetManager for fonts
     */
    public SystemView(Context context, SScore score, SSystem sys, AssetManager am)
    {
        super(context);
        this.assetManager = am;
        this.system = sys;
        this.score = score;
        backgroundPaint = new Paint();
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setColor(0xFFFFFFFA);
        backgroundPaintRect = new RectF();
        tappedItemPaint = new Paint();
        tappedItemPaint.setStyle(Paint.Style.STROKE);
        tappedItemPaint.setColor(0xFF000080); // red
        barRectPaint = new Paint();
        barRectPaint.setStyle(Paint.Style.STROKE);
        barRectPaint.setStrokeWidth(3);
        barRectPaint.setColor(0xFF0000FF); // blue
        this.tl = new Point(0,0);// topleft
        this.drawItemRect = false;
        zoomingMag = 1;
        viewRect = new Rect();
        isZooming = false;
        tappedBarIndex = -1;
    }

    /** request a special colouring for a particular item in this System
     *
     * @param item_h the unique identifier for the item
     */
    public void colourItem(int item_h)
    {
        if (item_h != 0)
        {
            renderItems = new RenderItem[1];
            int[] coloured_render = new int[1];
            coloured_render[0] = RenderItem.ColourRenderFlags_notehead;
            renderItems[0] = new RenderItem(item_h, new RenderItem.Colour(1,0,0,1), coloured_render);
        }
        else
        {
            renderItems = null;
        }
        invalidate();
    }

    /**
     * called by android to measure this view
     */
    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        float w = system.bounds().width;
        float h = system.bounds().height;
        setMeasuredDimension((int)w,(int)h);
    }

    /**
     * called by android to draw the View
     */
    protected void onDraw(Canvas canvas)
    {
        backgroundPaintRect.left = 0;
        backgroundPaintRect.top = 0;
        backgroundPaintRect.right = canvas.getWidth();
        backgroundPaintRect.bottom = getHeight();
        canvas.drawRect(backgroundPaintRect, backgroundPaint);
        if (system != null)
        {
            if (renderItems != null)
            {
                // render notehead opaque red
                try {
                    system.drawWithOptions(canvas, assetManager, tl, zoomingMag, renderItems);
                } catch (ScoreException e) {
                    System.out.println(" error on draw:" + e);
                }
            }
            else
                system.draw(canvas, assetManager, tl, zoomingMag);
        }
        if (drawItemRect)
        {
            canvas.drawRect(tappedItemRect, tappedItemPaint);
        }
        if (tappedBarIndex >= 0)
        {
            CursorRect cr = system.getCursorRect(canvas, tappedBarIndex);
            if (cr.barInSystem)
            {
                canvas.drawRect(cr.rect, barRectPaint);
            }
        }
    }

    /**
     * called during active pinch-zooming. We just draw the same system magnified
     *
     * @param zoom the magnification
     */
    void zooming(final float zoom)
    {
        zoomingMag = zoom;
        if (!isZooming)
        {
            viewRect.top = getTop();
            viewRect.bottom = getBottom();
            isZooming = true;
        }
        setTop((int) (viewRect.top * zoom));
        setBottom((int) (viewRect.bottom * zoom));
        invalidate();
    }

    /**
     * Sample touch handler which prints information about the touched item into the console
     */
    public boolean onTouchEvent (MotionEvent event)
    {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN)
        {
            Point p = new Point(event.getX(), event.getY());
            int partindex = system.getPartIndexForYPos(event.getY());
            int barindex = system.getBarIndexForXPos(event.getX());
            if (ShowBarCursorOnTap)
            {
                tappedBarIndex = barindex;
                invalidate();
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable(){

                    public void run() {
                        tappedBarIndex = -1;
                        drawItemRect = false;
                        colourItem(0); // uncolour
                        invalidate();
                    }
                }, 1000); // clear after 1s
            }
            Component[] components;
            try {
                components = system.hitTest(p);
                for (Component comp : components)
                {
                    java.lang.System.out.println(comp.toString());
                    tappedItemRect = comp.rect; // draw a rect around the item
                    drawItemRect = true;
                    colourItem(comp.item_h); // colour item red
                    invalidate();

                    RectF comprect = system.getBoundsForItem(comp.item_h);
                    java.lang.System.out.println("rect:" + comprect);
                    Component[] itemComponents = system.getComponentsForItem(comp.item_h);
                    if (itemComponents.length > 0)
                    {
                        java.lang.System.out.println(" composed of:");
                        for (Component icomp : itemComponents)
                        {
                            java.lang.System.out.println("  " + icomp.toString());
                            RectF icomprect = system.getBoundsForItem(icomp.item_h);
                            java.lang.System.out.println("  rect:" + icomprect);
                        }
                    }
                    TimedItem ti = score.getItemForHandle(partindex, barindex, comp.item_h);
                    java.lang.System.out.println(" item:" + ti);
                }
            } catch (ScoreException e) {
                java.lang.System.out.println(" exception:" + e.toString());
            }

            PartName pname = score.getPartNameForPart(partindex);
            java.lang.System.out.println(" part:" + pname.name + " (" + pname.abbrev + ") bar:" + score.getBarNumberForIndex(barindex));
            try
            {
                BarGroup bg = score.getBarContents( partindex,  barindex);
                for (Item item : bg.items)
                {
                    // if we have the contents-detail licence we can get detailed info
                    try
                    {
                        TimedItem timed_item = score.getItemForHandle( partindex, barindex, item.item_h);
                        if (timed_item != null)
                            System.out.println(timed_item.toString());
                        else
                            System.out.println(item.toString());
                    }
                    catch (ScoreException e){ // no licence - just print undetailed item
                        System.out.println(item.toString());
                    }
                }
            }
            catch (ScoreException e)
            {
                java.lang.System.out.println(" exception:" + e.toString());
            }
        }
        return false;
    }

    private SScore score;
    private SSystem system;
    private AssetManager assetManager;
    private Point tl;
    private Paint backgroundPaint;
    private RectF backgroundPaintRect;
    private boolean drawItemRect;
    private RectF tappedItemRect;
    private Paint tappedItemPaint;
    private Paint barRectPaint;
    private float zoomingMag;
    private RenderItem[] renderItems;
    private Rect viewRect;
    private boolean isZooming;
    private int tappedBarIndex;
}

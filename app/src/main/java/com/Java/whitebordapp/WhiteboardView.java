package com.Java.whitebordapp;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import java.util.LinkedList;

import com.Java.whitebordapp.R;

public class WhiteboardView extends View {
    
    public static final int MODE_ERASER = 0;
    public static final int MODE_MARKER = 1;

    android.graphics.Path touchPath;
    Paint touchPaint, canvasPaint, backgroundPaint;
    Bitmap canvasBitmap;
    Canvas touchCanvas;

    LinkedList<PaintPath> pathHistory, undoHistory;
    PathListener l;

    int canvasHeight, canvasWidth;
    int markerColor, eraserColor;
    int markerThickness;
    int touchMode = MODE_MARKER;


    public WhiteboardView(Context context) {
        super(context);
        init(null, 0);
    }

    public WhiteboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public WhiteboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }


    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a;
        a = getContext().obtainStyledAttributes(attrs,
                R.styleable.WhiteboardView, defStyle, 0);

        eraserColor = getDefaultEraserColor();
        markerColor = a.getColor(R.styleable.WhiteboardView_markerColor, getDefaultMarkerColor());
        markerThickness = a.getDimensionPixelSize(R.styleable.WhiteboardView_markerThickness,
                getDefaultMarkerThickness());
        touchMode = a.getInt(R.styleable.WhiteboardView_touchMode, touchMode);
        a.recycle();

                a.recycle();
                initTouchPaint();
                initBackgroundPaint();
                initCanvasPaint();
                initTouchPath();
                initHistory();
    }

    private int getDefaultEraserColor() {
        return getContext().getResources().getColor(R.color.whiteboard_default_eraser_color);
    }

    private int getDefaultMarkerThickness() {
        return (int) getContext().getResources().getDimension(R.dimen.whiteboard_marker_thickness);
    }

    private int getDefaultMarkerColor() {
        return getContext().getResources().getColor(R.color.whiteboard_default_marker_color);
    }

    private void initTouchPaint(){
        if(touchPaint == null) {
            touchPaint = new Paint();
            touchPaint.setAntiAlias(true);
            touchPaint.setColor( (touchMode == MODE_ERASER) ? eraserColor : markerColor);
            touchPaint.setStyle(Paint.Style.STROKE);
            touchPaint.setStrokeWidth(markerThickness);
        }
    }

    private void initBackgroundPaint(){
        if(backgroundPaint == null){
            backgroundPaint = new Paint();
            backgroundPaint.setAntiAlias(true);
            backgroundPaint.setColor(Color.WHITE);
            backgroundPaint.setStyle(Paint.Style.FILL);
        }
    }

    private void initCanvasPaint() {
        if(canvasPaint == null) {
            canvasPaint = new Paint();
        }
    }

    private void initTouchPath() {
        if(touchPath == null) {
            touchPath = new android.graphics.Path();
        }
        touchPath.reset();
    }

    private void initHistory() {
        pathHistory = new LinkedList<>();
        undoHistory = new LinkedList<>();
    }

    private void initCanvas(){
        if(canvasBitmap != null){
            canvasBitmap.recycle();
        }

        canvasBitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888);
        touchCanvas = new Canvas(canvasBitmap);
    }

    private void initCanvas(int width, int height){
        this.canvasWidth = width;
        this.canvasHeight = height;
        initCanvas();
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){
        super.onSizeChanged(w,h,oldw,oldh);
        initCanvas(w, h);
    }

    @Override
    public boolean onTouchEvent (@NonNull MotionEvent event){
        int action = event.getAction();
        switch(action){
            case MotionEvent.ACTION_DOWN:
                touchPath.moveTo(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                touchPath.lineTo(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_UP:
                touchCanvas.drawPath(touchPath, touchPaint);
                recordPath();

                if(l != null){
                    l.onPathCompleted();
                }
                break;
            default:
                return false;
        }

        invalidate();
        return true;
    }

    private void recordPath() {
        PaintPath paintPath = new PaintPath(new Paint(touchPaint), new android.graphics.Path(touchPath));
        pathHistory.push(paintPath);
        touchPath.reset();

        undoHistory.clear();
    }

    private void redrawCanvasBitmap() {
        initCanvas();

        for(int i=pathHistory.size()-1; i>=0; i--){
            PaintPath paintPath = pathHistory.get(i);
            touchCanvas.drawPath(paintPath.getPath(), paintPath.getPaint());
        }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // draw the existing bitmap of our canvas
        canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
        // draw the touch path over the bitmap
        canvas.drawPath(touchPath, touchPaint);
    }

    public void clear(){
        initTouchPath();
        initCanvas();
        initHistory();

        if(l != null){
            l.onPathsCleared();
        }

        invalidate();
    }

    public void setMarkerColor(int color){
        markerColor = color;
        if(touchMode != MODE_ERASER){
            touchPaint.setColor(markerColor);
        }
    }

    public void activateEraser(){
        touchMode = MODE_ERASER;
        touchPaint.setColor(eraserColor);
    }

    public void activateMarker(){
        touchMode = MODE_MARKER;
        touchPaint.setColor(markerColor);
    }

    public int getTouchMode(){
        return touchMode;
    }

    public void setMarkerThickness(int thickness){
        markerThickness = thickness;
        touchPaint.setStrokeWidth(thickness);
    }

 void undo(){
        if(pathHistory.size() > 0){
            PaintPath undoPath = pathHistory.pop();
            undoHistory.push(undoPath);
            redrawCanvasBitmap();

            if(l != null){
                l.onPathUndone();
            }
        }
    }

    public void redo(){
        if(undoHistory.size() > 0){
            PaintPath lastUndone = undoHistory.pop();
            pathHistory.push(lastUndone);
            redrawCanvasBitmap();

            if(l != null){
                l.onPathRedone();
            }
        }
    }

    public boolean canUndo(){
        return (pathHistory.size() > 0);
    }

    public boolean canRedo(){
        return (undoHistory.size() > 0);
    }

    public void setPathListener(PathListener l){
        this.l = l;
    }

    public void redraw(){
        redrawCanvasBitmap();
    }

    public Bitmap screenshot(){
        Bitmap result = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888);
        Canvas resultCanvas = new Canvas(result);
        resultCanvas.drawPaint(backgroundPaint);
        resultCanvas.drawBitmap(canvasBitmap, 0, 0, null);

        return result;
    }

    public interface PathListener{
        void onPathCompleted();
        void onPathUndone();
        void onPathRedone();
        void onPathsCleared();
    }


    private class WhiteboardView_touchMode {
    }
}

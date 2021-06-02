package com.Java.whitebordapp;

import android.graphics.Paint;
import android.graphics.Path;

public class PaintPath {
    Path path;
    Paint paint;

    PaintPath(Paint paint, Path path){
        this.paint = paint;
        this.path = path;
    }

    public Paint getPaint() {
        return paint;
    }

    public Path getPath() {
        return path;
    }
}
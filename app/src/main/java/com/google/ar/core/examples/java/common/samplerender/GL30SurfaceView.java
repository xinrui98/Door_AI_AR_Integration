package com.google.ar.core.examples.java.common.samplerender;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;


public class GL30SurfaceView extends GLSurfaceView {

    private TouchListener touchListener;

    public GL30SurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean performClick(){
        super.performClick();
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return touchListener.onTouch(ev);
    }

    public void setTouchListener(TouchListener touchListener){
        this.touchListener = touchListener;
    }

    interface TouchListener {
        boolean onTouch(MotionEvent ev);
    }

    abstract static class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public abstract boolean onScale(ScaleGestureDetector detector);
    }
}

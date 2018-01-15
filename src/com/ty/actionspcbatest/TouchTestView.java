package com.ty.actionspcbatest;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class TouchTestView extends View {
	public Vector<Point> mInput = new Vector<Point>();
	public HashMap<View,Rect> mUntouchRects = new HashMap<View,Rect>();
	private Paint mLinePaint;

	public TouchTestView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mLinePaint = new Paint();
        mLinePaint.setAntiAlias(true);
        mLinePaint.setARGB(255,0,0,255);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		for(int i=0;i<mInput.size()-1;i++){
			Point p1 = mInput.get(i);
			Point p2 = mInput.get(i+1);
            canvas.drawLine(p1.x,p1.y,p2.x,p2.y,mLinePaint);
        }
    }
	
	@Override
	public boolean onTouchEvent(MotionEvent e) {
		boolean handled = false;
		int action = e.getAction();
		if(MotionEvent.ACTION_DOWN == action || MotionEvent.ACTION_MOVE == action){
			Iterator iter = mUntouchRects.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry entry = (Map.Entry) iter.next();
				//Object key = entry.getKey();
				Rect rect = (Rect)entry.getValue();
				if(rect.contains((int)e.getX(), (int)e.getY())){
					return false;
				}
			}
			if(MotionEvent.ACTION_DOWN == action){
				mInput.clear();
			}
			mInput.add(new Point((int)e.getX(),(int)e.getY()));
			this.postInvalidate();
			handled = true;
		}
		return handled;
	}
	
	public void addUntouchRect(View view, Rect rect){
		mUntouchRects.put(view, rect);
	}
}

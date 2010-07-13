package com.galoula.messenger;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class CTProgressView extends View {

	private int foregroundColor;
	private int min=0;
	private int max=100;
	private Paint sPaint;
	private int position;
	
	public void setColor(int color) {
		foregroundColor=color;
	}
	
	public void setProgress(int progress) {
		position=progress;
	}
	
	public CTProgressView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.setBackgroundColor(0xFF464646);
		sPaint = new Paint();
		sPaint.setColor(0xFF668800);
		sPaint.setStyle(Paint.Style.FILL);
		position=0;
		// TODO Auto-generated constructor stub
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		sPaint.setColor(foregroundColor);
        int truepos = (int) (((float)position/(float)(max-min)) * this.getWidth());
		canvas.drawRect(0, 0, truepos, this.getHeight(), sPaint);
	}
	

}
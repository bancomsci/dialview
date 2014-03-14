package com.mindtherobot.samples.dialview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.BlurMaskFilter.Blur;
import android.graphics.Shader.TileMode;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.OnGestureListener;

@SuppressLint("WrongCall")
public class DialView extends View implements DialModel.Listener, OnGestureListener {

	private static final String TAG = DialView.class.getSimpleName();
	
	private static boolean toolsInitialized = false;
	private static Rect bounds;
	private static Bitmap texture;
	private static Paint texturePaint;
	private static Paint outerShadowPaint;
	private static Paint sideNickPaint;
	private static Paint innerShadowPaint;
	private static Paint centerShadowPaint;
	private static Paint centerBlurPaint;
	private static Paint innerNickPaint;
	private static Paint innerNickOutlinePaint;
	private static RectF nickRect;
	
	private static SoundPool soundPool;
	private static int clickSoundId;
	
	private static Paint createDefaultPaint() {
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setFilterBitmap(true);
		return paint;
	}
	
	private static void initSoundPoolIfNecessary(Context context) {
		if (soundPool == null) {
			soundPool = new SoundPool(1, AudioManager.STREAM_SYSTEM, 0);
			clickSoundId = soundPool.load(context, R.raw.click_sound, 1);
		}
	}
	
	private static void initDrawingToolsIfNecessary(Context context) {
		if (! toolsInitialized) {
			bounds = new Rect();
			
			// there's a subtle thing here. technically, different instances
			// of DialView might use different contexts. however, what we are
			// creating here is a Bitmap which is not bound to any context. 
			texture = BitmapFactory.decodeResource(context.getResources(),
												   R.drawable.dial_texture);
			texturePaint = createDefaultPaint();
			BitmapShader textureShader = new BitmapShader(texture, 
														  TileMode.MIRROR, 
														  TileMode.MIRROR);
			Matrix textureMatrix = new Matrix();
			textureMatrix.setScale(1.0f / texture.getWidth(), 1.0f / texture.getHeight());
			textureShader.setLocalMatrix(textureMatrix);
			texturePaint.setShader(textureShader);	
			
			outerShadowPaint = createDefaultPaint();
			outerShadowPaint.setShader(new LinearGradient(0.0f, 0.0f, 1.0f, 1.0f, 
												     new int[] { 0x00ffffff, 0xbb000000 }, 
												     new float[] { 0.0f, 0.9f },
												     TileMode.CLAMP));
			
			sideNickPaint = createDefaultPaint();
			sideNickPaint.setStyle(Paint.Style.STROKE);
			sideNickPaint.setColor(0xdd000000);
			sideNickPaint.setStrokeWidth(0.03f);
			sideNickPaint.setMaskFilter(new BlurMaskFilter(0.007f, Blur.NORMAL));
			
			innerShadowPaint = createDefaultPaint();
			innerShadowPaint.setShader(new LinearGradient(0.2f, 0.2f, 0.8f, 0.8f, 
												     new int[] { 0x66000000, 0x00ffffff }, 
												     new float[] { 0.0f, 1.0f },
												     TileMode.CLAMP));
			innerShadowPaint.setMaskFilter(new BlurMaskFilter(0.01f, Blur.NORMAL));
			
			centerShadowPaint = createDefaultPaint();
			centerShadowPaint.setShader(new RadialGradient(0.4f, 0.4f, 0.5f, 
												     new int[] { 0x00ffffff, 0xbb000000 }, 
												     new float[] { 0.0f, 1.0f },
												     TileMode.CLAMP));
			centerShadowPaint.setMaskFilter(new BlurMaskFilter(0.01f, Blur.NORMAL));
	
			centerBlurPaint = createDefaultPaint();
			centerBlurPaint.setMaskFilter(new BlurMaskFilter(0.01f, Blur.NORMAL));
			centerBlurPaint.setStyle(Paint.Style.STROKE);
			centerBlurPaint.setShader(textureShader);
			centerBlurPaint.setStrokeWidth(0.01f);
			
			innerNickPaint = createDefaultPaint();
			innerNickPaint.setShader(new LinearGradient(0.0f, 0.0f, 1.0f, 0.0f, 
				     				 new int[] { 0x55fffff8, 0x55fffff8, 0xbb000000 }, 
				     				 new float[] { 0.0f, 0.5f, 0.52f },
				     				 TileMode.CLAMP));
			
			innerNickOutlinePaint = createDefaultPaint();
			innerNickOutlinePaint.setStyle(Paint.Style.STROKE);
			innerNickOutlinePaint.setColor(0x22000000);
			innerNickOutlinePaint.setStrokeWidth(0.005f);
			innerNickOutlinePaint.setMaskFilter(new BlurMaskFilter(0.005f, Blur.NORMAL));
			
			nickRect = new RectF();
			
			toolsInitialized = true;
		}
	}
	
	private float outerBorderSize = 0.025f;
	private float sideNickSize = 0.011f;
	private int totalSideNicks = 60;
	private float centerRadius = 0.28f;
	
	private GestureDetector gestureDetector;
	private float dragStartDeg = Float.NaN;
	private float luftRotation = 0.0f;
	
	private DialModel model;
	private Handler handler;
	
	private long redrawTime;
	private int redrawTimes = 0;
	
	private DrawLayer outerLayer;
	private DrawLayer outerShadowLayer;
	private DrawLayer innerLayer;
	private DrawLayer innerShadowLayer;
	private DrawLayer nickAndCenterLayer;
	private DrawLayer centerShadowLayer;
	
	public DialView(Context context) {
		super(context);
		init();
	}

	public DialView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public DialView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		initDrawingToolsIfNecessary(getContext());
		initSoundPoolIfNecessary(getContext());
		
		gestureDetector = new GestureDetector(getContext(), this);
		
		setModel(new DialModel());
		handler = new Handler();
		
		outerLayer = new DrawLayer();
		outerShadowLayer = new DrawLayer();
		innerLayer = new DrawLayer();
		innerShadowLayer = new DrawLayer();
		nickAndCenterLayer = new DrawLayer();
		centerShadowLayer = new DrawLayer();
	}

	public final void setModel(DialModel model) {
		if (this.model != null) {
			this.model.removeListener(this);
		}
		this.model = model;
		this.model.addListener(this);
		
		invalidate();
	}
	
	public final DialModel getModel() {
		return model;
	}
	
	@SuppressLint("DrawAllocation")
	@Override
	protected final void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		SquareViewMeasurer measurer = new SquareViewMeasurer(100);
		measurer.onMeasure(widthMeasureSpec, heightMeasureSpec);
		setMeasuredDimension(measurer.getChosenDimension(), measurer.getChosenDimension());
	}

	private float getBaseRadius() {
		return 0.48f; // to avoid some aliasing issues
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		canvas.getClipBounds(bounds);

		long startTime = SystemClock.elapsedRealtime();
		
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		{
			canvas.translate(bounds.left, bounds.top);

			float rotation = model.getRotationInDegrees() + luftRotation;
			float midX = bounds.width() / 2.0f;
			float midY = bounds.height() / 2.0f;

			canvas.rotate(rotation, midX, midY);
			outerLayer.drawOn(canvas, 0, 0);
			canvas.rotate(- rotation, midX, midY);
			outerShadowLayer.drawOn(canvas, 0, 0);

			canvas.rotate(rotation, midX, midY);
			innerLayer.drawOn(canvas, 0, 0);
			canvas.rotate(- rotation, midX, midY);
			innerShadowLayer.drawOn(canvas, 0, 0);
			
			canvas.rotate(rotation, midX, midY);
			nickAndCenterLayer.drawOn(canvas, 0, 0);

			canvas.rotate(- rotation, midX, midY);
			centerShadowLayer.drawOn(canvas, 0, 0);
		}		
		canvas.restore();

		long endTime = SystemClock.elapsedRealtime();
		redrawTime += (endTime - startTime);
		if (++redrawTimes >= 10) {
			Log.d(TAG, "Av. time = " + (redrawTime / ((float) redrawTimes)) + " ms");
			
			redrawTimes = 0;
			redrawTime = 0;
		}
	}

	private void drawCenter(Canvas canvas) {
		canvas.drawCircle(0.5f, 0.5f, centerRadius, texturePaint);
		canvas.drawCircle(0.5f, 0.5f, centerRadius, centerBlurPaint);
	}

	private void drawInnerCircle(Canvas canvas, float baseRadius) {
		canvas.drawCircle(0.5f, 0.5f, baseRadius - outerBorderSize, texturePaint);
	}

	private void drawOuterCircle(Canvas canvas, float baseRadius) {
		canvas.drawCircle(0.5f, 0.5f, baseRadius, texturePaint);
	}

	private void drawCenterShadow(Canvas canvas) {
		canvas.drawCircle(0.5f, 0.5f, centerRadius - 0.02f, centerShadowPaint);
	}
	
	private void drawInnerNicks(Canvas canvas, float baseRadius) {
		float innerNickStep = 360.0f / model.getTotalNicks();
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		{
			for (int i = 0; i < model.getTotalNicks(); ++i) {
				canvas.rotate(innerNickStep, 0.5f, 0.5f);
				
				float width = 0.03f;
				float height = baseRadius - outerBorderSize - centerRadius;
				height = height - (float) (Math.random() * 0.005);
				
				nickRect.set(0.5f - width / 2, 
						0.5f - baseRadius + outerBorderSize + 0.02f, 
						0.5f + width / 2, 
						0.5f - baseRadius + outerBorderSize + height - 0.03f); 
						
				canvas.drawRoundRect(nickRect, 0.02f, 0.02f, innerNickPaint);
				canvas.drawRoundRect(nickRect, 0.02f, 0.02f, innerNickOutlinePaint);
			}
			
			canvas.restore();
		}
	}

	private void drawInnerShadow(Canvas canvas, float baseRadius) {
		canvas.drawCircle(0.5f, 0.5f, baseRadius - outerBorderSize,  innerShadowPaint);
	}

	private void drawOuterShadow(Canvas canvas, float baseRadius) {
		canvas.drawCircle(0.5f, 0.5f, baseRadius + 0.005f, outerShadowPaint);
	}

	private void drawSideNicks(Canvas canvas, float baseRadius) {
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		{
			float sideNickStep = 360.0f / totalSideNicks;
			for (int i = 0; i < totalSideNicks; ++i) {
				canvas.rotate(sideNickStep, 0.5f, 0.5f);

				float height = sideNickSize;
				if (i % 9 == 0) {
					height += 0.003f;
				}
				
				canvas.drawLine(0.5f, 0.5f - baseRadius - 0.005f, 
								0.5f, 0.5f - baseRadius - 0.005f + height, 
								sideNickPaint);
			}
			canvas.restore();
		}
	}

	@Override
	public void onDialPositionChanged(DialModel sender, int nicksChanged) {
		luftRotation = (float) (Math.random() * 1.0f - 0.5f);				
		invalidate();
		
		playSound(nicksChanged);
	}

	private void playSound(int nicksChanged) {
		int sounds = Math.min(3, Math.abs(nicksChanged));
		for (int i = 0; i < sounds; ++i) {
			soundPool.play(clickSoundId, 0.03f, 0.03f, 0, 0, 1.0f);
			SystemClock.sleep(10);
		}
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		Bundle bundle = (Bundle) state;
		super.onRestoreInstanceState(bundle.getParcelable("superState"));
		
		setModel(DialModel.restore(bundle));
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		Bundle bundle = new Bundle();
		bundle.putParcelable("superState", superState);

		model.save(bundle);
		
		return bundle;
	}

	private float xyToDegrees(float x, float y) {
		float distanceFromCenter = PointF.length((x - 0.5f), (y - 0.5f));
		if (distanceFromCenter < 0.1f
				|| distanceFromCenter > 0.5f) { // ignore center and out of bounds events
			return Float.NaN;
		} else {
			return (float) Math.toDegrees(Math.atan2(x - 0.5f, y - 0.5f));
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (gestureDetector.onTouchEvent(event)) {
			return true;
		} else {
			return super.onTouchEvent(event);
		}
	}

	@Override
	public boolean onDown(MotionEvent event) {
		float x = event.getX() / ((float) getWidth());
		float y = event.getY() / ((float) getHeight());
		
		dragStartDeg = xyToDegrees(x, y);
		Log.d(TAG, "deg = " + dragStartDeg);
		if (! Float.isNaN(dragStartDeg)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean onFling(MotionEvent eventA, MotionEvent eventB, float vx, float vy) {
		return false;
	}

	@Override
	public void onLongPress(MotionEvent event) {

	}

	@Override
	public boolean onScroll(MotionEvent eventA, MotionEvent eventB, float dx, float dy) {
		if (! Float.isNaN(dragStartDeg)) {
			float currentDeg = xyToDegrees(eventB.getX() / getWidth(), 
										   eventB.getY() / getHeight());
			
			if (! Float.isNaN(currentDeg)) {
				float degPerNick = 360.0f / model.getTotalNicks();
				float deltaDeg = dragStartDeg - currentDeg;

				final int nicks = (int) (Math.signum(deltaDeg) 
						* Math.floor(Math.abs(deltaDeg) / degPerNick));
				
				if (nicks != 0) {
					dragStartDeg = currentDeg;

					handler.post(new Runnable() {
						@Override
						public void run() {
							model.rotate(nicks);								
						}
					});
				} 
			} 
			
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void onShowPress(MotionEvent event) {
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent event) {
		return false;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		
		outerLayer.onSizeChange(w, h);
		outerShadowLayer.onSizeChange(w, h);
		innerLayer.onSizeChange(w, h);
		innerShadowLayer.onSizeChange(w, h);
		nickAndCenterLayer.onSizeChange(w, h);
		centerShadowLayer.onSizeChange(w, h);
		
		regenerateLayers(w);
	}
	
	private void regenerateLayers(int size) {
		float baseRadius = getBaseRadius();
		
		float scale = (float) size;
		
		Canvas canvas = outerLayer.getCanvas();
		canvas.scale(scale, scale);
		drawOuterCircle(canvas, baseRadius);
		drawSideNicks(canvas, baseRadius);
		
		canvas = outerShadowLayer.getCanvas();
		canvas.scale(scale, scale);
		drawOuterShadow(canvas, baseRadius);
		
		canvas = innerLayer.getCanvas();
		canvas.scale(scale, scale);
		drawInnerCircle(canvas, baseRadius);
		
		canvas = innerShadowLayer.getCanvas();
		canvas.scale(scale, scale);
		drawInnerShadow(canvas, baseRadius);
		
		canvas = nickAndCenterLayer.getCanvas();
		canvas.scale(scale, scale);
		drawInnerNicks(canvas, baseRadius);
		drawCenter(canvas);

		canvas = centerShadowLayer.getCanvas();
		canvas.scale(scale, scale);
		drawCenterShadow(canvas);
	}
	
	// TODO: dispose the layers
}

package com.example.androidconvolutioncorrelation;

import java.util.Arrays;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class CSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

	private static final String TAG = CSurfaceView.class.getSimpleName();
	private Context drawContext;
	public DrawThread drawThread = null;
	private SurfaceHolder drawSurfaceHolder;
	public boolean drawAnimation = true; // default is to draw q1 and q3
	private boolean flag = true;
	private static int blueCurvePos = 0;
	 

	private static final Handler handler = new Handler();

	public CSurfaceView(Context ctx, AttributeSet attributeSet) {
		super(ctx, attributeSet);
		Log.i(TAG, "surface view constructor");
		drawContext = ctx;
	}

	@Override
	public void surfaceCreated(SurfaceHolder paramSurfaceHolder) {
		Log.i(TAG, "serface created");
	}

	@Override
	public void surfaceChanged(SurfaceHolder paramSurfaceHolder, int pixelFormat,
			int newWidth, int newHeight) {
		Log.i(TAG, "surface changed");
		drawThread.setSurfaceSize(newWidth, newHeight);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder paramSurfaceHolder) {
		Log.i(TAG, "serface destroyed");
		int i = 1;
		while (true) {
			if (i == 0)
				return;
			try {
				Log.i(TAG, "thread ready to join");
				flag = false;
				drawThread.join();
				i = 0;
			} catch (InterruptedException localInterruptedException) {
				Log.i(TAG, "huhhhh");
			}
		}
	}
	
	public void startDrawing() {
		if (drawThread == null) {
			drawSurfaceHolder = getHolder();
			drawSurfaceHolder.addCallback(this);
			drawThread = new DrawThread(drawSurfaceHolder, drawContext, handler);
			drawThread.setName("" + System.currentTimeMillis());
		}
		if(drawThread.isAlive()) return;
		
		// delay drawing in order not to block the UI thread during activity switching
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				drawThread.start();
			}
		}, 90);
	}

	class DrawThread extends Thread {
		private Bitmap soundBackgroundImage;
		private SurfaceHolder soundSurfaceHolder;
		private Paint redPaint, bluePaint;
		private int drawScale = 30;
		private static final int blueCurveLength = 101;
		private static final int FFT_Len = 512;
		private double[] redCurve, blueCurve, convertedBlueCurve,
				convolutionCurve, correlationCurve;
		private double[] redStatic, blueStatic, redFFT, blueFFT, blueFFTCC, convolutionFFT, convolutionFFTMag, correlationFFT, correlationFFTMag;

		public DrawThread(SurfaceHolder paramContext, Context paramHandler, Handler arg4) {
			soundSurfaceHolder = paramContext;
			soundBackgroundImage = Bitmap.createBitmap(1, 1,
					Bitmap.Config.ARGB_8888);

			redPaint = new Paint();
			redPaint.setAntiAlias(true);
			redPaint.setARGB(255, 255, 0, 0);
			redPaint.setStrokeWidth(5);

			bluePaint = new Paint();
			bluePaint.setARGB(255, 0, 0, 255);
			bluePaint.setStrokeWidth(5);
		}

		/***************** the following performs FFT on the signal ***********/
		public void doDraw(Canvas canvas) {

			int height = canvas.getHeight();
			int width = canvas.getWidth();
			
			Paint paint = new Paint();
			paint.setColor(Color.YELLOW);
			paint.setStyle(Style.FILL);
			canvas.drawPaint(paint);

			// Log.i(TAG, "width = " + width); // 984

			// Q1 and Q3
			if (drawAnimation) {

				// Draw red curve
				float yStart, yStop;
				int thresd1 = width / 2 - 100, thresd2 = width / 2 + 100;
				if (redCurve == null || redCurve.length == 0) {
					redCurve = new double[width];
					Arrays.fill(redCurve, 0);
					Arrays.fill(redCurve, thresd1, thresd2 + 1, 2);
				}
				for (int x = 0; x < width - 1; x++) {
					yStart = (float) redCurve[x];
					yStop = (float) redCurve[x + 1];

					yStart *= -drawScale;
					yStop *= -drawScale;

					yStart += height / 8;
					yStop += height / 8;

					canvas.drawLine(x, yStart, x + 1, yStop, redPaint);
				}

				// Draw blue curve
				if (blueCurve == null || blueCurve.length == 0) {
					blueCurve = new double[101];
					for (int i = 0; i <= 100; i++) {
						blueCurve[i] = 1 - i / 100.0;
					}
				}
				
				// leftmost vertical line (just to look beautiful) 
				canvas.drawLine(blueCurvePos, height / 8, blueCurvePos, (float) blueCurve[0] * (-drawScale) + height / 8, bluePaint);
				for (int x = 0; x < blueCurveLength - 1; x++) {
					yStart = (float) blueCurve[x];
					yStop = (float) blueCurve[x + 1];

					yStart *= -drawScale;
					yStop *= -drawScale;

					yStart += height / 8;
					yStop += height / 8;

					canvas.drawLine(blueCurvePos + x, yStart, blueCurvePos + x + 1, yStop, bluePaint);
				}

				// Convolution
				if (convertedBlueCurve == null
						|| convertedBlueCurve.length == 0) {
					convertedBlueCurve = new double[blueCurveLength];
					for (int i = 0; i < blueCurve.length; i++) {
						convertedBlueCurve[i] = blueCurve[blueCurveLength - 1 - i];
					}
					
					convolutionCurve = new double[width];
					Arrays.fill(convolutionCurve, 0);
					for (int i = thresd1; i < thresd2 + blueCurveLength; i++) {
						for (int j = 0; j < blueCurveLength; j++) {
							convolutionCurve[i] += convertedBlueCurve[j]
									* redCurve[i - convertedBlueCurve.length + 1 + j];
						}
					}
				}

				for (int x = 0; x < width - 1; x++) {
					yStart = (float) redCurve[x];
					yStop = (float) redCurve[x + 1];

					if (x < blueCurvePos + blueCurveLength) {
						yStart = (float) convolutionCurve[x];
						yStop = (float) convolutionCurve[x + 1];
					}

					yStart *= -5;
					yStop *= -5;

					yStart += height / 2;
					yStop += height / 2;

					canvas.drawLine(x, yStart, x + 1, yStop, redPaint);
				}

				// Correlation
				if (correlationCurve == null || correlationCurve.length == 0) {
					correlationCurve = new double[width];
					Arrays.fill(correlationCurve, 0);
					for (int i = thresd1; i < thresd2 + blueCurveLength; i++) {
						for (int j = 0; j < blueCurveLength; j++)
							correlationCurve[i] += blueCurve[j]
									* redCurve[i - blueCurveLength + 1 + j];
					}
				}

				for (int x = 0; x < width - 1; x++) {
					yStart = (float) redCurve[x];
					yStop = (float) redCurve[x + 1];

					if (x < blueCurvePos + blueCurveLength) {
						yStart = (float) correlationCurve[x];
						yStop = (float) correlationCurve[x + 1];
					}

					yStart *= -5;
					yStop *= -5;

					yStart += height * 19 / 20;
					yStop += height * 19 / 20;

					canvas.drawLine(x, yStart, x + 1, yStop, redPaint);
				}
			} else {
				// Q2 and Q4
				
				// create static curves
				if(redStatic == null || redStatic.length == 0) {
					redStatic = new double[FFT_Len];
					Arrays.fill(redStatic, 0);
					Arrays.fill(redStatic, 50, 251, 2);
				}
				
				if(blueStatic == null || blueStatic.length == 0) {
					blueStatic = new double[FFT_Len];
					Arrays.fill(blueStatic, 0);
					for (int i = 50; i <= 150; i++) {
						blueStatic[i] = 1 - (i - 50.0) / 100.0;
					}
				}
				
				if(redFFT == null || redFFT.length == 0 || blueFFT == null || blueFFT.length == 0 ) {
					// Perform FFT on both curves
					redFFT = new double[2 * FFT_Len];
					blueFFT = new double[2 * FFT_Len];
					for(int i = 0; i < FFT_Len; i++) {
						redFFT[2 * i] = redStatic[i];
						redFFT[2 * i + 1] = 0.0;
						blueFFT[2 * i] = blueStatic[i];
						blueFFT[2 * i + 1] = 0.0;
					}
					
					DoubleFFT_1D fft = new DoubleFFT_1D(FFT_Len);
					fft.complexForward(redFFT);
					fft.complexForward(blueFFT);
					
					// Get complex conjugate of blueFFT
					blueFFTCC = Arrays.copyOf(blueFFT, 2 * FFT_Len);
					for(int i = 1; i < 2 * FFT_Len; i += 2) {
						blueFFTCC[i] *= -1; 
					}
					
					// Multiply
					convolutionFFT = new double[2 * FFT_Len];
					correlationFFT = new double[2 * FFT_Len];
					for (int i = 0; i < FFT_Len; i++) {
						double a = redFFT[2 * i];
						double b = redFFT[2 * i + 1];

						double c = blueFFT[2 * i];
						double d = blueFFT[2 * i + 1];
						
						double e = blueFFTCC[2 * i];
						double f = blueFFTCC[2 * i + 1];
						
						convolutionFFT[i * 2] = a*c - b*d;
						convolutionFFT[i * 2 + 1] = a*d + b*c;
						
						correlationFFT[i * 2] = a*e - b*f;
						correlationFFT[i * 2 + 1] = a*f + b*e;
					}
					
					// Inverse FFT
					fft.complexInverse(convolutionFFT, false);
					fft.complexInverse(correlationFFT, false);
					
					convolutionFFTMag = new double[FFT_Len];
					correlationFFTMag = new double[FFT_Len];
					double mx1 = -1, mx2 = -1;
					for (int i = 0; i < FFT_Len; i++) {
						double re = convolutionFFT[2 * i];
						double im = convolutionFFT[2 * i + 1];
						convolutionFFTMag[i] = Math.sqrt(re * re + im * im);
						if(convolutionFFTMag[i] > mx1) mx1 = convolutionFFTMag[i];
						
						double re2 = correlationFFT[2 * i];
						double im2 = correlationFFT[2 * i + 1];
						correlationFFTMag[i] = Math.sqrt(re2 * re2 + im2 * im2);
						if(correlationFFTMag[i] > mx2) mx2 = correlationFFTMag[i];
					}
					
					// normalize
					for(int i = 0; i < FFT_Len; i++) {
						convolutionFFTMag[i] = convolutionFFTMag[i] / mx1 * 200;
						correlationFFTMag[i] = correlationFFTMag[i] / mx2 * 200;
					}
					
//					Log.i(TAG, "CONVOLUTION" + Arrays.toString(convolutionFFTMag));
//					Log.i(TAG, "CORRELATION" + Arrays.toString(correlationFFTMag));
				}
				
				// Display
				float yScale = -1; //width * 1.0f / FFT_Len
				for (int i = 0; i < FFT_Len - 1; i++) {
					
					// original curve
					canvas.drawLine(i, -(float)redStatic[i]*50 + height / 7, (i + 1),
							-(float)redStatic[i + 1]*50 + height / 7, redPaint);
					
					canvas.drawLine(i, -(float)blueStatic[i]*50 + height / 7, (i + 1),
							-(float)blueStatic[i + 1]*50 + height / 7, bluePaint);
					
					// convolution curve
					float yStart = (float) convolutionFFTMag[i];
					float yStop = (float) convolutionFFTMag[i + 1];
					
					yStart *= yScale;
					yStop *= yScale;

					yStart += height / 2;
					yStop += height / 2;
				
					canvas.drawLine(i, yStart, (i + 1), yStop, redPaint);
					
					// correlation curve
					yStart = (float) correlationFFTMag[i];
					yStop = (float) correlationFFTMag[i + 1];

					yStart += height * 4.0 / 5;
					yStop += height * 4.0 / 5;
				
					canvas.drawLine(i, yStart, (i + 1), yStop, redPaint);
				}
			}
		}

		public void setSurfaceSize(int canvasWidth, int canvasHeight) {
			synchronized (soundSurfaceHolder) {
				soundBackgroundImage = Bitmap.createScaledBitmap(
						soundBackgroundImage, canvasWidth, canvasHeight, true);
			}
		}

		@Override
		public void run() {
			while (flag) {

				Canvas localCanvas = null;
				int width;
				try {
					localCanvas = soundSurfaceHolder.lockCanvas(null);
					synchronized (soundSurfaceHolder) {
						if (localCanvas != null) {
							width = localCanvas.getWidth();
							doDraw(localCanvas);
							blueCurvePos += 1;
							if (blueCurvePos + blueCurveLength >= width)
								blueCurvePos = 0;
						}
					}
				} finally {
					if (localCanvas != null)
						soundSurfaceHolder.unlockCanvasAndPost(localCanvas);
				}
			}
		}
	}
}

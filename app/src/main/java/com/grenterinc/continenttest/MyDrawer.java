package com.grenterinc.continenttest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import java.util.Arrays;

public class MyDrawer extends View {
    Paint paint;
    Bitmap bitmap;

    public MyDrawer(Context context, AttributeSet attrs) {
        super(context, attrs);

      //  if (isInEditMode())
    //        return;
      Log.d("MAN","man,here");
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int sizeX = size.x <= 0? 500 : size.x;
        int sizeY = size.y <= 0? 500 : size.y;
        bitmap = Bitmap.createBitmap(sizeX, sizeY, Bitmap.Config.RGB_565);

        bitmap.setPixel(20, 20, Color.RED);
        bitmap.setPixel(70, 50, Color.RED);
        bitmap.setPixel(30, 80, Color.RED);

        int[] colors = new int[10*15];
        Arrays.fill(colors, 0, 10*15, Color.GREEN);
        bitmap.setPixels(colors, 0, 10, 40, 40, 10, 15);

        Canvas canvas = new Canvas(bitmap);
        Paint p = new Paint();
        p.setColor(Color.BLUE);
        canvas.drawCircle(80, 80, 10, p);
    }

    @Override
    protected void onDraw(Canvas canvas) {
      //  if (isInEditMode())
      //      return;
        canvas.drawARGB(80, 102, 204, 255);
        canvas.drawBitmap(bitmap, 0, 0, paint);
    }
}

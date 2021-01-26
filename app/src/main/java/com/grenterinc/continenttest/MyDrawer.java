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

import static com.grenterinc.continenttest.MainActivity.mPosX;

public class MyDrawer extends View {
    Paint paint;
    public static Bitmap bitmap;
    public static MyDrawer singelton;
    int sizeX = 1000, sizeY = 1000;

    public MyDrawer(Context context, AttributeSet attrs) {
        super(context, attrs);
        singelton = this;
        //  if (isInEditMode())
        //        return;
        Log.d("MAN", "man,here");
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Log.d("MAN", Integer.toString(size.x) + '-' + size.y);
        sizeX = size.x <= 0 ? 1000 : size.x;
        sizeY = size.y <= 0 ? 1000 : size.y;
        bitmap = Bitmap.createBitmap(sizeX, sizeY, Bitmap.Config.RGB_565);


        int[] colors = new int[10 * 15];
        Arrays.fill(colors, 0, 10 * 15, Color.GREEN);
        bitmap.setPixels(colors, 0, 10, 40, 40, 10, 15);

        Canvas canvas = new Canvas(bitmap);
        Paint p = new Paint();
        p.setColor(Color.BLUE);
        int x = 500;
        for (int i = 0; i < size.y; ++i) {
            bitmap.setPixel(x, i, Color.RED);
        }
        canvas.drawCircle(80, 80, 10, p);
    }

    //  int cycler = 0;
    //  long lastGlobalCycleId=0;
    @Override
    protected void onDraw(Canvas canvas) {
        /*if (Math.abs(lastGlobalCycleId-MainActivity.globalCycleId) < 2){
            return;
        }
        lastGlobalCycleId = MainActivity.globalCycleId;*/
        MainActivity.Debug("onDraw activated!");

        canvas.drawARGB(80, 102, 204, 255);
        mPosX = (mPosX + 1) % sizeX;
        canvas.drawBitmap(bitmap, mPosX - sizeX, 0, paint);
        canvas.drawBitmap(bitmap, mPosX, 0, paint);
        canvas.drawBitmap(bitmap, mPosX + sizeX, 0, paint);
    }
}

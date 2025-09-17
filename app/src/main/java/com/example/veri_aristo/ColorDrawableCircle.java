package com.example.veri_aristo;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

// A custom drawable that draws a circle with a specified color
public class ColorDrawableCircle extends Drawable {

    private final Paint paint;

    // Constructor that initializes the paint with the specified color
    public ColorDrawableCircle(int color) {
        paint = new Paint();
        paint.setColor(color);
        paint.setAntiAlias(true);
    }

    @Override
    // Draw the circle on the canvas
    public void draw(Canvas canvas) {
        int width = getBounds().right - getBounds().left;
        int height = getBounds().bottom - getBounds().top;
        int radius = Math.min(width, height) / 2;

        canvas.drawCircle(width / 2f, height / 2f, radius, paint);
    }

    @Override
    // Set the alpha value for the paint
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    // Set the color filter for the paint
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override
    // Return the opacity of the drawable
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}

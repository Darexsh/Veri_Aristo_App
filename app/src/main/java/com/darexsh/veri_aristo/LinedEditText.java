package com.darexsh.veri_aristo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatEditText;

// EditText that draws notebook-style horizontal lines behind text.
public class LinedEditText extends AppCompatEditText {

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect lineRect = new Rect();

    public LinedEditText(Context context) {
        super(context);
        init();
    }

    public LinedEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LinedEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2f);
        linePaint.setColor(0xFFD7CBB5);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int height = getHeight();
        int lineHeight = getLineHeight();
        int lineCount = Math.max(getLineCount(), 1);
        int totalLines = Math.max(lineCount, height / lineHeight);

        int lastLineBottom;
        getLineBounds(lineCount - 1, lineRect);
        lastLineBottom = lineRect.bottom;

        for (int i = 0; i < totalLines; i++) {
            int y;
            if (i < lineCount) {
                getLineBounds(i, lineRect);
                y = lineRect.bottom;
            } else {
                y = lastLineBottom + (i - lineCount + 1) * lineHeight;
            }
            canvas.drawLine(getPaddingLeft(), y, getWidth() - getPaddingRight(), y, linePaint);
        }

        super.onDraw(canvas);
    }
}

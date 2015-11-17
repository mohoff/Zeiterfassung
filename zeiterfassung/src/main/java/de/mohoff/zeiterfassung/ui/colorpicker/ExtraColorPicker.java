package de.mohoff.zeiterfassung.ui.colorpicker;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;

import de.mohoff.zeiterfassung.R;

/**
 * Created by moo on 11/15/15.
 */
public class ExtraColorPicker extends ColorPicker {
    int mExtraColor;

    public ExtraColorPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ExtraColorPicker,
                0, 0);

        try {
            mExtraColor = a.getColor(R.styleable.ExtraColorPicker_extraColor, 0);
        } finally {
            a.recycle();
        }
    }

    private Rect getExtraColorRect(int left, int top, int right, int bottom){
        rect.left = left;
        rect.top = top;
        rect.right = right;
        rect.bottom = bottom;
        return rect;
    }

    @Override
    protected void drawHorizontalPicker(Canvas canvas){
        paint.setColor(mExtraColor);
        getExtraColorRect(
                POPOUT_SIZE,
                POPOUT_SIZE,
                cellSize * 3 + POPOUT_SIZE,
                canvas.getHeight()/2 - 3*POPOUT_SIZE
        );

        // Draw extra color separately
        if(isColorSelected() && mSelectedIndex == -2){
            popoutRect.left = 0;
            popoutRect.top = 0;
            popoutRect.right = rect.right + POPOUT_SIZE;
            popoutRect.bottom = rect.bottom + POPOUT_SIZE;
            popoutPaint.setColor(mExtraColor);
            canvas.drawRect(popoutRect, popoutPaint);
        } else {
            canvas.drawRect(rect, paint);
        }

        // Draw color palette
        drawHorizontalPalette(canvas, mUsedPalette.length,
                getFirstPaletteRect(
                        0,
                        canvas.getHeight()/2 + 3*POPOUT_SIZE,
                        POPOUT_SIZE,
                        canvas.getHeight() - POPOUT_SIZE)
        );
    }

    @Override
    public int getSelectedColor() {
        if(isColorSelected() && mSelectedIndex == -2)
            return mExtraColor;
        else {
            return super.getSelectedColor();
        }
    }

    @Override
    public void setSelectedColor(int color) {
        if(mExtraColor == color){
            mSelectedIndex = -2;
        } else {
            super.setSelectedColor(color);
        }
    }


    @Override
    public boolean isColorSelected(){
        return mSelectedIndex == -2 || super.isColorSelected();
    }

    @Override
    protected int getFieldIndexAt(float x, float y) {
        if (mOrientation == HORIZONTAL) {
            if(y < (c.getHeight()/2 - 2*POPOUT_SIZE) && x < cellSize*3 + POPOUT_SIZE){
                return -2;
            }
            return (int)x/cellSize;
        } else {
            return (int)y/cellSize;
        }
    }
}

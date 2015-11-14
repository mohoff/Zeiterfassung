package de.mohoff.zeiterfassung.ui.components;

/**
 * Created by moo on 11/13/15.
 */
    import android.content.Context;
    import android.content.res.TypedArray;
    import android.graphics.Canvas;
    import android.graphics.Paint;
    import android.graphics.Paint.Style;
    import android.graphics.Rect;
    import android.os.Parcel;
    import android.os.Parcelable;
    import android.util.AttributeSet;
    import android.view.MotionEvent;
    import android.view.View;

    import de.mohoff.zeiterfassung.R;

public class ColorPicker extends View {

    public interface OnColorChangedListener {
        void onColorChanged(int c);
    }

    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;

    private static int POPOUT_SIZE = 8; // px

    private int mOrientation;
    private int mDefaultIndex, mSelectedIndex;
    private int[] mUsedPalette;

    private Paint paint, popoutPaint;
    private Rect rect = new Rect();
    private Rect popoutRect = new Rect();
    private OnColorChangedListener onColorChanged;

    // Width and height without padding
    private int pickerWidth, pickerHeight;
    // Width (for horizontal pickers) and height (for vertical pickers)
    // of a single color field.
    private int cellSize;

    private boolean isClick = false;

    /*{
        if (isInEditMode()) {
            colors = ColorPalette.ULTRALIGHT;
        } else {
            colors = new int[1];
        }
    }*/

    public ColorPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ColorPicker,
                0, 0);

        try {
            mOrientation = a.getInteger(R.styleable.ColorPicker_orientation, 0);
            int paletteIndex = a.getInteger(R.styleable.ColorPicker_usePalette, 0);
            if(paletteIndex == 0) mUsedPalette = ColorPalette.NORMAL;
            if(paletteIndex == 1) mUsedPalette = ColorPalette.LIGHT;
            if(paletteIndex == 2) mUsedPalette = ColorPalette.ULTRALIGHT;
            mDefaultIndex = a.getInteger(R.styleable.ColorPicker_defaultIndex, 0);
            if(mDefaultIndex == 1) mDefaultIndex = mUsedPalette.length-1;
            if(mDefaultIndex == 2) mDefaultIndex = -1;
            mSelectedIndex = mDefaultIndex;
        } finally {
            a.recycle();
        }

        paint = new Paint();
        popoutPaint = new Paint();
        paint.setStyle(Style.FILL);
        popoutPaint.setStyle(Style.FILL);

        recalcCellSize();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mOrientation == HORIZONTAL) {
            drawHorizontalPicker(canvas);
        } else {
            drawVerticalPicker(canvas);
        }
    }

    private void drawVerticalPicker(Canvas canvas) {
        rect.left = 0;
        rect.top = POPOUT_SIZE;
        rect.right = canvas.getWidth();
        rect.bottom = POPOUT_SIZE;

        for (int i=0; i<mUsedPalette.length; i++) {
            paint.setColor(mUsedPalette[i]);
            rect.top = rect.bottom;
            rect.bottom += cellSize;

            if(isColorSelected() &&
                    mUsedPalette[i] == mUsedPalette[mSelectedIndex]){
                popoutRect.left = 0;
                popoutRect.top = rect.top - POPOUT_SIZE;
                popoutRect.right = canvas.getWidth();
                popoutRect.bottom = rect.bottom + POPOUT_SIZE;
                popoutPaint.setColor(mUsedPalette[i]);
                continue;
            } else {
                rect.left = POPOUT_SIZE;
                rect.right = canvas.getWidth() - POPOUT_SIZE;
            }
            canvas.drawRect(rect, paint);
        }
        canvas.drawRect(popoutRect, popoutPaint);
    }

    private void drawHorizontalPicker(Canvas canvas) {
        rect.left = POPOUT_SIZE;
        rect.top = 0;
        rect.right = POPOUT_SIZE;
        rect.bottom = canvas.getHeight();

        for (int i=0; i<mUsedPalette.length; i++) {
            paint.setColor(mUsedPalette[i]);
            rect.left = rect.right;
            rect.right += cellSize;

            if(isColorSelected() &&
                    mUsedPalette[i] == mUsedPalette[mSelectedIndex]){
                popoutRect.left = rect.left - POPOUT_SIZE;
                popoutRect.top = 0;
                popoutRect.right = rect.right + POPOUT_SIZE;
                popoutRect.bottom = canvas.getHeight();
                popoutPaint.setColor(mUsedPalette[i]);
                continue;
            } else {
                rect.top = POPOUT_SIZE;
                rect.bottom = canvas.getHeight() - POPOUT_SIZE;
            }
            canvas.drawRect(rect, paint);
        }
        canvas.drawRect(popoutRect, popoutPaint);
    }

    private void onColorChanged(int color) {
        if (onColorChanged != null) {
            onColorChanged.onColorChanged(color);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int actionId = event.getAction();
        int newPaletteIndex;

        switch (actionId) {
            case MotionEvent.ACTION_DOWN:
                isClick = true;
                break;
            case MotionEvent.ACTION_UP:
                newPaletteIndex = getPaletteIndexAt(event.getX(), event.getY());
                setSelectedIndex(newPaletteIndex);
                if(isClick) super.performClick();
                break;
            case MotionEvent.ACTION_MOVE:
                newPaletteIndex = getPaletteIndexAt(event.getX(), event.getY());
                setSelectedIndex(newPaletteIndex);
                break;
            case MotionEvent.ACTION_CANCEL:
                isClick = false;
                break;
            case MotionEvent.ACTION_OUTSIDE:
                isClick = false;
                break;
            default:
                break;
        }
        return true;
    }

    private int getPaletteIndexAt(float x, float y) {
        if (mOrientation == HORIZONTAL) {
            return (int)x/cellSize;
        } else {
            return (int)y/cellSize;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        pickerWidth = w - (getPaddingLeft() + getPaddingRight());
        pickerHeight = h - (getPaddingTop() + getPaddingBottom());

        recalcCellSize();
        super.onSizeChanged(w, h, oldw, oldh);
    }

    private void recalcCellSize() {
        if (mOrientation == HORIZONTAL) {
            cellSize = (pickerWidth-(2*POPOUT_SIZE)) / mUsedPalette.length;
        } else {
            cellSize = (pickerHeight-(2*POPOUT_SIZE)) / mUsedPalette.length;
        }
    }

    public int getSelectedColor() {
        if(isColorSelected())
            return mUsedPalette[mSelectedIndex];
        else {
            return -1;
        }
    }

    public int getIndexForColor(int color){
        for(int i=0; i<mUsedPalette.length; i++){
            if(mUsedPalette[i] == color){
                return i;
            }
        }
        return -1;
    }

    public boolean isColorSelected(){
        if(mSelectedIndex >= 0 && mSelectedIndex > mUsedPalette.length){
            return true;
        }
        return false;
    }

    public void setSelectedIndex(int index) {
        if(mSelectedIndex != index){
            mSelectedIndex = index;
            invalidate();
            onColorChanged(getSelectedColor());
        }
    }

    public void setOnColorChangedListener(OnColorChangedListener l) {
        this.onColorChanged = l;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        // begin boilerplate code that allows parent classes to save state
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);

        ss.selectedColor = getSelectedColor();

        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        // begin boilerplate code so parent classes can restore state
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        this.mSelectedIndex = getIndexForColor(ss.selectedColor);
    }

    static class SavedState extends BaseSavedState {
        int selectedColor;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.selectedColor = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(this.selectedColor);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}



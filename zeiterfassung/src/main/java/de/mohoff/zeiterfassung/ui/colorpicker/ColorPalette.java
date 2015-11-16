package de.mohoff.zeiterfassung.ui.colorpicker;

import android.graphics.Color;

/**
 * Created by moo on 11/13/15.
 */
public class ColorPalette {
    public static int[] ULTRALIGHT;
    public static int[] LIGHT;
    public static int[] NORMAL;


    public static int getIndexForColor(int[] palette, int color){
        for(int i=0; i<palette.length; i++){
            if(palette[i] == color) return i;
        }
        return -1;
    }

    static {
        ULTRALIGHT = new int[] {
                Color.parseColor("#FFEBEE"),    // red
                Color.parseColor("#FCE4EC"),    // pink
                Color.parseColor("#F3E5F5"),    // purple
                Color.parseColor("#EDE7F6"),    // deep purple
                Color.parseColor("#E8EAF6"),    // indigo
                Color.parseColor("#E3F2FD"),    // blue
                Color.parseColor("#E1F5FE"),    // light blue
                Color.parseColor("#E0F7FA"),    // cyan
                Color.parseColor("#E0F2F1"),    // teal
                Color.parseColor("#E8F5E9"),    // green
                Color.parseColor("#F1F8E9"),    // light green
                Color.parseColor("#F9FBE7"),    // lime
                Color.parseColor("#FFFDE7"),    // yellow
                Color.parseColor("#FFF8E1"),    // amber
                Color.parseColor("#FFF3E0"),    // orange
                Color.parseColor("#FBE9E7"),    // deep orange
                Color.parseColor("#EFEBE9"),    // brown
        };

        LIGHT = new int[] {
                Color.parseColor("#FFCDD2"),    // red
                Color.parseColor("#F8BBD0"),    // pink
                Color.parseColor("#E1BEE7"),    // purple
                Color.parseColor("#D1C4E9"),    // deep purple
                Color.parseColor("#C5CAE9"),    // indigo
                Color.parseColor("#BBDEFB"),    // blue
                Color.parseColor("#B3E5FC"),    // light blue
                Color.parseColor("#B2EBF2"),    // cyan
                Color.parseColor("#B2DFDB"),    // teal
                Color.parseColor("#C8E6C9"),    // green
                Color.parseColor("#DCEDC8"),    // light green
                Color.parseColor("#F0F4C3"),    // lime
                Color.parseColor("#FFF9C4"),    // yellow
                Color.parseColor("#FFECB3"),    // amber
                Color.parseColor("#FFE0B2"),    // orange
                Color.parseColor("#FFCCBC"),    // deep orange
                Color.parseColor("#D7CCC8"),    // brown
        };

        NORMAL = new int[] {
                Color.parseColor("#F44336"),    // red
                Color.parseColor("#E91E63"),    // pink
                Color.parseColor("#9C27B0"),    // purple
                Color.parseColor("#673AB7"),    // deep purple
                Color.parseColor("#3F51B5"),    // indigo
                Color.parseColor("#2196F3"),    // blue
                Color.parseColor("#03A9F4"),    // light blue
                Color.parseColor("#00BCD4"),    // cyan
                Color.parseColor("#009688"),    // teal
                Color.parseColor("#4CAF50"),    // green
                Color.parseColor("#8BC34A"),    // light green
                Color.parseColor("#CDDC39"),    // lime
                Color.parseColor("#FFEB3B"),    // yellow
                Color.parseColor("#FFC107"),    // amber
                Color.parseColor("#FF9800"),    // orange
                Color.parseColor("#FF5722"),    // deep orange
                Color.parseColor("#795548"),    // brown
        };

    }
}


package de.mohoff.zeiterfassung.ui.intro;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import de.mohoff.zeiterfassung.R;

public class IntroSlide extends Fragment {
    private static final String ARG_LAYOUT_RES_ID = "layoutResId";
    private int layoutResId;
    private String mTitle, mSubtitle, mDescription;
    private Drawable mSlideIcon;

    public IntroSlide() {
        // Required empty public constructor
    }

    public static IntroSlide newInstance(int layoutResId) {
        IntroSlide slide = new IntroSlide();
        Bundle args = new Bundle();
        args.putInt(ARG_LAYOUT_RES_ID, layoutResId);
        slide.setArguments(args);

        return slide;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null && getArguments().containsKey(ARG_LAYOUT_RES_ID)) {
            layoutResId = getArguments().getInt(ARG_LAYOUT_RES_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(layoutResId, container, false);

        ((TextView) v.findViewById(R.id.title)).setText(Html.fromHtml(mTitle));
        if(mSubtitle != null && !mSubtitle.equals("") && !mSubtitle.equals(" ")){
            ((TextView) v.findViewById(R.id.subtitle)).setText(Html.fromHtml(mSubtitle));
        }
        ((ImageView) v.findViewById(R.id.slideIcon)).setImageDrawable(mSlideIcon);
        ((TextView) v.findViewById(R.id.description)).setText(Html.fromHtml(mDescription));

        return v;
    }

    public void setTitle(String title){
        mTitle = title;
    }

    public void setSubitle(String subtitle){
        mSubtitle = subtitle;
    }

    public void setImage(Drawable slideIcon){
        mSlideIcon = slideIcon;
    }

    public void setDescription(String description){
        mDescription = description;
    }
}

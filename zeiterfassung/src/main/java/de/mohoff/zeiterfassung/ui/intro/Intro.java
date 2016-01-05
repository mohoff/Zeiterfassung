package de.mohoff.zeiterfassung.ui.intro;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import com.github.paolorotolo.appintro.AppIntro;

import de.mohoff.zeiterfassung.R;

public class Intro extends AppIntro {

    /*@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
    }*/

    // Please DO NOT override onCreate. Use init.
    @Override
    public void init(Bundle savedInstanceState) {
        showStatusBar(false);
        // Set bottom bar color
        setBarColor(getResources().getColor(R.color.greenish));
        // set separator bar color between bottom bar and main pane
        setSeparatorColor(getResources().getColor(R.color.white));
        //setNavBarColor(getResources().getColor(R.color.white_90));

        // Hide Skip/Done button.
        showSkipButton(false);
        setProgressButtonEnabled(true);
        setFadeAnimation();

        // Create intro slides
        Resources r = getResources();
        IntroSlide slide1 = populateSlide(
                IntroSlide.newInstance(R.layout.intro_welcome),
                r.getString(R.string.welcome_1_title),
                r.getString(R.string.welcome_1_subtitle),
                r.getDrawable(R.mipmap.ic_launcher),
                r.getString(R.string.welcome_1_description)
        );
        IntroSlide slide2 = populateSlide(
                IntroSlide.newInstance(R.layout.intro_regular),
                r.getString(R.string.gettingstarted_2_title),
                r.getString(R.string.gettingstarted_2_subtitle),
                r.getDrawable(R.drawable.ic_zones),
                r.getString(R.string.gettingstarted_2_description)
        );
        IntroSlide slide3 = populateSlide(
                IntroSlide.newInstance(R.layout.intro_regular),
                r.getString(R.string.gettingstarted_3_title),
                r.getString(R.string.gettingstarted_3_subtitle),
                r.getDrawable(R.drawable.ic_map),
                r.getString(R.string.gettingstarted_3_description)
        );
        IntroSlide slide4 = populateSlide(
                IntroSlide.newInstance(R.layout.intro_regular),
                r.getString(R.string.enjoy_4_title),
                r.getString(R.string.enjoy_4_subtitle),
                r.getDrawable(R.drawable.ic_location_on_black_24dp),
                r.getString(R.string.enjoy_4_description)
        );

        // Add intro slides
        addSlide(slide1);
        addSlide(slide2);
        addSlide(slide3);
        addSlide(slide4);

        // Should be enough to ask for permissions when LocationService is started the first time.
        /*askForPermissions(
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                1 // 1 = shows right before showing 2nd slide.
        );*/
    }

    private IntroSlide populateSlide(IntroSlide s, String title, String subtitle, Drawable slideIcon, String description){
        s.setTitle(title);
        s.setSubitle(subtitle);
        s.setImage(slideIcon);
        s.setDescription(description);
        return s;
    }

    @Override
    public void onSkipPressed() {
        // Do something when users tap on Skip button.
    }

    @Override
    public void onDonePressed() {
        // Do something when users tap on Done button.
        finish();
    }

    @Override
    public void onSlideChanged() {
        // Do something when the slide changes.
    }

    @Override
    public void onNextPressed() {
        // Do something when users tap on Next button.
    }
}

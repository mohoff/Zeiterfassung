package de.mohoff.zeiterfassung.ui.intro;

import android.Manifest;
import android.graphics.Color;
import android.os.Bundle;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;

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
        // Add your slide's fragments here.
        // AppIntro will automatically generate the dots indicator and buttons.
        /*addSlide(first_fragment);
        addSlide(second_fragment);
        addSlide(third_fragment);
        addSlide(fourth_fragment);*/
        showStatusBar(false);
        // Instead of fragments, you can also use our default slide
        // Just set a title, description, background and image. AppIntro will do the rest.
        addSlide(IntroSlide.newInstance(R.layout.intro_1_welcome));
        addSlide(IntroSlide.newInstance(R.layout.intro_2_gettingstarted1));
        addSlide(IntroSlide.newInstance(R.layout.intro_3_gettingstarted2));
        addSlide(IntroSlide.newInstance(R.layout.intro_4_enjoy));


        /*addSlide(AppIntroFragment.newInstance(
                "Intro",
                "Zeiterfassung let\'s you track your whereabouts in the background. You define your Zones which you want to have captured and start the background service. As a result, you can examine your location history and enable useful statistics. And you don\'t even need GPS for that!",
                R.mipmap.ic_launcher,
                getResources().getColor(R.color.greenish))
        );
        addSlide(AppIntroFragment.newInstance(
                        "Define Zones",
                        "At first you should define the Zones for which you want to track your movements.",
                        R.drawable.ic_zones,
                        getResources().getColor(R.color.greenish))
        );
        */

        // Should be enough to ask for permissions when LocationService is started the first time.
        /*askForPermissions(
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                1 // 1 = shows right before showing 2nd slide.
        );*/


        // Set bottom bar color
        setBarColor(getResources().getColor(R.color.greenish));
        // set separator bar color between bottom bar and main pane
        setSeparatorColor(getResources().getColor(R.color.white));
        //setNavBarColor(getResources().getColor(R.color.white_90));

        // Hide Skip/Done button.
        showSkipButton(false);
        setProgressButtonEnabled(true);
        setFadeAnimation();
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

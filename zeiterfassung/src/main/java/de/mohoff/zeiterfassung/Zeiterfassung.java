package de.mohoff.zeiterfassung;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

/**
 * Created by moo on 11/19/15.
 */
@ReportsCrashes(
        formUri = "http://www.mohoff.de/acra.php",
        //formUriBasicAuthLogin = "your username", // optional
        //formUriBasicAuthPassword = "your password",  // optional
        reportType = org.acra.sender.HttpSender.Type.JSON
)
public class Zeiterfassung extends MultiDexApplication {
    @Override
    public void onCreate() {
        ACRA.init(this);
        super.onCreate();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}

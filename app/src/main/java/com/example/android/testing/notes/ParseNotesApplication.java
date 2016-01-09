package com.example.android.testing.notes;

import android.app.Application;

import com.example.android.testing.notes.data.Note;
import com.parse.Parse;
import com.parse.ParseObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import timber.log.Timber;

/**
 * Created by luke on 31/12/15.
 */
public class ParseNotesApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        ParseObject.registerSubclass(Note.class);
        Parse.enableLocalDatastore(this);

        try {
            InputStream is = getAssets().open("server.properties");
            Properties properties = new Properties();
            properties.load(is);
            String appId = properties.getProperty("applicationId");
            String clientId = properties.getProperty("clientId");

            Parse.initialize(this, appId, clientId);
        } catch (IOException e) {
            Timber.d("Could not open properties file...");
        }

        if(BuildConfig.DEBUG) {
            //Debug logging
            Timber.plant(new Timber.DebugTree());
        }
    }

}

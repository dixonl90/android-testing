package com.example.android.testing.notes;

import android.app.Application;

import com.example.android.testing.notes.data.Note;
import com.parse.Parse;
import com.parse.ParseObject;

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
        Parse.initialize(this, "pO9hNIcVPRkG7bhyjiGJ1a5uXISKGYRJNvFaUGEl", "cX0sr60R60xcHojOzRjpTwhEjTSVYOIzCvkjT9mh");


        if(BuildConfig.DEBUG) {
            //Debug logging
            Timber.plant(new Timber.DebugTree());
        }
    }

}

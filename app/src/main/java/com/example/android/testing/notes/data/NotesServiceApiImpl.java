/*
 * Copyright 2015, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.testing.notes.data;

import android.os.Handler;
import android.support.v4.util.ArrayMap;

import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Implementation of the Notes Service API that adds a latency simulating network.
 */
public class NotesServiceApiImpl implements NotesServiceApi {

    @Override
    public void getAllNotes(final NotesServiceCallback callback) {
        ParseQuery<Note> query = ParseQuery.getQuery(Note.class);
        query.setCachePolicy(ParseQuery.CachePolicy.NETWORK_ELSE_CACHE);
        query.findInBackground(new FindCallback<Note>() {
            @Override
            public void done(List<Note> notes, ParseException e) {
                if(e == null) {
                    callback.onLoaded(notes);
                }
                else {
                    Timber.e(e.getMessage());
                }
            }
        });
    }

    @Override
    public void getNote(final String noteId, final NotesServiceCallback callback) {
        ParseQuery<Note> query = ParseQuery.getQuery(Note.class);
        query.setCachePolicy(ParseQuery.CachePolicy.NETWORK_ELSE_CACHE);
        query.getInBackground(noteId, new GetCallback<Note>() {
            @Override
            public void done(Note note, ParseException e) {
                if(e == null) {
                    callback.onLoaded(note);
                }
                else {
                    Timber.e(e.getMessage());
                }
            }
        });
    }

    @Override
    public void saveNote(final Note note) {
        note.saveEventually(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(e == null) {

                }
                else {
                    Timber.e("Could not save '%s' to Parse! %s", note.getTitle(), e.getMessage());
                }
            }
        });
    }

    @Override
    public void deleteNote(final Note note) {
        note.deleteEventually(new DeleteCallback() {
            @Override
            public void done(ParseException e) {
                if(e == null) {
                    Timber.d("Deleted %s", note.getTitle());
                }
                else {
                    Timber.e(e.getMessage());
                }
            }
        });
    }
}

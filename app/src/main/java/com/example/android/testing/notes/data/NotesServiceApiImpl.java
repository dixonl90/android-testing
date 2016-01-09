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

import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import java.util.List;

import timber.log.Timber;

/**
 * Implementation of the Notes Service API that adds a latency simulating network.
 */
public class NotesServiceApiImpl implements NotesServiceApi {

    private static String ALL_NOTES = "ALL_NOTES";

    @Override
    public void getAllNotes(final NotesServiceCallback callback) {
        ParseQuery<Note> query = ParseQuery.getQuery(Note.class);
        query.fromPin(ALL_NOTES);
        query.orderByDescending(Note.CREATED);
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
        query.fromPin(ALL_NOTES);
        query.whereEqualTo(Note.ID, noteId);
        query.getFirstInBackground(new GetCallback<Note>() {
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

        note.pinInBackground(ALL_NOTES, new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(e == null) {
                    Timber.d("Pinned note: %s", note.getObjectId());
                }
                else {
                    Timber.e("Could not pin '%s' %s", note.getTitle(), e.getMessage());
                }
            }
        });
    }

    @Override
    public void uploadExistingNotes(final UploadDataCallback callback) {
        ParseQuery<Note> query = ParseQuery.getQuery(Note.class);
        query.fromPin(ALL_NOTES);
        query.whereEqualTo(Note.HAS_UPLOADED, false);
        query.findInBackground(new FindCallback<Note>() {
            @Override
            public void done(List<Note> notes, ParseException e) {
                if(e == null) {
                    Timber.d("Found %d notes to upload!", notes.size());
                    for (final Note note : notes) {
                        note.setUploaded(true);
                        note.saveInBackground(new SaveCallback() {
                            @Override
                            public void done(ParseException e) {
                                if(e == null) {
                                    Timber.d("Uploaded note: %s", note.getObjectId());
                                }
                                else {
                                    note.setUploaded(false);
                                    Timber.e("Could not save '%s' to Parse! %s", note.getTitle(), e.getMessage());
                                }
                            }
                        });
                    }
                    callback.onDataUploaded();
                }
                else {
                    Timber.e(e.getMessage());
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

    @Override
    public void refreshData(final NotesServiceCallback<List<Note>> callback) {
        ParseQuery<Note> query = ParseQuery.getQuery(Note.class);
        query.findInBackground(new FindCallback<Note>() {
            @Override
            public void done(final List<Note> notes, ParseException e) {
                if(e == null) {
                    for(final Note note : notes) {
                        note.setUploaded(true);
                        note.pinInBackground(ALL_NOTES, new SaveCallback() {
                            @Override
                            public void done(ParseException e) {
                                if(e == null) {
                                    Timber.d("Pinned note %s - %s", note.getId(), note.getTitle());
                                }
                                else {
                                    Timber.e("Could not pin note: %s - %s", note.getId(), e.getMessage());
                                    note.setUploaded(false);
                                }
                            }
                        });
                    }

                    getAllNotes(new NotesServiceCallback() {
                        @Override
                        public void onLoaded(Object notes) {
                            callback.onLoaded((List<Note>) notes);
                        }

                        @Override
                        public void onError(String message) {

                        }
                    });
                }
                else {
                    Timber.e(e.getMessage());
                }
            }
        });
    }

}

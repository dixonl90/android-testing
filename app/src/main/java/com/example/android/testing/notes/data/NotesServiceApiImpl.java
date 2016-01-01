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

import java.sql.Time;
import java.util.List;

import timber.log.Timber;

public class NotesServiceApiImpl implements NotesServiceApi {

    private static final String NOTES_PIN = "notes";

    @Override
    public void getAllNotes(final NotesServiceCallback callback) {

        ParseQuery<Note> query = ParseQuery.getQuery(Note.class);
        query.fromPin(NOTES_PIN);
        query.findInBackground(new FindCallback<Note>() {
            @Override
            public void done(List<Note> notes, ParseException e) {
                if(e == null) {
                    Timber.d("Found %d notes in cache!", notes.size());
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
        query.fromPin(NOTES_PIN);
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
        note.pinInBackground(NOTES_PIN, new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(e == null) {
                    Timber.d("Pinned note: %s", note.getTitle());
                }
                else {
                    Timber.e("Error pinning note: %s", note.getObjectId());
                }
            }
        });
    }


    @Override
    public void refreshData(final RefreshDataCallback callback) {
        ParseQuery<Note> query = ParseQuery.getQuery(Note.class);
        query.findInBackground(new FindCallback<Note>() {
            public void done(final List<Note> notes, ParseException e) {
                if (e == null) {
                    ParseObject.unpinAllInBackground(new DeleteCallback() {
                        @Override
                        public void done(ParseException e) {
                            if(e == null) {
                                //Save all the notes
                                ParseObject.pinAllInBackground(NOTES_PIN, notes, new SaveCallback() {
                                    public void done(ParseException e) {
                                        if (e == null) {
                                            Timber.d("Found %d notes!", notes.size());
                                            callback.onDataRefreshed();
                                        } else {
                                            Timber.e("Error pinning notes: %s", e.getMessage());
                                        }
                                    }
                                });
                            }
                            else {
                                Timber.e("Error clearing cache: %s", e.getMessage());
                            }
                        }
                    });
                } else {
                    Timber.e("Error finding pinned notes: %s", e.getMessage());
                }
            }
        });
    }

    @Override
    public void uploadExistingNotes(final UploadDataCallback callback) {
        Timber.d("Looking for notes to upload...");

        ParseQuery<Note> query = new ParseQuery<Note>(Note.class);
        query.fromPin(NOTES_PIN);
        query.whereEqualTo(Note.HAS_UPLOADED, false);
        query.findInBackground(new FindCallback<Note>() {
            @Override
            public void done(List<Note> notes, ParseException findEx) {
                if(findEx == null) {
                    Timber.d("Found %d notes to upload.", notes.size());
                    for (final Note note : notes) {
                        Timber.d("Trying to upload %s", note.getTitle());
                        note.setUploaded(true);
                        note.saveInBackground(new SaveCallback() {
                            @Override
                            public void done(ParseException saveEx) {
                                if(saveEx == null) {
                                    Timber.d("Uploaded %s %s", note.getTitle(), note.getObjectId());
                                    callback.onDataUploaded();
                                    //TODO: We should only update the row we changed, not everything...
                                }
                                else {
                                    note.setUploaded(false);
                                    Timber.e("Error trying to upload note:%s - %s",
                                            note.getTitle(),
                                            saveEx.getMessage());
                                }
                            }
                        });
                    }
                }
                else {
                    Timber.e("Error trying to find notes to upload. %s",
                            findEx.getMessage());
                }
            }
        });
    }

}

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

package com.example.android.testing.notes.notes;

import com.example.android.testing.notes.data.Note;
import com.example.android.testing.notes.data.NotesRepository;
import com.example.android.testing.notes.util.EspressoIdlingResource;
import com.parse.ParseQuery;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;

import java.util.List;

import timber.log.Timber;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Listens to user actions from the UI ({@link NotesFragment}), retrieves the data and updates the
 * UI as required.
 */
public class NotesPresenter implements NotesContract.UserActionsListener {

    private final NotesRepository mNotesRepository;
    private final NotesContract.View mNotesView;

    public NotesPresenter(
            @NonNull NotesRepository notesRepository, @NonNull NotesContract.View notesView) {
        mNotesRepository = checkNotNull(notesRepository, "notesRepository cannot be null");
        mNotesView = checkNotNull(notesView, "notesView cannot be null!");
    }

    @Override
    public void loadNotes(boolean forceUpdate) {


            mNotesView.setProgressIndicator(true);

            Timber.d("Loading notes...");

            if (forceUpdate && mNotesView.isNetworkAvailable()) {
                Timber.d("Force update requested");
                mNotesRepository.refreshData(new NotesRepository.RefreshDataCallback() {
                    @Override
                    public void onNotesRefreshed() {
                        Timber.d("Force update complete");
                        getNotes();
                    }
                });
            }
            else
                getNotes();
        }
//        else {
//            //We're offline!
//            mNotesView.setProgressIndicator(false);
//            Timber.w("Offline! Don't refresh!");
//
//        }
//    }

    private void getNotes() {
        // The network request might be handled in a different thread so make sure Espresso knows
        // that the app is busy until the response is handled.
        EspressoIdlingResource.increment(); // App is busy until further notice

        Timber.d("Loading from cache");

        mNotesRepository.getNotes(new NotesRepository.LoadNotesCallback() {
            @Override
            public void onNotesLoaded(List<Note> notes) {
                Timber.d("Loaded from cache");
                EspressoIdlingResource.decrement(); // Set app as idle.
                mNotesView.setProgressIndicator(false);
                mNotesView.showNotes(notes);
            }

            @Override
            public void onError(String message) {
                EspressoIdlingResource.decrement(); // Set app as idle.
                mNotesView.setProgressIndicator(false);

            }
        });
    }

    @Override
    public void addNewNote() {
        mNotesView.showAddNote();
    }

    @Override
    public void openNoteDetails(@NonNull Note requestedNote) {
        checkNotNull(requestedNote, "requestedNote cannot be null!");
        mNotesView.showNoteDetailUi(requestedNote.getId());
    }

    public void uploadExisitingNotes() {
        mNotesRepository.uploadExistingNotes(new NotesRepository.UploadDataCallback() {
            @Override
            public void onNotesUploaded() {

            }
        });
    }

}

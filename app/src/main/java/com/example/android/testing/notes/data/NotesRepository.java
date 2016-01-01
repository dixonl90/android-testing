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

import android.support.annotation.NonNull;

import java.util.List;

/**
 * Main entry point for accessing notes data.
 */
public interface NotesRepository {

    interface LoadNotesCallback {

        void onNotesLoaded(List<Note> notes);

        void onError(String message);
    }

    interface GetNoteCallback {

        void onNoteLoaded(Note note);

        void onError(String message);
    }

    interface RefreshDataCallback {

        void onNotesRefreshed();

    }

    interface UploadDataCallback {

        void onNotesUploaded();

    }

    void getNotes(@NonNull LoadNotesCallback callback);

    void getNote(@NonNull String noteId, @NonNull GetNoteCallback callback);

    void saveNote(@NonNull Note note);

    void refreshData(@NonNull RefreshDataCallback callback);

    void uploadExistingNotes(@NonNull UploadDataCallback callback);

}

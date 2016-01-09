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

import com.example.android.testing.notes.Injection;
import com.example.android.testing.notes.addnote.AddNoteActivity;
import com.example.android.testing.notes.notedetail.NoteDetailActivity;
import com.example.android.testing.notes.R;
import com.example.android.testing.notes.data.Note;
import com.parse.DeleteCallback;
import com.parse.ParseException;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.support.v7.widget.helper.ItemTouchHelper.SimpleCallback;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Display a grid of {@link Note}s
 */
public class NotesFragment extends Fragment implements NotesContract.View {

    private static final int REQUEST_ADD_NOTE = 1;

    private NotesContract.UserActionsListener mActionsListener;

    private NotesAdapter mListAdapter;

    public NotesFragment() {
        // Requires empty public constructor
    }

    public static NotesFragment newInstance() {
        return new NotesFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mListAdapter = new NotesAdapter(new ArrayList<Note>(0), mItemListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        mActionsListener.loadNotes(false);
        mActionsListener.uploadExisitingNotes();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setRetainInstance(true);

        mActionsListener = new NotesPresenter(Injection.provideNotesRepository(), this);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // If a note was successfully added, show snackbar
        if (REQUEST_ADD_NOTE == requestCode && Activity.RESULT_OK == resultCode) {
            Snackbar.make(getView(), getString(R.string.successfully_saved_note_message),
                    Snackbar.LENGTH_SHORT).show();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_notes, container, false);
        final RecyclerView recyclerView = (RecyclerView) root.findViewById(R.id.notes_list);
        recyclerView.setAdapter(mListAdapter);


        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), null));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT|ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {

                final View undo = viewHolder.itemView.findViewById(R.id.undo_layout);
                mListAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
                mListAdapter.mNotesToRemove.add(mListAdapter.mNotes.get(viewHolder.getAdapterPosition()));


            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);

        itemTouchHelper.attachToRecyclerView(recyclerView);

        // Set up floating action button
        FloatingActionButton fab =
                (FloatingActionButton) getActivity().findViewById(R.id.fab_add_notes);

        fab.setImageResource(R.drawable.ic_add);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActionsListener.addNewNote();
            }
        });

        // Pull-to-refresh
        SwipeRefreshLayout swipeRefreshLayout =
                (SwipeRefreshLayout) root.findViewById(R.id.refresh_layout);
        swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(getActivity(), R.color.colorPrimary),
                ContextCompat.getColor(getActivity(), R.color.colorAccent),
                ContextCompat.getColor(getActivity(), R.color.colorPrimaryDark));
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mActionsListener.loadNotes(true);
                mActionsListener.uploadExisitingNotes();
            }
        });
        return root;
    }

    /**
     * Listener for clicks on notes in the RecyclerView.
     */
    NoteItemListener mItemListener = new NoteItemListener() {
        @Override
        public void onNoteClick(Note clickedNote) {
            mActionsListener.openNoteDetails(clickedNote);
        }

        @Override
        public void onItemRemove(Note removedNote) {
            Timber.d("We need to remove the item now!");
            mActionsListener.deleteNote(removedNote);
        }
    };

    @Override
    public void setProgressIndicator(final boolean active) {

        if (getView() == null) {
            return;
        }
        final SwipeRefreshLayout srl =
                (SwipeRefreshLayout) getView().findViewById(R.id.refresh_layout);

        // Make sure setRefreshing() is called after the layout is done with everything else.
        srl.post(new Runnable() {
            @Override
            public void run() {
                srl.setRefreshing(active);
            }
        });
    }

    @Override
    public void showNotes(List<Note> notes) {
        mListAdapter.replaceData(notes);
    }

    @Override
    public void showAddNote() {
        Intent intent = new Intent(getContext(), AddNoteActivity.class);
        startActivityForResult(intent, REQUEST_ADD_NOTE);
    }

    @Override
    public void showNoteDetailUi(String noteId) {
        // in it's own Activity, since it makes more sense that way and it gives us the flexibility
        // to show some Intent stubbing.
        Intent intent = new Intent(getContext(), NoteDetailActivity.class);
        intent.putExtra(NoteDetailActivity.EXTRA_NOTE_ID, noteId);
        startActivity(intent);
    }


    private static class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.ViewHolder> {

        private List<Note> mNotes;
        private NoteItemListener mItemListener;
        private SimpleDateFormat simpleDateFormat;
        public List<Note> mNotesToRemove;

        public NotesAdapter(List<Note> notes, NoteItemListener itemListener) {
            setList(notes);
            mItemListener = itemListener;
            mNotesToRemove = new ArrayList<>();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);
            View noteView = inflater.inflate(R.layout.item_note, parent, false);

            simpleDateFormat = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");

            return new ViewHolder(noteView, mItemListener);
        }

        @Override
        public void onBindViewHolder(final ViewHolder viewHolder, final int position) {
            final Note note = mNotes.get(position);
            final View undo = viewHolder.itemView.findViewById(R.id.undo_layout);

            viewHolder.title.setText(note.getTitle());
            viewHolder.createdDate.setText(simpleDateFormat.format(note.getCreated()));

            if(!note.hasUploaded()) {
                viewHolder.title.setTextColor(Color.parseColor("#f51800"));
            }
            else {
                viewHolder.title.setTextColor(Color.BLACK);
            }

            if (undo != null) {

                if(mNotesToRemove.contains(note)) {
                    undo.setVisibility(View.VISIBLE);

                    final Runnable delayedDelete = new Runnable() {
                        public void run() {
                            if (undo.isShown()) {
                                Timber.d("Delete %d after timeout", viewHolder.getAdapterPosition());
                                removeNote(note, position);
                            }
                        }
                    };

                    TextView button = (TextView) viewHolder.itemView.findViewById(R.id.note_detail_undo_button);
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            notifyItemChanged(viewHolder.getAdapterPosition());
                            undo.setVisibility(View.GONE);
                            Timber.d("Undo removal! Position: %d", viewHolder.getAdapterPosition());
                            mNotesToRemove.remove(note);
                            undo.removeCallbacks(delayedDelete);
                        }
                    });

                    undo.postDelayed(delayedDelete, 5000); //5 second delay
                }
                else
                    undo.setVisibility(View.GONE);
            }
        }

        private void removeNote(final Note note, final int position) {
            mNotes.remove(note);
            notifyItemRemoved(position);
            mItemListener.onItemRemove(note);
        }

        public void replaceData(List<Note> notes) {
            setList(notes);
            notifyDataSetChanged();
        }

        private void setList(List<Note> notes) {
            mNotes = checkNotNull(notes);
        }

        @Override
        public int getItemCount() {
            return mNotes.size();
        }

        public Note getItem(int position) {
            return mNotes.get(position);
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            public TextView title;
            public TextView createdDate;

            private NoteItemListener mItemListener;

            public ViewHolder(View itemView, NoteItemListener listener) {
                super(itemView);
                mItemListener = listener;
                title = (TextView) itemView.findViewById(R.id.note_detail_title);
                createdDate = (TextView) itemView.findViewById(R.id.note_detail_created_date);

                itemView.findViewById(R.id.note_row).setOnClickListener(this);

                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                int position = getAdapterPosition();
                Note note = getItem(position);
                mItemListener.onNoteClick(note);

            }
        }
    }

    public interface NoteItemListener {

        void onNoteClick(Note clickedNote);

        void onItemRemove(Note removedNote);
    }

    @Override
    public void showError(String errorMessage) {
        Snackbar.make(getView(), "Error: " + errorMessage, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void refreshList() {
        mListAdapter.notifyDataSetChanged();
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isAvailable()
                && cm.getActiveNetworkInfo().isConnected();
    }

    @Override
    public void showOfflineMessage(String message) {
        Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
    }

}

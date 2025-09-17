package com.example.veri_aristo;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;

// NotesFragment allows users to create and save personal notes
public class NotesFragment extends Fragment {

    private EditText editTextNotes;
    private MaterialButton btnSave;

    private static final String PREF_NAME = "notes_prefs";
    private static final String NOTES_KEY = "user_notes";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notes, container, false);

        editTextNotes = view.findViewById(R.id.editText_notes);
        btnSave = view.findViewById(R.id.btn_save_notes);

        // Load saved notes when the fragment is created
        loadNotes();

        // Set up the save button to store notes in SharedPreferences
        btnSave.setOnClickListener(v -> saveNotes());

        return view;
    }

    // Load saved notes from SharedPreferences
    private void loadNotes() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String savedNotes = prefs.getString(NOTES_KEY, "");
        editTextNotes.setText(savedNotes);
    }

    // Save notes to SharedPreferences
    private void saveNotes() {
        String notes = editTextNotes.getText().toString().trim();
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(NOTES_KEY, notes).apply();

        Toast toast = Toast.makeText(requireContext(), "Notizen gespeichert", Toast.LENGTH_SHORT);
        toast.show();

        // Automatically dismiss the toast after a short duration
        new android.os.Handler().postDelayed(toast::cancel, 1000);
    }
}
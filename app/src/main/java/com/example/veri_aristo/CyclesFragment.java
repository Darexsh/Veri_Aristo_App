package com.example.veri_aristo;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

// CyclesFragment displays a history of cycle events (insertions/removals) in a card format
public class CyclesFragment extends Fragment {

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_CYCLE_HISTORY = "cycle_history";

    public CyclesFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cycles, container, false);

        // Initialize UI components
        LinearLayout cycleContainer = view.findViewById(R.id.cycle_container);
        Button clearHistoryButton = view.findViewById(R.id.btn_clear_history);

        // Load and display cycle history
        displayCycleHistory(cycleContainer);

        // Set up clear history button
        clearHistoryButton.setOnClickListener(v -> {
            clearCycleHistory();                    // Clear the cycle history from SharedPreferences
            cycleContainer.removeAllViews();        // Clear the UI
            displayCycleHistory(cycleContainer);    // Reload empty history
        });

        return view;
    }

    // Display the cycle history in the provided LinearLayout
    private void displayCycleHistory(LinearLayout cycleContainer) {
        cycleContainer.removeAllViews(); // Clear existing views
        List<Cycle> cycleHistory = getCycleHistory();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

        // Filter out invalid cycles
        List<Cycle> validCycles = new ArrayList<>();
        for (Cycle cycle : cycleHistory) {
            if (cycle.getType() != null) {
                validCycles.add(cycle);
            }
        }

        // Update SharedPreferences if invalid cycles were removed
        if (validCycles.size() < cycleHistory.size()) {
            SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, requireContext().MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            Gson gson = new Gson();
            editor.putString(KEY_CYCLE_HISTORY, gson.toJson(validCycles));
            editor.apply();
        }

        // Sort cycles by date in descending order (newest first)
        validCycles.sort((c1, c2) -> Long.compare(c2.getDateMillis(), c1.getDateMillis()));

        for (Cycle cycle : validCycles) {
            CardView cardView = new CardView(requireContext());
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, 36);     // 12dp bottom margin
            cardView.setLayoutParams(cardParams);   // Set layout parameters for the card
            cardView.setCardElevation(0f);          // 4dp elevation
            cardView.setRadius(24);                 // 8dp corner radius
            cardView.setCardBackgroundColor(requireContext().getResources().getColor(android.R.color.transparent)); // Transparent background
            cardView.setPadding(36, 36, 36, 36);    // 12dp padding

            // Create a LinearLayout to hold the card content
            LinearLayout cardContent = new LinearLayout(requireContext());
            cardContent.setOrientation(LinearLayout.VERTICAL);
            cardContent.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            // Create and configure the TextView for the date
            TextView dateTextView = new TextView(requireContext());
            dateTextView.setTextSize(18);
            dateTextView.setTypeface(null, android.graphics.Typeface.BOLD);
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(cycle.getDateMillis());
            String dateText;

            // Format the date based on the cycle type
            if ("insertion".equals(cycle.getType()) || "removal".equals(cycle.getType())) {
                Calendar endCal = Calendar.getInstance();
                endCal.setTimeInMillis(cycle.getEndDateMillis());

                if (cycle.getEndDateMillis() > 0) {
                    dateText = String.format("%s - %s",
                            dateFormat.format(cal.getTime()),
                            dateFormat.format(endCal.getTime()));
                } else {
                    dateText = dateFormat.format(cal.getTime());
                }
            } else {
                // If the cycle type is unknown, just show the start date
                dateText = dateFormat.format(cal.getTime());
            }
            dateTextView.setText(dateText);
            cardContent.addView(dateTextView);

            // Create and configure the TextView for the status
            TextView statusTextView = new TextView(requireContext());
            statusTextView.setTextSize(14);
            statusTextView.setPadding(0, 12, 0, 0); // 4dp top margin
            if ("insertion".equals(cycle.getType())) {
                statusTextView.setText("Ring eingelegt");
                statusTextView.setTextColor(0xFF4CAF50); // Green
            } else {
                statusTextView.setText("Ring entfernt");
                statusTextView.setTextColor(0xFFF44336); // Red
            }
            cardContent.addView(statusTextView);

            cardView.addView(cardContent);
            cycleContainer.addView(cardView);
        }
    }

    // Clear the cycle history from SharedPreferences
    private void clearCycleHistory() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, requireContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_CYCLE_HISTORY);
        editor.apply();
    }

    // Retrieve the cycle history from SharedPreferences
    private List<Cycle> getCycleHistory() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, requireContext().MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString(KEY_CYCLE_HISTORY, null);
        Type type = new TypeToken<List<Cycle>>(){}.getType();
        List<Cycle> cycleHistory = gson.fromJson(json, type);
        return cycleHistory != null ? cycleHistory : new ArrayList<>();
    }
}
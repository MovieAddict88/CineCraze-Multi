package com.cinecraze.free;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    private boolean settingsChanged = false;
    private static final String PREFS_NAME = "ParentalControls";
    private static final String PIN_KEY = "pin";
    private static final String SELECTED_RATINGS_KEY = "selected_ratings";
    private static final String UNRATED_KEY = "unrated";

    private SharedPreferences prefs;
    private TextView selectedRatingsTextView;
    private SwitchCompat unratedSwitch;
    private CardView pinCard, ratingsCard, unratedCard;

    // PIN creation variables
    private View dot1, dot2, dot3, dot4;
    private View[] dots;
    private StringBuilder enteredPin = new StringBuilder();
    private Button savePinButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        selectedRatingsTextView = findViewById(R.id.selected_ratings_text_view);
        unratedSwitch = findViewById(R.id.unrated_switch);
        pinCard = findViewById(R.id.pin_card);
        ratingsCard = findViewById(R.id.ratings_card);
        unratedCard = findViewById(R.id.unrated_card);

        // PIN creation elements
        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        dot3 = findViewById(R.id.dot3);
        dot4 = findViewById(R.id.dot4);
        dots = new View[]{dot1, dot2, dot3, dot4};
        savePinButton = findViewById(R.id.save_pin_button);

        selectedRatingsTextView.setOnClickListener(v -> showMultiSelectRatingDialog());

        // Set up PIN creation
        setupPinCreation();

        if (isPinSet()) {
            promptForPin();
        } else {
            showSettings();
        }
    }

    private void setupPinCreation() {
        // Set up number buttons
        for (int i = 0; i <= 9; i++) {
            int buttonId = getResources().getIdentifier("btn" + i, "id", getPackageName());
            Button button = findViewById(buttonId);
            final int number = i;
            button.setOnClickListener(v -> {
                if (enteredPin.length() < 4) {
                    enteredPin.append(number);
                    dots[enteredPin.length() - 1].setBackgroundResource(R.drawable.pin_dot_filled);

                    // Show save button when PIN is complete
                    if (enteredPin.length() == 4) {
                        savePinButton.setVisibility(View.VISIBLE);
                    }
                }
            });
        }

        // Back button
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            if (enteredPin.length() > 0) {
                dots[enteredPin.length() - 1].setBackgroundResource(R.drawable.pin_dot_empty);
                enteredPin.deleteCharAt(enteredPin.length() - 1);
                savePinButton.setVisibility(View.GONE);
            }
        });

        // Clear button
        Button btnClear = findViewById(R.id.btnClear);
        btnClear.setOnClickListener(v -> {
            enteredPin.setLength(0);
            for (View dot : dots) {
                dot.setBackgroundResource(R.drawable.pin_dot_empty);
            }
            savePinButton.setVisibility(View.GONE);
        });

        // Save PIN button
        savePinButton.setOnClickListener(v -> {
            if (enteredPin.length() == 4) {
                savePin(enteredPin.toString());
                Toast.makeText(this, "PIN saved", Toast.LENGTH_SHORT).show();
                enteredPin.setLength(0);
                for (View dot : dots) {
                    dot.setBackgroundResource(R.drawable.pin_dot_empty);
                }
                savePinButton.setVisibility(View.GONE);
            }
        });
    }

    private boolean isPinSet() {
        return prefs.contains(PIN_KEY);
    }

    private void promptForPin() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter PIN");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_pin_entry, null);
        builder.setView(dialogView);

        // Get references to UI elements
        View dot1 = dialogView.findViewById(R.id.dot1);
        View dot2 = dialogView.findViewById(R.id.dot2);
        View dot3 = dialogView.findViewById(R.id.dot3);
        View dot4 = dialogView.findViewById(R.id.dot4);

        View[] dots = {dot1, dot2, dot3, dot4};
        StringBuilder enteredPin = new StringBuilder();

        // Set up number buttons
        for (int i = 0; i <= 9; i++) {
            int buttonId = getResources().getIdentifier("btn" + i, "id", getPackageName());
            Button button = dialogView.findViewById(buttonId);
            final int number = i;
            button.setOnClickListener(v -> {
                if (enteredPin.length() < 4) {
                    enteredPin.append(number);
                    dots[enteredPin.length() - 1].setBackgroundResource(R.drawable.pin_dot_filled);
                }
            });
        }

        // Back button
        Button btnBack = dialogView.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            if (enteredPin.length() > 0) {
                dots[enteredPin.length() - 1].setBackgroundResource(R.drawable.pin_dot_empty);
                enteredPin.deleteCharAt(enteredPin.length() - 1);
            }
        });

        // Clear button
        Button btnClear = dialogView.findViewById(R.id.btnClear);
        btnClear.setOnClickListener(v -> {
            enteredPin.setLength(0);
            for (View dot : dots) {
                dot.setBackgroundResource(R.drawable.pin_dot_empty);
            }
        });

        AlertDialog dialog = builder.create();

        // Cancel button
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        // OK button
        Button btnOk = dialogView.findViewById(R.id.btnOk);
        btnOk.setOnClickListener(v -> {
            if (enteredPin.length() == 4) {
                if (checkPin(enteredPin.toString())) {
                    dialog.dismiss();
                    showSettings();
                } else {
                    Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show();
                    enteredPin.setLength(0);
                    for (View dot : dots) {
                        dot.setBackgroundResource(R.drawable.pin_dot_empty);
                    }
                }
            } else {
                Toast.makeText(this, "Please enter 4-digit PIN", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private boolean checkPin(String enteredPin) {
        String savedPin = prefs.getString(PIN_KEY, null);
        return enteredPin.equals(savedPin);
    }

    private void showSettings() {
        loadSettings();

        unratedSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveUnratedSetting(isChecked);
        });
    }

    private void loadSettings() {
        Set<String> selectedRatings = prefs.getStringSet(SELECTED_RATINGS_KEY, new HashSet<>(Arrays.asList("G", "PG", "PG-13", "R", "NC-17")));
        updateSelectedRatingsTextView(selectedRatings);

        boolean showUnrated = prefs.getBoolean(UNRATED_KEY, true);
        unratedSwitch.setChecked(showUnrated);
    }

    private void savePin(String pin) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PIN_KEY, pin);
        editor.apply();
    }

    private void saveRatings(Set<String> ratings) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(SELECTED_RATINGS_KEY, ratings);
        editor.apply();
        settingsChanged = true;
    }

    private void saveUnratedSetting(boolean showUnrated) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(UNRATED_KEY, showUnrated);
        editor.apply();
        settingsChanged = true;
    }

    @Override
    public void finish() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("settings_changed", settingsChanged);
        setResult(RESULT_OK, resultIntent);
        super.finish();
    }

    @Override
    public void onBackPressed() {
        // Overriding to ensure result is sent back
        finish();
    }

    private void updateSelectedRatingsTextView(Set<String> ratings) {
        if (ratings == null || ratings.isEmpty()) {
            selectedRatingsTextView.setText("Click to select ratings");
        } else {
            List<String> sortedRatings = new ArrayList<>(ratings);
            Collections.sort(sortedRatings);
            selectedRatingsTextView.setText(String.join(", ", sortedRatings));
        }
    }

    private void showMultiSelectRatingDialog() {
        String[] ratings = new String[]{"G", "PG", "PG-13", "R", "NC-17"};
        boolean[] checkedItems = new boolean[ratings.length];

        Set<String> selectedRatings = prefs.getStringSet(SELECTED_RATINGS_KEY, new HashSet<>(Arrays.asList("G", "PG", "PG-13", "R", "NC-17")));
        for (int i = 0; i < ratings.length; i++) {
            if (selectedRatings.contains(ratings[i])) {
                checkedItems[i] = true;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Allowed Ratings");
        builder.setMultiChoiceItems(ratings, checkedItems, (dialog, which, isChecked) -> {
            checkedItems[which] = isChecked;
        });

        builder.setPositiveButton("OK", (dialog, which) -> {
            Set<String> newSelectedRatings = new HashSet<>();
            for (int i = 0; i < ratings.length; i++) {
                if (checkedItems[i]) {
                    newSelectedRatings.add(ratings[i]);
                }
            }
            saveRatings(newSelectedRatings);
            updateSelectedRatingsTextView(newSelectedRatings);
        });
        builder.setNegativeButton("Cancel", null);

        builder.show();
    }
}
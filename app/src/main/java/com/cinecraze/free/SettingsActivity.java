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
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    private boolean settingsChanged = false;
    private static final String PREFS_NAME = "ParentalControls";
    private static final String PIN_KEY = "pin";
    private static final String SELECTED_COUNTRY_KEY = "selected_country";
    private static final String RATINGS_MAP_KEY = "ratings_map";
    private static final String UNRATED_KEY = "unrated";

    private SharedPreferences prefs;
    private TextView selectedRatingsTextView;
    private SwitchCompat unratedSwitch;
    private CardView pinCard, ratingsCard, unratedCard;
    private Spinner countrySpinner;
    private Gson gson = new Gson();

    private static final Map<String, List<String>> COUNTRY_RATINGS = new LinkedHashMap<>();
    private static final List<String> COUNTRIES = new ArrayList<>();

    static {
        // Using LinkedHashMap to maintain insertion order for the spinner
        COUNTRY_RATINGS.put("United States", Arrays.asList("G", "PG", "PG-13", "R", "NC-17", "TV-Y", "TV-Y7", "TV-G", "TV-PG", "TV-14", "TV-MA"));
        COUNTRY_RATINGS.put("Philippines", Arrays.asList("G", "PG", "R-13", "R-16", "R-18", "SPG"));
        COUNTRY_RATINGS.put("Japan", Arrays.asList("G", "PG12", "R15+", "R18+"));
        COUNTRY_RATINGS.put("South Korea", Arrays.asList("ALL", "12", "15", "19"));
        COUNTRY_RATINGS.put("Thailand", Arrays.asList("P", "G", "13+", "15+", "18+", "20+"));
        COUNTRY_RATINGS.put("India", Arrays.asList("U", "U/A", "U/A 7+", "U/A 13+", "U/A 16+", "A", "S"));
        COUNTRY_RATINGS.put("Turkey", Arrays.asList("Genel İzleyici", "7+", "13+", "18+"));
        // China is omitted as it has no formal rating system

        COUNTRIES.addAll(COUNTRY_RATINGS.keySet());
    }

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

        // Set up country spinner
        setupCountrySpinner();

        selectedRatingsTextView.setOnClickListener(v -> showMultiSelectRatingDialog());

        // Set up PIN creation
        setupPinCreation();

        if (isPinSet()) {
            promptForPin();
        } else {
            showSettings();
        }
    }

    private void setupCountrySpinner() {
        countrySpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, COUNTRIES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        countrySpinner.setAdapter(adapter);

        // Add the spinner to the layout
        ViewGroup parent = (ViewGroup) ratingsCard.getParent();
        int ratingsCardIndex = parent.indexOfChild(ratingsCard);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(16, 16, 16, 16);
        countrySpinner.setLayoutParams(params);
        parent.addView(countrySpinner, ratingsCardIndex);

        countrySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedCountry = COUNTRIES.get(position);
                prefs.edit().putString(SELECTED_COUNTRY_KEY, selectedCountry).apply();
                updateSelectedRatingsTextView();
                settingsChanged = true;
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
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
        String selectedCountry = prefs.getString(SELECTED_COUNTRY_KEY, COUNTRIES.get(0));
        int spinnerPosition = COUNTRIES.indexOf(selectedCountry);
        countrySpinner.setSelection(spinnerPosition);

        updateSelectedRatingsTextView();

        boolean showUnrated = prefs.getBoolean(UNRATED_KEY, true);
        unratedSwitch.setChecked(showUnrated);
    }

    private void savePin(String pin) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PIN_KEY, pin);
        editor.apply();
    }

    private void saveRatings(String country, Set<String> ratings) {
        Map<String, Set<String>> allRatings = getRatingsMap();
        allRatings.put(country, ratings);
        String json = gson.toJson(allRatings);
        prefs.edit().putString(RATINGS_MAP_KEY, json).apply();
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

    private void updateSelectedRatingsTextView() {
        String selectedCountry = (String) countrySpinner.getSelectedItem();
        Set<String> ratings = getRatingsMap().get(selectedCountry);

        if (ratings == null || ratings.isEmpty()) {
            selectedRatingsTextView.setText("Click to select ratings for " + selectedCountry);
        } else {
            List<String> sortedRatings = new ArrayList<>(ratings);
            Collections.sort(sortedRatings);
            selectedRatingsTextView.setText(String.join(", ", sortedRatings));
        }
    }

    private Map<String, Set<String>> getRatingsMap() {
        String json = prefs.getString(RATINGS_MAP_KEY, null);
        if (json == null) {
            // Default to all ratings selected for all countries
            Map<String, Set<String>> defaultMap = new HashMap<>();
            for (String country : COUNTRIES) {
                defaultMap.put(country, new HashSet<>(COUNTRY_RATINGS.get(country)));
            }
            return defaultMap;
        }
        Type type = new TypeToken<Map<String, Set<String>>>() {}.getType();
        return gson.fromJson(json, type);
    }

    private void showMultiSelectRatingDialog() {
        String selectedCountry = (String) countrySpinner.getSelectedItem();
        List<String> countryRatingsList = COUNTRY_RATINGS.get(selectedCountry);
        if (countryRatingsList == null) return;

        String[] ratings = countryRatingsList.toArray(new String[0]);
        boolean[] checkedItems = new boolean[ratings.length];

        Set<String> selectedRatings = getRatingsMap().get(selectedCountry);
        if (selectedRatings == null) {
            selectedRatings = new HashSet<>(countryRatingsList); // Default to all selected
        }

        for (int i = 0; i < ratings.length; i++) {
            if (selectedRatings.contains(ratings[i])) {
                checkedItems[i] = true;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Ratings for " + selectedCountry);
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
            saveRatings(selectedCountry, newSelectedRatings);
            updateSelectedRatingsTextView();
        });
        builder.setNegativeButton("Cancel", null);

        builder.show();
    }
}
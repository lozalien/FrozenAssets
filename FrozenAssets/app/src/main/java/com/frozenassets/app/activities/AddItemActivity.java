package com.frozenassets.app.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.activity.OnBackPressedCallback;

import com.frozenassets.app.R;
import com.frozenassets.app.models.FoodCategory;
import com.frozenassets.app.models.InventoryItem;
import com.frozenassets.app.models.Tag;
import com.frozenassets.app.ViewModels.InventoryViewModel;
import com.frozenassets.app.database.InventoryDatabase;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddItemActivity extends AppCompatActivity {
    private static final String KEY_NAME = "name";
    private static final String KEY_CATEGORY = "category";
    private static final String KEY_QUANTITY = "quantity";
    private static final String KEY_DATE = "date";
    private static final String KEY_SELECTED_TAGS = "selected_tags";
    private static final String KEY_UNIT = "unit";
    private static final String KEY_FREEZE_MONTHS = "freeze_months";
    private static final int DEFAULT_FREEZE_MONTHS = 6; // 6 months default

    private InventoryViewModel viewModel;
    private TextInputLayout nameLayout;
    private TextInputLayout categoryLayout;
    private TextInputLayout quantityLayout;
    private TextInputLayout dateFrozenLayout;
    private TextInputLayout unitLayout;
    private TextInputEditText nameInput;
    private AutoCompleteTextView categoryInput;
    private TextInputEditText quantityInput;
    private TextInputEditText dateFrozenInput;
    private TextInputEditText unitInput;
    private ChipGroup tagsGroup;
    private Calendar calendar;
    private Date dateFrozen;
    private SimpleDateFormat dateFormat;
    private boolean isEditMode = false;
    private int editItemId = -1;
    private InventoryItem currentItem;

    // Freeze time control
    private int freezeMonths;
    private TextView freezeTimeText;
    private ImageButton decreaseTimeButton;
    private ImageButton increaseTimeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        // Check if we're in edit mode
        isEditMode = getIntent().getBooleanExtra("is_edit", false);
        editItemId = getIntent().getIntExtra("item_id", -1);

        dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        calendar = Calendar.getInstance();
        dateFrozen = calendar.getTime();

        // Initialize freezeDays from category or saved state
        if (savedInstanceState != null) {
            freezeMonths = savedInstanceState.getInt(KEY_FREEZE_MONTHS, DEFAULT_FREEZE_MONTHS);
        } else {
            freezeMonths = DEFAULT_FREEZE_MONTHS;
        }

        initializeViews();
        setupValidation();
        setupOnBackPressedCallback();

        if (isEditMode && editItemId != -1) {
            loadExistingItem();
        } else if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        }
    }

    private void initializeViews() {
        // Initialize TextInputLayouts and inputs
        nameLayout = findViewById(R.id.name_layout);
        categoryLayout = findViewById(R.id.category_layout);
        quantityLayout = findViewById(R.id.quantity_layout);
        dateFrozenLayout = findViewById(R.id.date_frozen_layout);
        unitLayout = findViewById(R.id.weight_layout);
        nameInput = findViewById(R.id.name_input);
        categoryInput = findViewById(R.id.category_input);
        quantityInput = findViewById(R.id.quantity_input);
        dateFrozenInput = findViewById(R.id.date_frozen_input);
        unitInput = findViewById(R.id.unit_input);
        tagsGroup = findViewById(R.id.tags_group);

        // Initialize freeze time controls
        freezeTimeText = findViewById(R.id.freeze_time_text);
        decreaseTimeButton = findViewById(R.id.decrease_time_button);
        increaseTimeButton = findViewById(R.id.increase_time_button);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(isEditMode ? R.string.title_activity_edit_item : R.string.add_item);
        }

        // Setup ViewModel
        viewModel = new ViewModelProvider(this).get(InventoryViewModel.class);

        // Setup category dropdown
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                FoodCategory.getDefaultCategories()
        );
        categoryInput.setAdapter(adapter);
        categoryInput.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCategory = parent.getItemAtPosition(position).toString();
            long duration = FoodCategory.getDurationForCategory(selectedCategory);
            freezeMonths = (int) (duration / (30L * 24L * 60L * 60L * 1000L));
            updateFreezeTimeDisplay();
        });

        // Setup date picker
        dateFrozenInput.setText(dateFormat.format(dateFrozen));
        setupDatePicker();

        // Setup tags
        setupTags();

        // Setup freeze time controls
        setupFreezeTimeControls();

        // Setup save button
        findViewById(R.id.save_button).setOnClickListener(v -> saveItem());
        
        // Setup manage tags button
        findViewById(R.id.manage_tags_button).setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, TagsActivity.class);
            startActivity(intent);
        });

        // Set helper texts
        nameLayout.setHelperText(getString(R.string.helper_name));
        categoryLayout.setHelperText(getString(R.string.helper_category));
        quantityLayout.setHelperText(getString(R.string.helper_quantity));
        dateFrozenLayout.setHelperText(getString(R.string.helper_date));
        unitLayout.setHelperText(getString(R.string.helper_unit));
    }

    private void setupValidation() {
        nameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateName();
            }
        });

        categoryInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateCategory();
            }
        });

        quantityInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateQuantity();
            }
        });

        unitInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateUnit();
            }
        });
    }

    private void setupDatePicker() {
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, day) -> {
            Calendar now = Calendar.getInstance();
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, day);

            if (selected.after(now)) {
                Snackbar.make(view, R.string.future_date, Snackbar.LENGTH_SHORT).show();
                return;
            }

            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            dateFrozen = calendar.getTime();
            dateFrozenInput.setText(dateFormat.format(dateFrozen));
        };

        dateFrozenInput.setOnClickListener(v -> {
            new DatePickerDialog(
                    this,
                    dateSetListener,
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }

    private void setupTags() {
        tagsGroup.removeAllViews();
        
        // Load tags from database
        InventoryDatabase.getDatabase(this).tagDao().getAllTags().observe(this, tags -> {
            if (tags != null) {
                tagsGroup.removeAllViews();
                
                for (Tag tag : tags) {
                    Chip chip = new Chip(this);
                    chip.setText(tag.getName());
                    chip.setCheckable(true);
                    chip.setClickable(true);
                    chip.setCheckedIconVisible(false); // Hide checkmark icon
                    chip.setRippleColorResource(R.color.primary);
                    chip.setChipMinHeight(getResources().getDimensionPixelSize(R.dimen.chip_min_height));
                    chip.setTextStartPadding(getResources().getDimensionPixelSize(R.dimen.chip_text_padding));
                    chip.setTextEndPadding(getResources().getDimensionPixelSize(R.dimen.chip_text_padding));
                    
                    // Set styling based on selection state
                    chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        updateChipAppearance(chip, isChecked);
                    });
                    
                    // Set initial appearance
                    updateChipAppearance(chip, false);
                    
                    tagsGroup.addView(chip);
                }
                
                // Add "Add Custom Tag" button
                addCustomTagButton();
            }
        });
    }
    
    private void updateChipAppearance(Chip chip, boolean isSelected) {
        if (isSelected) {
            chip.setChipBackgroundColorResource(R.color.primary);
            chip.setTextColor(getResources().getColor(android.R.color.white, getTheme()));
        } else {
            chip.setChipBackgroundColorResource(R.color.surface_variant);
            chip.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));
        }
    }
    
    private void addCustomTagButton() {
        Chip addButton = new Chip(this);
        addButton.setText("+ Add Custom Tag");
        addButton.setCheckable(false);
        addButton.setClickable(true);
        addButton.setChipBackgroundColorResource(R.color.surface_variant);
        addButton.setTextColor(getResources().getColor(R.color.primary, getTheme()));
        addButton.setRippleColorResource(R.color.primary);
        addButton.setChipMinHeight(getResources().getDimensionPixelSize(R.dimen.chip_min_height));
        addButton.setTextStartPadding(getResources().getDimensionPixelSize(R.dimen.chip_text_padding));
        addButton.setTextEndPadding(getResources().getDimensionPixelSize(R.dimen.chip_text_padding));
        
        addButton.setOnClickListener(v -> showAddCustomTagDialog());
        
        tagsGroup.addView(addButton);
    }
    
    private void showAddCustomTagDialog() {
        android.widget.EditText editText = new android.widget.EditText(this);
        editText.setHint("Enter custom tag name");
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Add Custom Tag")
                .setView(editText)
                .setPositiveButton("Add", (dialog, which) -> {
                    String tagName = editText.getText().toString().trim();
                    if (!tagName.isEmpty()) {
                        addCustomTag(tagName);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void addCustomTag(String tagName) {
        // Check if tag already exists
        new Thread(() -> {
            Tag existingTag = InventoryDatabase.getDatabase(this).tagDao().getTagByName(tagName);
            if (existingTag == null) {
                Tag newTag = new Tag(tagName, false);
                InventoryDatabase.getDatabase(this).tagDao().insert(newTag);
                
                runOnUiThread(() -> {
                    Snackbar.make(findViewById(android.R.id.content),
                            "Tag '" + tagName + "' added successfully",
                            Snackbar.LENGTH_SHORT).show();
                });
            } else {
                runOnUiThread(() -> {
                    Snackbar.make(findViewById(android.R.id.content),
                            "Tag already exists",
                            Snackbar.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void setupFreezeTimeControls() {
        updateFreezeTimeDisplay();

        decreaseTimeButton.setOnClickListener(v -> {
            if (freezeMonths > 1) {
                freezeMonths--;
                updateFreezeTimeDisplay();
            }
        });

        increaseTimeButton.setOnClickListener(v -> {
            if (freezeMonths < 24) {
                freezeMonths++;
                updateFreezeTimeDisplay();
            }
        });
    }

    private void updateFreezeTimeDisplay() {
        if (freezeTimeText != null) {
            freezeTimeText.setText(String.format(Locale.getDefault(),
                    "%d %s", freezeMonths,
                    freezeMonths == 1 ? "Month" : "Months"));
        }
    }

    private boolean validateName() {
        String name = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
        if (TextUtils.isEmpty(name)) {
            nameLayout.setError(getString(R.string.error_name_required));
            return false;
        }
        if (name.length() < 2) {
            nameLayout.setError(getString(R.string.error_name_too_short));
            return false;
        }
        nameLayout.setError(null);
        return true;
    }

    private boolean validateCategory() {
        String category = categoryInput.getText().toString().trim();
        if (TextUtils.isEmpty(category)) {
            categoryLayout.setError(getString(R.string.error_category_required));
            return false;
        }
        if (!FoodCategory.getDefaultCategories().contains(category)) {
            categoryLayout.setError(getString(R.string.error_invalid_category));
            return false;
        }
        categoryLayout.setError(null);
        return true;
    }

    private boolean validateQuantity() {
        String quantityStr = quantityInput.getText() != null ? quantityInput.getText().toString().trim() : "";
        if (TextUtils.isEmpty(quantityStr)) {
            quantityLayout.setError(getString(R.string.error_quantity_required));
            return false;
        }
        try {
            int quantity = Integer.parseInt(quantityStr);
            if (quantity <= 0) {
                quantityLayout.setError(getString(R.string.error_quantity_positive));
                return false;
            }
            if (quantity > 999) {
                quantityLayout.setError(getString(R.string.error_quantity_too_large));
                return false;
            }
        } catch (NumberFormatException e) {
            quantityLayout.setError(getString(R.string.error_invalid_quantity));
            return false;
        }
        quantityLayout.setError(null);
        return true;
    }

    private boolean validateUnit() {
        String unit = unitInput.getText() != null ? unitInput.getText().toString().trim() : "";
        if (TextUtils.isEmpty(unit)) {
            unitLayout.setError(null);
            return true;
        }
        unitLayout.setError(null);
        return true;
    }

    private boolean validateAll() {
        return validateName() && validateCategory() && validateQuantity() && validateUnit();
    }

    private void saveItem() {
        try {
            if (!validateAll()) {
                Snackbar.make(findViewById(android.R.id.content),
                        getString(R.string.error_fix_form),
                        Snackbar.LENGTH_LONG).show();
                return;
            }

            String name = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
            String category = categoryInput.getText() != null ? categoryInput.getText().toString().trim() : "";
            String quantityStr = quantityInput.getText() != null ? quantityInput.getText().toString().trim() : "0";
            int quantity = Integer.parseInt(quantityStr);
            String unit = unitInput.getText() != null ? unitInput.getText().toString().trim() : "";
            ArrayList<String> selectedTags = getSelectedTags();

            if (isEditMode && currentItem != null) {
                currentItem.setName(name);
                currentItem.setCategory(category);
                currentItem.setQuantity(quantity);
                currentItem.setDateFrozen(dateFrozen);
                currentItem.setTags(selectedTags);
                currentItem.setMaxFreezeDays(freezeMonths * 30);
                currentItem.setWeight(null);
                currentItem.setWeightUnit(unit);

                Date newExpirationDate = new Date(dateFrozen.getTime() +
                        (long)freezeMonths * 30L * 24L * 60L * 60L * 1000L);
                currentItem.setExpirationDate(newExpirationDate);

                viewModel.update(currentItem);

                // Show save confirmation then return
                Snackbar.make(findViewById(android.R.id.content),
                                getString(R.string.item_updated),
                                Snackbar.LENGTH_SHORT)
                        .addCallback(new Snackbar.Callback() {
                            @Override
                            public void onDismissed(Snackbar snackbar, int event) {
                                finish(); // This will return to the previous screen
                            }
                        }).show();
            } else {
                Date expirationDate = new Date(dateFrozen.getTime() +
                        (long)freezeMonths * 30L * 24L * 60L * 60L * 1000L);

                InventoryItem item = new InventoryItem(
                        name,
                        category,
                        quantity,
                        dateFrozen,
                        expirationDate,
                        selectedTags,
                        null,  // No weight
                        unit,  // Store unit string
                        freezeMonths * 30
                );

                viewModel.insert(item);

                Snackbar.make(findViewById(android.R.id.content),
                                getString(R.string.item_saved),
                                Snackbar.LENGTH_SHORT)
                        .addCallback(new Snackbar.Callback() {
                            @Override
                            public void onDismissed(Snackbar snackbar, int event) {
                                finish();
                            }
                        }).show();
            }

        } catch (Exception e) {
            Log.e("AddItemActivity", "Error saving item", e);
            Snackbar.make(findViewById(android.R.id.content),
                    "Error saving item: " + e.getMessage(),
                    Snackbar.LENGTH_LONG).show();
        }
    }

    private ArrayList<String> getSelectedTags() {
        ArrayList<String> tags = new ArrayList<>();
        for (int i = 0; i < tagsGroup.getChildCount(); i++) {
            Chip chip = (Chip) tagsGroup.getChildAt(i);
            if (chip.isChecked()) {
                tags.add(chip.getText().toString());
            }
        }
        return tags;
    }

    private void loadExistingItem() {
        viewModel.getItemById(editItemId).observe(this, item -> {
            if (item != null) {
                currentItem = item;

                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(R.string.title_activity_edit_item);
                }

                nameInput.setText(item.getName());
                categoryInput.setText(item.getCategory(), false);
                quantityInput.setText(String.valueOf(item.getQuantity()));

                dateFrozen = item.getDateFrozen();
                calendar.setTime(dateFrozen);
                dateFrozenInput.setText(dateFormat.format(dateFrozen));

                freezeMonths = item.getMaxFreezeDays() / 30;
                updateFreezeTimeDisplay();

                // Set unit if exists
                if (item.getWeightUnit() != null) {
                    unitInput.setText(item.getWeightUnit());
                }

                // Set tags
                for (int i = 0; i < tagsGroup.getChildCount(); i++) {
                    View view = tagsGroup.getChildAt(i);
                    if (view instanceof Chip) {
                        Chip chip = (Chip) view;
                        chip.setChecked(item.getTags().contains(chip.getText().toString()));
                    }
                }
            }
        });
    }

    private void restoreState(Bundle savedInstanceState) {
        nameInput.setText(savedInstanceState.getString(KEY_NAME, ""));
        categoryInput.setText(savedInstanceState.getString(KEY_CATEGORY, ""));
        quantityInput.setText(savedInstanceState.getString(KEY_QUANTITY, ""));
        unitInput.setText(savedInstanceState.getString(KEY_UNIT, ""));

        long savedDate = savedInstanceState.getLong(KEY_DATE, Calendar.getInstance().getTimeInMillis());
        dateFrozen = new Date(savedDate);
        calendar.setTime(dateFrozen);
        dateFrozenInput.setText(dateFormat.format(dateFrozen));

        freezeMonths = savedInstanceState.getInt(KEY_FREEZE_MONTHS, DEFAULT_FREEZE_MONTHS);
        updateFreezeTimeDisplay();

        ArrayList<String> savedTags = savedInstanceState.getStringArrayList(KEY_SELECTED_TAGS);
        if (savedTags != null) {
            for (int i = 0; i < tagsGroup.getChildCount(); i++) {
                Chip chip = (Chip) tagsGroup.getChildAt(i);
                chip.setChecked(savedTags.contains(chip.getText().toString()));
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_NAME, nameInput.getText() != null ? nameInput.getText().toString() : "");
        outState.putString(KEY_CATEGORY, categoryInput.getText() != null ? categoryInput.getText().toString() : "");
        outState.putString(KEY_QUANTITY, quantityInput.getText() != null ? quantityInput.getText().toString() : "0");
        outState.putString(KEY_UNIT, unitInput.getText() != null ? unitInput.getText().toString() : "");
        outState.putLong(KEY_DATE, dateFrozen.getTime());
        outState.putStringArrayList(KEY_SELECTED_TAGS, getSelectedTags());
        outState.putInt(KEY_FREEZE_MONTHS, freezeMonths);
    }

    private boolean hasUnsavedChanges() {
        if (isEditMode && currentItem != null) {
            String nameText = nameInput.getText() != null ? nameInput.getText().toString() : "";
            String categoryText = categoryInput.getText() != null ? categoryInput.getText().toString() : "";
            String quantityText = quantityInput.getText() != null ? quantityInput.getText().toString() : "0";
            return !currentItem.getName().equals(nameText) ||
                    !currentItem.getCategory().equals(categoryText) ||
                    currentItem.getQuantity() != Integer.parseInt(quantityText.isEmpty() ? "0" : quantityText) ||
                    !currentItem.getDateFrozen().equals(dateFrozen) ||
                    currentItem.getMaxFreezeDays() != (freezeMonths * 30) ||
                    !getSelectedTags().equals(currentItem.getTags()) ||
                    !TextUtils.equals(currentItem.getWeightUnit(), unitInput.getText() != null ? unitInput.getText().toString() : "");
        } else {
            return !TextUtils.isEmpty(nameInput.getText()) ||
                    !TextUtils.isEmpty(categoryInput.getText()) ||
                    !TextUtils.isEmpty(quantityInput.getText()) ||
                    !TextUtils.isEmpty(unitInput.getText()) ||
                    freezeMonths != DEFAULT_FREEZE_MONTHS ||
                    !getSelectedTags().isEmpty();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (hasUnsavedChanges()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.unsaved_changes)
                    .setMessage(R.string.confirm_exit)
                    .setPositiveButton(R.string.yes, (dialog, which) -> finish())
                    .setNegativeButton(R.string.no, null)
                    .show();
            return true;
        }
        finish();
        return true;
    }

    private void setupOnBackPressedCallback() {
        OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (hasUnsavedChanges()) {
                    new MaterialAlertDialogBuilder(AddItemActivity.this)
                            .setTitle(R.string.unsaved_changes)
                            .setMessage(R.string.confirm_exit)
                            .setPositiveButton(R.string.yes, (dialog, which) -> {
                                setEnabled(false);
                                getOnBackPressedDispatcher().onBackPressed();
                            })
                            .setNegativeButton(R.string.no, null)
                            .show();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    @Override
    public void finish() {
        super.finish();
        // Modern activity transitions are handled automatically by the system
    }
}
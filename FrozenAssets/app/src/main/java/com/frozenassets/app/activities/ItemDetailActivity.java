package com.frozenassets.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;  // Add this import
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.frozenassets.app.R;
import com.frozenassets.app.models.InventoryItem;
import com.frozenassets.app.ViewModels.InventoryViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


public class ItemDetailActivity extends AppCompatActivity {
    private EditText notesInput;
    private InventoryViewModel viewModel;
    private InventoryItem currentItem;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(InventoryViewModel.class);

        // Get item ID from intent
        int itemId = getIntent().getIntExtra("item_id", -1);
        if (itemId == -1) {
            finish();
            return;
        }

        // Observe item details
        viewModel.getItemById(itemId).observe(this, item -> {
            if (item != null) {
                currentItem = item;
                updateUI(item);
            }
        });

        // Setup buttons
        findViewById(R.id.edit_button).setOnClickListener(v -> startEditMode());
        findViewById(R.id.delete_button).setOnClickListener(v -> confirmDelete());
        notesInput = findViewById(R.id.notes_input);
        setupNotesSaving();
    }
    private void setupNotesSaving() {
        notesInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && currentItem != null) {
                String notes = notesInput.getText().toString();
                currentItem.setNotes(notes);
                viewModel.update(currentItem);
            }
        });

        // Also save when the back button is pressed
        notesInput.setOnEditorActionListener((v, actionId, event) -> {
            String notes = notesInput.getText().toString();
            if (currentItem != null) {
                currentItem.setNotes(notes);
                viewModel.update(currentItem);
            }
            return false;
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (currentItem != null && notesInput != null) {
            String notes = notesInput.getText().toString();
            currentItem.setNotes(notes);
            viewModel.update(currentItem);
        }
    }

    private void updateUI(InventoryItem item) {
        setTitle(item.getName());

        // Set basic item details
        ((TextView) findViewById(R.id.text_name)).setText(item.getName());
        ((TextView) findViewById(R.id.text_category)).setText(getString(R.string.category_label, item.getCategory()));
        ((TextView) findViewById(R.id.text_quantity)).setText(getString(R.string.quantity_label, item.getQuantity()));
        ((TextView) findViewById(R.id.text_frozen_date)).setText(getString(R.string.frozen_on,
                dateFormat.format(item.getDateFrozen())));
        ((TextView) findViewById(R.id.text_expiry_date)).setText(getString(R.string.expires_on,
                dateFormat.format(item.getExpirationDate())));

        // Set notes if they exist
        if (item.getNotes() != null) {
            notesInput.setText(item.getNotes());
        }

        // Calculate and set expiration status
        long daysUntilExpiration = getDaysUntilExpiration(item.getExpirationDate());
        TextView countdownView = findViewById(R.id.text_expiration_countdown);
        MaterialCardView cardView = findViewById(R.id.detail_card);

        int backgroundColor;
        int textColor;

        if (daysUntilExpiration < 0) {
            backgroundColor = ContextCompat.getColor(this, R.color.expiration_expired);
            textColor = ContextCompat.getColor(this, R.color.text_expired);
            countdownView.setText(String.format(Locale.getDefault(), "%d DAYS\nEXPIRED",
                    Math.abs(daysUntilExpiration)));
        } else if (daysUntilExpiration <= 14) {
            backgroundColor = ContextCompat.getColor(this, R.color.expiration_critical);
            textColor = ContextCompat.getColor(this, R.color.text_critical);
            countdownView.setText(String.format(Locale.getDefault(), "EXPIRES IN\n%d DAYS",
                    daysUntilExpiration));
        } else if (daysUntilExpiration <= 60) {
            backgroundColor = ContextCompat.getColor(this, R.color.expiration_warning);
            textColor = ContextCompat.getColor(this, R.color.text_warning);
            countdownView.setText(String.format(Locale.getDefault(), "EXPIRES IN\n%d DAYS",
                    daysUntilExpiration));
        } else {
            backgroundColor = ContextCompat.getColor(this, R.color.expiration_normal);
            textColor = ContextCompat.getColor(this, R.color.text_primary);
            countdownView.setText(String.format(Locale.getDefault(), "EXPIRES IN\n%d DAYS",
                    daysUntilExpiration));
        }

        cardView.setCardBackgroundColor(backgroundColor);
        countdownView.setTextColor(textColor);
        countdownView.setVisibility(View.VISIBLE);

        // Setup tags
        ChipGroup chipGroup = findViewById(R.id.chip_group_tags);
        chipGroup.removeAllViews();
        for (String tag : item.getTags()) {
            Chip chip = new Chip(this);
            chip.setText(tag);
            chip.setClickable(false);
            chip.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            chipGroup.addView(chip);
        }
    }

    private void startEditMode() {
        Intent intent = new Intent(this, AddItemActivity.class);
        intent.putExtra("item_id", currentItem.getId());
        intent.putExtra("is_edit", true);
        startActivity(intent);
        // Don't finish the activity here if you want to return to the detail view
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_item)
                .setMessage(R.string.confirm_delete)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    viewModel.delete(currentItem);
                    finish();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private long getDaysUntilExpiration(Date expirationDate) {
        long now = System.currentTimeMillis();
        long diff = expirationDate.getTime() - now;
        return TimeUnit.MILLISECONDS.toDays(diff);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
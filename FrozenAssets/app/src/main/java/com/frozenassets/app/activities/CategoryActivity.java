package com.frozenassets.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.frozenassets.app.R;
import com.frozenassets.app.ViewModels.InventoryViewModel;
import com.frozenassets.app.adapters.InventoryAdapter;
import com.frozenassets.app.models.InventoryItem;
import com.frozenassets.app.models.SortOrder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class CategoryActivity extends AppCompatActivity {
    private static final String TAG = "CategoryActivity";
    public static final String EXTRA_CATEGORY_NAME = "category_name";
    
    private InventoryViewModel viewModel;
    private InventoryAdapter adapter;
    private String categoryName;
    private SortOrder currentSortOrder = SortOrder.EXPIRATION_ASC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");

        try {
            setContentView(R.layout.activity_category);

            // Get category name from intent
            categoryName = getIntent().getStringExtra(EXTRA_CATEGORY_NAME);
            if (categoryName == null) {
                Log.e(TAG, "No category name provided");
                Toast.makeText(this, "Error: No category specified", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            Log.d(TAG, "Category: " + categoryName);

            setupToolbar();
            setupRecyclerView();
            setupViewModel();
            setupFAB();

            // Set title to category name
            setTitle(categoryName);

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error starting category view", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when resuming
        if (viewModel != null && categoryName != null) {
            loadCategoryItems();
        }
    }

    private void setupToolbar() {
        try {
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setDisplayShowHomeEnabled(true);
                }
                Log.d(TAG, "Toolbar setup complete");
            } else {
                Log.e(TAG, "Toolbar not found in layout");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up toolbar", e);
        }
    }

    private void setupRecyclerView() {
        try {
            RecyclerView recyclerView = findViewById(R.id.recycler_view);
            if (recyclerView != null) {
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                adapter = new InventoryAdapter(
                    // OnItemClickListener
                    item -> {
                        Log.d(TAG, "Item clicked: " + item.getName());
                        Intent intent = new Intent(CategoryActivity.this, ItemDetailActivity.class);
                        intent.putExtra("item_id", item.getId());
                        startActivity(intent);
                    },
                    // OnItemLongClickListener - CategoryActivity doesn't support multi-select
                    (item, position) -> {
                        Log.d(TAG, "Long click not supported in CategoryActivity");
                        return false;
                    }
                );
                recyclerView.setAdapter(adapter);
                Log.d(TAG, "RecyclerView setup complete");
            } else {
                Log.e(TAG, "RecyclerView not found in layout");
                throw new RuntimeException("RecyclerView not found in layout");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerView", e);
            throw e;
        }
    }

    private void setupViewModel() {
        try {
            viewModel = new ViewModelProvider(this).get(InventoryViewModel.class);
            loadCategoryItems();
            Log.d(TAG, "ViewModel setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up ViewModel", e);
            runOnUiThread(() -> Toast.makeText(this, "Error initializing data", Toast.LENGTH_LONG).show());
        }
    }

    private void loadCategoryItems() {
        if (viewModel == null || categoryName == null) {
            Log.w(TAG, "Cannot load items - viewModel or categoryName is null");
            return;
        }

        try {
            Log.d(TAG, "Loading items for category: " + categoryName + " with sort order: " + currentSortOrder);
            viewModel.getItemsByCategory(categoryName, currentSortOrder).observe(this, items -> {
                if (isFinishing() || isDestroyed()) return;
                
                Log.d(TAG, "Category " + categoryName + " loaded. Count: " + (items != null ? items.size() : 0));
                if (adapter != null) {
                    adapter.submitList(items);
                }
                
                // Show empty state if no items
                if (items == null || items.isEmpty()) {
                    Toast.makeText(this, "No items found in " + categoryName, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading category items", e);
            Toast.makeText(this, "Error loading items", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupFAB() {
        try {
            FloatingActionButton fab = findViewById(R.id.fab_add);
            if (fab != null) {
                fab.setOnClickListener(view -> {
                    Log.d(TAG, "FAB clicked");
                    try {
                        Intent intent = new Intent(CategoryActivity.this, AddItemActivity.class);
                        // Pre-select the current category
                        intent.putExtra("preselected_category", categoryName);
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Error starting AddItemActivity", e);
                        Toast.makeText(this, "Error opening Add Item screen", Toast.LENGTH_SHORT).show();
                    }
                });
                Log.d(TAG, "FAB setup complete");
            } else {
                Log.w(TAG, "FAB not found in layout");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up FAB", e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sort_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem sortItem = menu.findItem(R.id.action_sort);
        if (sortItem != null) {
            // Update the sort icon title based on current sort order
            if (currentSortOrder == SortOrder.EXPIRATION_ASC) {
                sortItem.setTitle(R.string.sort_expiring_first);
            } else {
                sortItem.setTitle(R.string.sort_expiring_last);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.action_sort) {
            toggleSortOrder();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleSortOrder() {
        // Toggle the sort order
        currentSortOrder = (currentSortOrder == SortOrder.EXPIRATION_ASC) 
            ? SortOrder.EXPIRATION_DESC 
            : SortOrder.EXPIRATION_ASC;
        
        // Refresh the menu to update the icon
        invalidateOptionsMenu();
        
        // Reload the data with new sort order
        loadCategoryItems();
    }
}

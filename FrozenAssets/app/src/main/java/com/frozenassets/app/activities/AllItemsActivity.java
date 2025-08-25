package com.frozenassets.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.os.Build;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.activity.OnBackPressedCallback;

import com.frozenassets.app.R;
import com.frozenassets.app.adapters.InventoryAdapter;
import com.frozenassets.app.models.InventoryItem;
import com.frozenassets.app.ViewModels.InventoryViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.util.List;

public class AllItemsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "AllItemsActivity";
    private DrawerLayout drawerLayout;
    private InventoryViewModel viewModel;
    private InventoryAdapter adapter;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private LiveData<List<InventoryItem>> allItemsLiveData = null;
    private ActionMode actionMode;
    private ActionMode.Callback actionModeCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");
        MobileAds.initialize(this, initializationStatus -> {});

        try {
            // Enable hardware acceleration safely
            if (getWindow() != null) {
                getWindow().setFlags(
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
            }

            setContentView(R.layout.activity_all_items);

            // Setup UI components
            setupToolbar();
            setupNavigationDrawer();
            setupRecyclerView();

            // Initialize ViewModel - do this synchronously
            setupViewModel();
            setupFAB();

            // Initialize AdMob banner with error handling
            setupAdMob();

            // Set title and load all items
            setTitle(getString(R.string.all_items));
            loadAllItems();

            // Setup OnBackPressedCallback for Android 14+ compatibility
            setupOnBackPressedCallback();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error starting app", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        try {
            // Add null check before refreshing
            if (viewModel != null) {
                loadAllItems();
            }

            // Enable hardware acceleration safely
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && getWindow() != null) {
                getWindow().getDecorView().setRenderEffect(null);
            }
            if (getWindow() != null) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume", e);
        }
    }

    private void setupToolbar() {
        try {
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                Log.d(TAG, "Toolbar setup complete");
            } else {
                Log.e(TAG, "Toolbar not found in layout");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up toolbar", e);
        }
    }

    private void loadAllItems() {
        // Ensure viewModel is not null and activity is not finishing
        if (viewModel == null || isFinishing() || isDestroyed()) {
            Log.w(TAG, "Cannot load all items - activity state invalid");
            return;
        }

        try {
            // Remove previous observer if exists
            if (allItemsLiveData != null) {
                allItemsLiveData.removeObservers(this);
            }

            allItemsLiveData = viewModel.getAllItems();
            if (allItemsLiveData != null) {
                allItemsLiveData.observe(this, items -> {
                    if (isFinishing() || isDestroyed()) return;
                    
                    Log.d(TAG, "Updating all items. Count: " + (items != null ? items.size() : 0));
                    if (adapter != null) {
                        adapter.submitList(null); // Clear current list
                        adapter.submitList(items); // Add new items
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading all items", e);
        }
    }

    private void setupNavigationDrawer() {
        try {
            drawerLayout = findViewById(R.id.drawer_layout);
            NavigationView navigationView = findViewById(R.id.nav_view);
            Toolbar toolbar = findViewById(R.id.toolbar);

            if (drawerLayout != null && navigationView != null && toolbar != null) {
                ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                        this, drawerLayout, toolbar,
                        R.string.navigation_drawer_open,
                        R.string.navigation_drawer_close);
                drawerLayout.addDrawerListener(toggle);
                toggle.syncState();
                navigationView.setNavigationItemSelectedListener(this);
                Log.d(TAG, "Navigation drawer setup complete");
            } else {
                Log.e(TAG, "Navigation drawer components not found in layout");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up navigation drawer", e);
        }
    }

    private void setupViewModel() {
        try {
            viewModel = new ViewModelProvider(this).get(InventoryViewModel.class);
            Log.d(TAG, "ViewModel setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up ViewModel", e);
            runOnUiThread(() -> Toast.makeText(this, "Error initializing data", Toast.LENGTH_LONG).show());
        }
    }

    private void setupRecyclerView() {
        try {
            RecyclerView recyclerView = findViewById(R.id.recycler_view);
            if (recyclerView != null) {
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                
                // Setup ActionMode callback for multi-select
                setupActionModeCallback();
                
                adapter = new InventoryAdapter(
                    // OnItemClickListener
                    item -> {
                        Log.d(TAG, "Item clicked: " + item.getName());
                        Intent intent = new Intent(AllItemsActivity.this, ItemDetailActivity.class);
                        intent.putExtra("item_id", item.getId());
                        startActivity(intent);
                    },
                    // OnItemLongClickListener
                    (item, position) -> {
                        Log.d(TAG, "Item long clicked: " + item.getName());
                        startActionMode(position);
                        return true;
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

    private void setupFAB() {
        try {
            FloatingActionButton fab = findViewById(R.id.fab_add);
            if (fab != null) {
                fab.setOnClickListener(view -> {
                    Log.d(TAG, "FAB clicked");
                    try {
                        Intent intent = new Intent(AllItemsActivity.this, AddItemActivity.class);
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Error starting AddItemActivity", e);
                        Toast.makeText(this, "Error opening Add Item screen", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Log.e(TAG, "FAB not found in layout");
            }
            Log.d(TAG, "FAB setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up FAB", e);
        }
    }

    private void setupAdMob() {
        try {
            AdView adview = findViewById(R.id.adView);
            if (adview != null) {
                AdRequest adRequest = new AdRequest.Builder().build();
                adview.loadAd(adRequest);
                
                adview.setAdListener(new com.google.android.gms.ads.AdListener() {
                    @Override
                    public void onAdFailedToLoad(com.google.android.gms.ads.LoadAdError loadAdError) {
                        Log.w(TAG, "Ad failed to load: " + loadAdError.getMessage());
                    }
                    
                    @Override
                    public void onAdLoaded() {
                        Log.d(TAG, "Ad loaded successfully");
                    }
                });
                
                Log.d(TAG, "AdView setup complete");
            } else {
                Log.w(TAG, "AdView not found in layout");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up AdMob", e);
            // Don't crash the app if ads fail
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        try {
            Log.d(TAG, "Navigation item selected: " + item.getTitle());
            int id = item.getItemId();

            if (id == R.id.nav_all_items) {
                // Already on all items, just reload
                loadAllItems();
            } else if (id == R.id.nav_eat_soon) {
                startActivity(new Intent(this, EatSoonActivity.class));
            } else if (id == R.id.nav_chicken) {
                navigateToCategory(getString(R.string.chicken));
            } else if (id == R.id.nav_beef) {
                navigateToCategory(getString(R.string.beef));
            } else if (id == R.id.nav_pork) {
                navigateToCategory(getString(R.string.pork));
            } else if (id == R.id.nav_fish) {
                navigateToCategory(getString(R.string.fish));
            } else if (id == R.id.nav_cooked_meals) {
                navigateToCategory(getString(R.string.cooked_meals));
            } else if (id == R.id.nav_vegetables) {
                navigateToCategory(getString(R.string.vegetables));
            } else if (id == R.id.nav_fruits) {
                navigateToCategory(getString(R.string.fruits));
            } else if (id == R.id.nav_other) {
                navigateToCategory(getString(R.string.other));
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error handling navigation selection", e);
            return false;
        }
    }

    private void navigateToCategory(String category) {
        try {
            Intent intent = new Intent(this, CategoryActivity.class);
            intent.putExtra("category", category);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to category: " + category, e);
            Toast.makeText(this, "Error opening category", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupOnBackPressedCallback() {
        try {
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    if (actionMode != null) {
                        actionMode.finish();
                    } else if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                    } else {
                        finish();
                    }
                }
            });
            Log.d(TAG, "OnBackPressedCallback setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up OnBackPressedCallback", e);
        }
    }
    
    private void setupActionModeCallback() {
        actionModeCallback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.multi_select_menu, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.action_select_all) {
                    adapter.selectAll();
                    updateActionModeTitle();
                    return true;
                } else if (id == R.id.action_deselect_all) {
                    adapter.deselectAll();
                    updateActionModeTitle();
                    return true;
                } else if (id == R.id.action_delete) {
                    confirmBulkDelete();
                    return true;
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                actionMode = null;
                adapter.clearSelection();
            }
        };
    }
    
    private void startActionMode(int position) {
        if (actionMode == null) {
            actionMode = startActionMode(actionModeCallback);
            adapter.setMultiSelectMode(true);
            adapter.toggleItemSelection(position);
            updateActionModeTitle();
        }
    }
    
    private void updateActionModeTitle() {
        if (actionMode != null) {
            int selectedCount = adapter.getSelectedItemCount();
            actionMode.setTitle(getString(R.string.multi_select_title, selectedCount));
        }
    }
    
    private void confirmBulkDelete() {
        int selectedCount = adapter.getSelectedItemCount();
        if (selectedCount == 0) {
            Toast.makeText(this, "No items selected", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new AlertDialog.Builder(this)
            .setTitle(R.string.confirm_bulk_delete_title)
            .setMessage(getString(R.string.confirm_bulk_delete_message, selectedCount))
            .setPositiveButton(R.string.delete, (dialog, which) -> performBulkDelete())
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    private void performBulkDelete() {
        if (adapter == null || viewModel == null) return;
        
        try {
            // Get selected positions and items
            java.util.Set<Integer> selectedPositions = adapter.getSelectedPositions();
            java.util.List<InventoryItem> itemsToDelete = new java.util.ArrayList<>();
            
            // Get current list from adapter
            for (int position : selectedPositions) {
                if (position < adapter.getItemCount()) {
                    InventoryItem item = adapter.getCurrentList().get(position);
                    if (item != null) {
                        itemsToDelete.add(item);
                    }
                }
            }
            
            // Delete items from database
            for (InventoryItem item : itemsToDelete) {
                viewModel.delete(item);
            }
            
            // Show success message
            Toast.makeText(this, getString(R.string.bulk_delete_success, itemsToDelete.size()), 
                          Toast.LENGTH_SHORT).show();
            
            // Exit action mode
            if (actionMode != null) {
                actionMode.finish();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error performing bulk delete", e);
            Toast.makeText(this, getString(R.string.bulk_delete_failed, e.getMessage()), 
                          Toast.LENGTH_LONG).show();
        }
    }
}
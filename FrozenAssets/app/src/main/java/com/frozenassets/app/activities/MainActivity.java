package com.frozenassets.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.view.WindowManager;
import android.os.Build;

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
import com.frozenassets.app.models.SortOrder;
import com.frozenassets.app.ViewModels.InventoryViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.List;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "MainActivity";
    private DrawerLayout drawerLayout;
    private InventoryViewModel viewModel;
    private InventoryAdapter adapter;
    private LiveData<List<InventoryItem>> currentItemsLiveData = null;
    private SortOrder currentSortOrder = SortOrder.EXPIRATION_ASC;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");

        try {
            // Enable hardware acceleration safely
            if (getWindow() != null) {
                getWindow().setFlags(
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
            }

            setContentView(R.layout.activity_main);

            // Setup UI components
            setupToolbar();
            setupNavigationDrawer();
            setupRecyclerView();

            // Initialize ViewModel - do this synchronously
            setupViewModel();
            setupFAB();

            // Default to showing "Eat Soon" items
            setTitle(R.string.expiring_items);
            loadExpiringItems();

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
                refreshCurrentView();
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

    private void refreshCurrentView() {
        // MainActivity only handles "Eat Soon" view, categories are handled by CategoryActivity
        loadExpiringItems();
    }

    private void loadExpiringItems() {
        // Ensure viewModel is not null and activity is not finishing
        if (viewModel == null || isFinishing() || isDestroyed()) {
            Log.w(TAG, "Cannot load expiring items - activity state invalid");
            return;
        }

        try {
            // Remove previous observer if exists
            if (currentItemsLiveData != null) {
                currentItemsLiveData.removeObservers(this);
            }
            currentItemsLiveData = viewModel.getExpiringItems(currentSortOrder);
            if (currentItemsLiveData != null) {
                currentItemsLiveData.observe(this, items -> {
                    if (isFinishing() || isDestroyed()) return;
                    
                    Log.d(TAG, "Updating expiring items. Count: " + (items != null ? items.size() : 0) + " with sort order: " + currentSortOrder);
                    if (adapter != null) {
                        adapter.submitList(null); // Clear current list
                        adapter.submitList(items); // Add new items
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading expiring items", e);
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
                adapter = new InventoryAdapter(
                    // OnItemClickListener
                    item -> {
                        Log.d(TAG, "Item clicked: " + item.getName());
                        Intent intent = new Intent(MainActivity.this, ItemDetailActivity.class);
                        intent.putExtra("item_id", item.getId());
                        startActivity(intent);
                    },
                    // OnItemLongClickListener - MainActivity doesn't support multi-select, so return false
                    (item, position) -> {
                        Log.d(TAG, "Long click not supported in MainActivity");
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

    private void setupFAB() {
        try {
            FloatingActionButton fab = findViewById(R.id.fab_add);
            if (fab != null) {
                fab.setOnClickListener(view -> {
                    Log.d(TAG, "FAB clicked");
                    try {
                        Intent intent = new Intent(MainActivity.this, AddItemActivity.class);
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


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        try {
            Log.d(TAG, "Navigation item selected: " + item.getTitle());
            int id = item.getItemId();

            if (id == R.id.nav_eat_soon) {
                setTitle(R.string.expiring_items);
                loadExpiringItems();
            } else if (id == R.id.nav_chicken) {
                startCategoryActivity(getString(R.string.chicken));
            } else if (id == R.id.nav_beef) {
                startCategoryActivity(getString(R.string.beef));
            } else if (id == R.id.nav_pork) {
                startCategoryActivity(getString(R.string.pork));
            } else if (id == R.id.nav_fish) {
                startCategoryActivity(getString(R.string.fish));
            } else if (id == R.id.nav_cooked_meals) {
                startCategoryActivity(getString(R.string.cooked_meals));
            } else if (id == R.id.nav_vegetables) {
                startCategoryActivity(getString(R.string.vegetables));
            } else if (id == R.id.nav_fruits) {
                startCategoryActivity(getString(R.string.fruits));
            } else if (id == R.id.nav_other) {
                startCategoryActivity(getString(R.string.other));
            } else if (id == R.id.nav_all_items) {
                startActivity(new Intent(this, AllItemsActivity.class));
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

    private void startCategoryActivity(String category) {
        try {
            Log.d(TAG, "Starting CategoryActivity for: " + category);
            Intent intent = new Intent(this, CategoryActivity.class);
            intent.putExtra(CategoryActivity.EXTRA_CATEGORY_NAME, category);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error starting CategoryActivity for: " + category, e);
            Toast.makeText(this, "Error opening " + category + " category", Toast.LENGTH_SHORT).show();
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
        if (itemId == R.id.action_sort) {
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
        loadExpiringItems();
    }

    private void setupOnBackPressedCallback() {
        try {
            OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                    } else {
                        // Remove this callback and let the system handle back press
                        setEnabled(false);
                        getOnBackPressedDispatcher().onBackPressed();
                    }
                }
            };
            getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
            Log.d(TAG, "OnBackPressedCallback setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up OnBackPressedCallback", e);
        }
    }

    @Override
    @Deprecated
    public void onBackPressed() {
        // Fallback for older Android versions
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
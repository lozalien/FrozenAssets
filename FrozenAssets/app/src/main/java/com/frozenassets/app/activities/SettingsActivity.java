package com.frozenassets.app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.activity.OnBackPressedCallback;

import com.frozenassets.app.R;
import com.frozenassets.app.models.InventoryItem;
import com.frozenassets.app.ViewModels.InventoryViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "SettingsActivity";
    private static final int PERMISSION_REQUEST_CODE = 123;
    
    private DrawerLayout drawerLayout;
    private InventoryViewModel viewModel;
    private TextView tvTotalItems;
    private TextView tvLastExport;
    private TextView tvLastImport;
    private ExecutorService executorService;
    
    private ActivityResultLauncher<Intent> exportLauncher;
    private ActivityResultLauncher<Intent> importLauncher;
    private String pendingExportFormat;
    private String pendingImportFormat;

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

            setContentView(R.layout.activity_settings);

            // Initialize executor service
            executorService = Executors.newSingleThreadExecutor();

            // Setup UI components
            setupToolbar();
            setupNavigationDrawer();
            setupViewModel();
            setupViews();
            setupFileActivityResults();

            // Set title
            setTitle(getString(R.string.settings));

            // Setup OnBackPressedCallback for Android 14+ compatibility
            setupOnBackPressedCallback();

            // Load initial data
            loadDataInfo();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error starting settings", Toast.LENGTH_LONG).show();
            finish();
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

    private void setupViews() {
        tvTotalItems = findViewById(R.id.tv_total_items);
        tvLastExport = findViewById(R.id.tv_last_export);
        tvLastImport = findViewById(R.id.tv_last_import);

        MaterialButton btnExportCsv = findViewById(R.id.btn_export_csv);
        MaterialButton btnExportJson = findViewById(R.id.btn_export_json);
        MaterialButton btnImportCsv = findViewById(R.id.btn_import_csv);
        MaterialButton btnImportJson = findViewById(R.id.btn_import_json);

        btnExportCsv.setOnClickListener(v -> exportData("csv"));
        btnExportJson.setOnClickListener(v -> exportData("json"));
        btnImportCsv.setOnClickListener(v -> importData("csv"));
        btnImportJson.setOnClickListener(v -> importData("json"));
    }

    private void setupFileActivityResults() {
        exportLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d(TAG, "Export result received. ResultCode: " + result.getResultCode());
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    Log.d(TAG, "Export URI: " + uri);
                    if (uri != null && pendingExportFormat != null) {
                        performExport(uri, pendingExportFormat);
                    } else {
                        Log.w(TAG, "Export URI is null or no pending format");
                    }
                } else {
                    Log.w(TAG, "Export cancelled or no data returned");
                }
            });

        importLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d(TAG, "Import result received. ResultCode: " + result.getResultCode());
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    Log.d(TAG, "Import URI: " + uri);
                    if (uri != null) {
                        performImport(uri);
                    } else {
                        Log.w(TAG, "Import URI is null");
                        Toast.makeText(this, "No file selected or invalid file", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.w(TAG, "Import cancelled or no data returned. ResultCode: " + result.getResultCode());
                    pendingImportFormat = null; // Clear on cancel/failure
                    if (result.getResultCode() == RESULT_CANCELED) {
                        Toast.makeText(this, "File selection cancelled", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to select file", Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }


    private void setupOnBackPressedCallback() {
        try {
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
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

    private void loadDataInfo() {
        if (viewModel != null) {
            viewModel.getAllItems().observe(this, items -> {
                if (items != null) {
                    tvTotalItems.setText(getString(R.string.total_items_count, items.size()));
                }
            });
        }
    }

    private void exportData(String format) {
        if (!checkPermissions()) {
            requestPermissions();
            return;
        }

        pendingExportFormat = format;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        if ("csv".equals(format)) {
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_TITLE, "frozen_assets_export_" + 
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".csv");
        } else {
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE, "frozen_assets_export_" + 
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".json");
        }

        exportLauncher.launch(intent);
    }

    private void performExport(Uri uri, String format) {
        Toast.makeText(this, getString(R.string.creating_file), Toast.LENGTH_SHORT).show();
        
        executorService.execute(() -> {
            try {
                // Get all items synchronously on background thread
                List<InventoryItem> items = new ArrayList<>();
                viewModel.getAllItems().observe(this, observedItems -> {
                    if (observedItems != null) {
                        items.clear();
                        items.addAll(observedItems);
                        
                        // Perform the actual export
                        try {
                            if ("csv".equals(format)) {
                                exportToCsv(uri, items);
                            } else {
                                exportToJson(uri, items);
                            }
                            
                            runOnUiThread(() -> {
                                Toast.makeText(SettingsActivity.this, 
                                    getString(R.string.export_successful, uri.getPath()), 
                                    Toast.LENGTH_LONG).show();
                                updateLastExportTime();
                            });
                        } catch (Exception e) {
                            Log.e(TAG, "Export failed", e);
                            runOnUiThread(() -> 
                                Toast.makeText(SettingsActivity.this, 
                                    getString(R.string.export_failed, e.getMessage()), 
                                    Toast.LENGTH_LONG).show());
                        }
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Export failed", e);
                runOnUiThread(() -> 
                    Toast.makeText(SettingsActivity.this, 
                        getString(R.string.export_failed, e.getMessage()), 
                        Toast.LENGTH_LONG).show());
            }
        });
    }

    private void exportToCsv(Uri uri, List<InventoryItem> items) throws Exception {
        try (FileWriter writer = new FileWriter(getContentResolver().openFileDescriptor(uri, "w").getFileDescriptor())) {
            // CSV Header
            writer.append("Name,Category,Quantity,Date Frozen,Expiration Date,Notes,Tags,Weight,Weight Unit,Max Freeze Days\n");
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            
            for (InventoryItem item : items) {
                writer.append(escapeCsvField(item.getName())).append(",");
                writer.append(escapeCsvField(item.getCategory())).append(",");
                writer.append(String.valueOf(item.getQuantity())).append(",");
                writer.append(item.getDateFrozen() != null ? dateFormat.format(item.getDateFrozen()) : "").append(",");
                writer.append(item.getExpirationDate() != null ? dateFormat.format(item.getExpirationDate()) : "").append(",");
                writer.append(escapeCsvField(item.getNotes() != null ? item.getNotes() : "")).append(",");
                writer.append(escapeCsvField(item.getTags() != null ? String.join(";", item.getTags()) : "")).append(",");
                writer.append(escapeCsvField(item.getWeight() != null ? item.getWeight() : "")).append(",");
                writer.append(escapeCsvField(item.getWeightUnit() != null ? item.getWeightUnit() : "")).append(",");
                writer.append(String.valueOf(item.getMaxFreezeDays()));
                writer.append("\n");
            }
            writer.flush();
        }
    }

    private void exportToJson(Uri uri, List<InventoryItem> items) throws Exception {
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd").setPrettyPrinting().create();
        String json = gson.toJson(items);
        
        try (FileWriter writer = new FileWriter(getContentResolver().openFileDescriptor(uri, "w").getFileDescriptor())) {
            writer.write(json);
            writer.flush();
        }
    }

    private String escapeCsvField(String field) {
        if (field == null) return "";
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    private void importData(String format) {
        Log.d(TAG, "Starting import for format: " + format);
        
        if (!checkPermissions()) {
            Log.d(TAG, "Permissions not granted, requesting...");
            requestPermissions();
            return;
        }

        pendingImportFormat = format;

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        // Be more flexible with MIME types to handle different file systems
        if ("csv".equals(format)) {
            // Accept multiple MIME types for CSV files
            String[] mimeTypes = {"text/csv", "text/plain", "application/csv", "text/comma-separated-values"};
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            Log.d(TAG, "Configured CSV import with flexible MIME types");
        } else {
            // Accept multiple MIME types for JSON files  
            String[] mimeTypes = {"application/json", "text/json", "text/plain"};
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            Log.d(TAG, "Configured JSON import with flexible MIME types");
        }

        try {
            Log.d(TAG, "Launching import file picker...");
            importLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch file picker", e);
            Toast.makeText(this, "Failed to open file picker: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void performImport(Uri uri) {
        Log.d(TAG, "Starting import from URI: " + uri);
        Log.d(TAG, "URI path: " + uri.getPath());
        Log.d(TAG, "URI scheme: " + uri.getScheme());
        
        Toast.makeText(this, getString(R.string.reading_file), Toast.LENGTH_SHORT).show();
        
        executorService.execute(() -> {
            try {
                List<InventoryItem> items = new ArrayList<>();
                String fileName = uri.getLastPathSegment();
                Log.d(TAG, "File name from URI: " + fileName);
                
                // Use the format that was selected by the user button click
                Log.d(TAG, "Using pending import format: " + pendingImportFormat);
                if ("csv".equals(pendingImportFormat)) {
                    Log.d(TAG, "Attempting CSV import based on user selection...");
                    items = importFromCsv(uri);
                } else if ("json".equals(pendingImportFormat)) {
                    Log.d(TAG, "Attempting JSON import based on user selection...");
                    items = importFromJson(uri);
                } else {
                    // Fallback: try to determine from filename
                    boolean isCsv = false;
                    if (fileName != null) {
                        isCsv = fileName.toLowerCase().endsWith(".csv");
                        Log.d(TAG, "Fallback: determined file type from name - CSV: " + isCsv);
                    } else {
                        Log.d(TAG, "Fallback: no filename available, defaulting to CSV");
                        isCsv = true;
                    }
                    
                    if (isCsv) {
                        Log.d(TAG, "Fallback: attempting CSV import...");
                        items = importFromCsv(uri);
                    } else {
                        Log.d(TAG, "Fallback: attempting JSON import...");
                        items = importFromJson(uri);
                    }
                }
                
                Log.d(TAG, "Import completed successfully. Items found: " + items.size());
                final List<InventoryItem> finalItems = items;
                runOnUiThread(() -> showImportConfirmation(finalItems));
                
                // Clear the pending import format
                pendingImportFormat = null;
                
            } catch (Exception e) {
                Log.e(TAG, "Import failed from URI: " + uri, e);
                pendingImportFormat = null; // Clear on error too
                runOnUiThread(() -> 
                    Toast.makeText(SettingsActivity.this, 
                        getString(R.string.import_failed, e.getMessage()), 
                        Toast.LENGTH_LONG).show());
            }
        });
    }

    private List<InventoryItem> importFromCsv(Uri uri) throws Exception {
        Log.d(TAG, "Starting CSV import from URI: " + uri);
        List<InventoryItem> items = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)))) {
            String header = reader.readLine(); // Skip header
            Log.d(TAG, "CSV Header: " + header);
            
            int lineNumber = 1;
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    Log.d(TAG, "Skipping empty line " + lineNumber);
                    continue;
                }
                
                Log.d(TAG, "Processing line " + lineNumber + ": " + line.substring(0, Math.min(50, line.length())) + "...");
                String[] fields = parseCsvLine(line);
                Log.d(TAG, "Parsed " + fields.length + " fields from line " + lineNumber);
                
                if (fields.length >= 3 && !fields[0].trim().isEmpty() && !fields[1].trim().isEmpty()) {
                    InventoryItem item = new InventoryItem();
                    item.setName(fields[0].trim());
                    item.setCategory(fields[1].trim());
                    
                    // Parse quantity with error handling
                    try {
                        String quantityStr = fields[2].trim();
                        if (quantityStr.isEmpty()) {
                            Log.w(TAG, "Empty quantity field on line " + lineNumber + ", defaulting to 1");
                            item.setQuantity(1);
                        } else {
                            item.setQuantity(Integer.parseInt(quantityStr));
                        }
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Invalid quantity '" + fields[2] + "' on line " + lineNumber + ", defaulting to 1");
                        item.setQuantity(1);
                    }
                    
                    // Parse date frozen
                    if (fields.length > 3 && !fields[3].trim().isEmpty()) {
                        try {
                            item.setDateFrozen(dateFormat.parse(fields[3].trim()));
                        } catch (Exception e) {
                            Log.w(TAG, "Invalid date frozen '" + fields[3] + "' on line " + lineNumber + ", skipping date");
                        }
                    }
                    
                    // Parse expiration date
                    if (fields.length > 4 && !fields[4].trim().isEmpty()) {
                        try {
                            item.setExpirationDate(dateFormat.parse(fields[4].trim()));
                        } catch (Exception e) {
                            Log.w(TAG, "Invalid expiration date '" + fields[4] + "' on line " + lineNumber + ", skipping date");
                        }
                    }
                    if (fields.length > 5) {
                        item.setNotes(fields[5]);
                    }
                    if (fields.length > 6 && !fields[6].isEmpty()) {
                        item.setTags(Arrays.asList(fields[6].split(";")));
                    }
                    if (fields.length > 7) {
                        item.setWeight(fields[7]);
                    }
                    if (fields.length > 8) {
                        item.setWeightUnit(fields[8]);
                    }
                    if (fields.length > 9 && !fields[9].isEmpty()) {
                        try {
                            item.setMaxFreezeDays(Integer.parseInt(fields[9].trim()));
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "Invalid maxFreezeDays '" + fields[9] + "' on line " + lineNumber + ", using default");
                            // Leave default value (will be set by constructor)
                        }
                    }
                    
                    items.add(item);
                    Log.d(TAG, "Successfully added item: " + item.getName());
                } else {
                    Log.w(TAG, "Line " + lineNumber + " has insufficient fields (" + fields.length + "), skipping");
                }
            }
        }
        
        Log.d(TAG, "CSV import completed. Total items: " + items.size());
        return items;
    }

    private List<InventoryItem> importFromJson(Uri uri) throws Exception {
        StringBuilder jsonBuilder = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
        }
        
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd").create();
        Type listType = new TypeToken<List<InventoryItem>>(){}.getType();
        return gson.fromJson(jsonBuilder.toString(), listType);
    }

    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    currentField.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString());
                currentField.setLength(0);
            } else {
                currentField.append(c);
            }
        }
        
        fields.add(currentField.toString());
        return fields.toArray(new String[0]);
    }

    private void showImportConfirmation(List<InventoryItem> items) {
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.import_confirm_title))
            .setMessage(getString(R.string.confirm_import, items.size()))
            .setPositiveButton(getString(R.string.import_action), (dialog, which) -> {
                executorService.execute(() -> {
                    for (InventoryItem item : items) {
                        viewModel.insert(item);
                    }
                    runOnUiThread(() -> {
                        Toast.makeText(SettingsActivity.this, 
                            getString(R.string.import_successful, items.size()), 
                            Toast.LENGTH_LONG).show();
                        updateLastImportTime();
                        loadDataInfo();
                    });
                });
            })
            .setNegativeButton(getString(R.string.cancel), null)
            .show();
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } else {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 
                PERMISSION_REQUEST_CODE);
        }
    }

    private void updateLastExportTime() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        tvLastExport.setText(getString(R.string.last_export_date, timestamp));
    }

    private void updateLastImportTime() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        tvLastImport.setText(getString(R.string.last_import_date, timestamp));
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        try {
            Log.d(TAG, "Navigation item selected: " + item.getTitle());
            int id = item.getItemId();

            if (id == R.id.nav_settings) {
                // Already on settings, do nothing
            } else if (id == R.id.nav_all_items) {
                startActivity(new Intent(this, AllItemsActivity.class));
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
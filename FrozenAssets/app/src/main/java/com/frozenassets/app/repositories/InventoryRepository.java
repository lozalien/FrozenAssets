package com.frozenassets.app.repositories;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.room.Room;

import com.frozenassets.app.database.InventoryDao;
import com.frozenassets.app.database.InventoryDatabase;
import com.frozenassets.app.models.InventoryItem;
import com.frozenassets.app.models.FoodCategory;
import com.frozenassets.app.models.SortOrder;
import com.frozenassets.app.utils.DateUtils;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InventoryRepository {
    private final InventoryDao inventoryDao;
    private final ExecutorService executorService;
    private final LiveData<List<InventoryItem>> allItems;
    private final LiveData<List<String>> allCategories;

    public InventoryRepository(Application application) {
        InventoryDatabase database = null;
        try {
            database = InventoryDatabase.getDatabase(application);
            if (database == null) {
                throw new RuntimeException("Database creation returned null");
            }
        } catch (Exception e) {
            Log.e("InventoryRepository", "Error initializing database", e);
            try {
                // Create a fallback in-memory database
                database = Room.inMemoryDatabaseBuilder(application, InventoryDatabase.class)
                    .allowMainThreadQueries() // Only for emergency fallback
                    .build();
                Log.w("InventoryRepository", "Using in-memory fallback database");
            } catch (Exception fallbackError) {
                Log.e("InventoryRepository", "Failed to create fallback database", fallbackError);
                throw new RuntimeException("Unable to initialize any database", fallbackError);
            }
        }

        try {
            inventoryDao = database.inventoryDao();
            if (inventoryDao == null) {
                throw new RuntimeException("DAO creation returned null");
            }
            
            executorService = Executors.newFixedThreadPool(4);
            allItems = inventoryDao.getAllItems();
            allCategories = inventoryDao.getAllCategories();
            
            Log.d("InventoryRepository", "Repository initialized successfully");
        } catch (Exception e) {
            Log.e("InventoryRepository", "Error setting up repository components", e);
            throw new RuntimeException("Repository initialization failed", e);
        }
    }

    // Get all items
    public LiveData<List<InventoryItem>> getAllItems() {
        return allItems;
    }

    // Get all items with sorting
    public LiveData<List<InventoryItem>> getAllItems(SortOrder sortOrder) {
        switch (sortOrder) {
            case EXPIRATION_DESC:
                return inventoryDao.getAllItemsSortedDesc();
            case EXPIRATION_ASC:
            default:
                return inventoryDao.getAllItems();
        }
    }

    // Get all categories
    public LiveData<List<String>> getAllCategories() {
        return allCategories;
    }

    // Get items by category
    public LiveData<List<InventoryItem>> getItemsByCategory(String category) {
        return inventoryDao.getItemsByCategory(category);
    }

    // Get items by category with sorting
    public LiveData<List<InventoryItem>> getItemsByCategory(String category, SortOrder sortOrder) {
        switch (sortOrder) {
            case EXPIRATION_DESC:
                return inventoryDao.getItemsByCategorySortedDesc(category);
            case EXPIRATION_ASC:
            default:
                return inventoryDao.getItemsByCategory(category);
        }
    }

    // Get items nearing expiration (within two months)
    public LiveData<List<InventoryItem>> getItemsNearingExpiration() {
        Date threshold = DateUtils.getExpirationThreshold();
        return inventoryDao.getItemsNearingExpiration(threshold);
    }

    // Get items nearing expiration with sorting
    public LiveData<List<InventoryItem>> getItemsNearingExpiration(SortOrder sortOrder) {
        Date threshold = DateUtils.getExpirationThreshold();
        switch (sortOrder) {
            case EXPIRATION_DESC:
                return inventoryDao.getItemsNearingExpirationSortedDesc(threshold);
            case EXPIRATION_ASC:
            default:
                return inventoryDao.getItemsNearingExpiration(threshold);
        }
    }

    // Insert item
    public void insert(InventoryItem item) {
        if (item == null) {
            Log.w("InventoryRepository", "Attempted to insert null item");
            return;
        }
        
        try {
            // Calculate expiration date if not set
            if (item.getExpirationDate() == null && item.getDateFrozen() != null) {
                long duration = FoodCategory.getDurationForCategory(item.getCategory());
                item.setExpirationDate(new Date(item.getDateFrozen().getTime() + duration));
            }

            executorService.execute(() -> {
                try {
                    inventoryDao.insert(item);
                } catch (Exception e) {
                    Log.e("InventoryRepository", "Error inserting item: " + item.getName(), e);
                }
            });
        } catch (Exception e) {
            Log.e("InventoryRepository", "Error preparing item for insertion", e);
        }
    }

    // Update item
    public void update(InventoryItem item) {
        if (item == null) {
            Log.w("InventoryRepository", "Attempted to update null item");
            return;
        }
        
        executorService.execute(() -> {
            try {
                inventoryDao.update(item);
            } catch (Exception e) {
                Log.e("InventoryRepository", "Error updating item: " + item.getName(), e);
            }
        });
    }

    // Delete item
    public void delete(InventoryItem item) {
        if (item == null) {
            Log.w("InventoryRepository", "Attempted to delete null item");
            return;
        }
        
        executorService.execute(() -> {
            try {
                inventoryDao.delete(item);
            } catch (Exception e) {
                Log.e("InventoryRepository", "Error deleting item: " + item.getName(), e);
            }
        });
    }

    // Search items
    public LiveData<List<InventoryItem>> searchItems(String query) {
        return inventoryDao.searchItems(query);
    }

    // Get item by ID
    public LiveData<InventoryItem> getItemById(int id) {
        return inventoryDao.getItemById(id);
    }

    // Get item count
    public LiveData<Integer> getItemCount() {
        return inventoryDao.getItemCount();
    }

    // Get item count by category
    public LiveData<Integer> getItemCountByCategory(String category) {
        return inventoryDao.getItemCountByCategory(category);
    }

    // Batch insert items
    public void insertAll(List<InventoryItem> items) {
        executorService.execute(() -> inventoryDao.insertAll(items));
    }
}
package com.frozenassets.app.ViewModels;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.frozenassets.app.models.InventoryItem;
import com.frozenassets.app.repositories.InventoryRepository;

import java.util.List;

public class InventoryViewModel extends AndroidViewModel {
    private final InventoryRepository repository;
    private final LiveData<List<InventoryItem>> allItems;
    private final LiveData<List<String>> allCategories;
    private final LiveData<List<InventoryItem>> expiringItems;

    public InventoryViewModel(Application application) {
        super(application);
        repository = new InventoryRepository(application);
        allItems = repository.getAllItems();
        allCategories = repository.getAllCategories();
        expiringItems = repository.getItemsNearingExpiration();
    }

    // Items operations
    public LiveData<List<InventoryItem>> getAllItems() {
        return allItems;
    }

    public LiveData<List<InventoryItem>> getExpiringItems() {
        return expiringItems;
    }

    public LiveData<List<InventoryItem>> getItemsByCategory(String category) {
        return repository.getItemsByCategory(category);
    }

    public LiveData<InventoryItem> getItemById(int id) {
        return repository.getItemById(id);
    }

    public void insert(InventoryItem item) {
        repository.insert(item);
    }

    public void update(InventoryItem item) {
        repository.update(item);
    }

    public void delete(InventoryItem item) {
        repository.delete(item);
    }

    // Category operations
    public LiveData<List<String>> getAllCategories() {
        return allCategories;
    }

    public LiveData<Integer> getItemCountByCategory(String category) {
        return repository.getItemCountByCategory(category);
    }

    // Search operations
    public LiveData<List<InventoryItem>> searchItems(String query) {
        return repository.searchItems(query);
    }

    // Statistics
    public LiveData<Integer> getItemCount() {
        return repository.getItemCount();
    }

    // Batch operations
    public void insertAll(List<InventoryItem> items) {
        repository.insertAll(items);
    }
}
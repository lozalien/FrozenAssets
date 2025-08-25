package com.frozenassets.app.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.frozenassets.app.models.InventoryItem;

import java.util.Date;
import java.util.List;

@Dao
public interface InventoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(InventoryItem item);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<InventoryItem> items);

    @Update
    void update(InventoryItem item);

    @Delete
    void delete(InventoryItem item);

    @Query("SELECT * FROM inventory_items ORDER BY expirationDate ASC")
    LiveData<List<InventoryItem>> getAllItems();

    @Query("SELECT * FROM inventory_items WHERE category = :category ORDER BY expirationDate ASC")
    LiveData<List<InventoryItem>> getItemsByCategory(String category);

    @Query("SELECT * FROM inventory_items WHERE expirationDate <= :expirationThreshold ORDER BY expirationDate ASC")
    LiveData<List<InventoryItem>> getItemsNearingExpiration(Date expirationThreshold);

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    LiveData<InventoryItem> getItemById(int id);

    @Query("SELECT DISTINCT category FROM inventory_items")
    LiveData<List<String>> getAllCategories();

    @Query("SELECT * FROM inventory_items WHERE name LIKE '%' || :searchQuery || '%' OR category LIKE '%' || :searchQuery || '%'")
    LiveData<List<InventoryItem>> searchItems(String searchQuery);

    @Query("SELECT COUNT(*) FROM inventory_items")
    LiveData<Integer> getItemCount();

    @Query("SELECT COUNT(*) FROM inventory_items WHERE category = :category")
    LiveData<Integer> getItemCountByCategory(String category);

    @Query("UPDATE inventory_items SET notes = :notes WHERE id = :itemId")
    void updateNotes(int itemId, String notes);

   }
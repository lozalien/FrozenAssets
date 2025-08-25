package com.frozenassets.app.database;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.frozenassets.app.models.InventoryItem;
import com.frozenassets.app.models.Tag;
import com.frozenassets.app.utils.DateConverter;
import com.frozenassets.app.utils.ListConverter;

@Database(entities = {InventoryItem.class, Tag.class}, version = 10, exportSchema = true)
@TypeConverters({DateConverter.class, ListConverter.class})
public abstract class InventoryDatabase extends RoomDatabase {
    private static final String TAG = "InventoryDatabase";
    private static final String DATABASE_NAME = "inventory_database";

    public abstract InventoryDao inventoryDao();
    public abstract TagDao tagDao();
    private static volatile InventoryDatabase INSTANCE;

    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Log.d(TAG, "Performing migration from 5 to 6");

            // Create new table with the updated schema
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS inventory_items_new (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "name TEXT NOT NULL, " +
                            "category TEXT NOT NULL, " +
                            "quantity INTEGER NOT NULL DEFAULT 1, " +
                            "notes TEXT, " +
                            "maxFreezeDays INTEGER NOT NULL DEFAULT 180, " +
                            "dateFrozen INTEGER, " +
                            "expirationDate INTEGER, " +
                            "tags TEXT, " +
                            "weight TEXT, " + // Changed from REAL to TEXT
                            "weightUnit TEXT)"
            );

            // Copy existing data
            database.execSQL(
                    "INSERT INTO inventory_items_new (id, name, category, quantity, notes, " +
                            "dateFrozen, expirationDate, tags, weight, weightUnit) " +
                            "SELECT id, name, category, quantity, notes, " +
                            "dateFrozen, expirationDate, tags, " +
                            "CAST(weight AS TEXT), weightUnit FROM inventory_items"
            );

            // Update maxFreezeDays based on category
            database.execSQL(
                    "UPDATE inventory_items_new SET maxFreezeDays = " +
                            "CASE category " +
                            "WHEN 'Chicken' THEN 270 " +
                            "WHEN 'Beef' THEN 365 " +
                            "WHEN 'Pork' THEN 180 " +
                            "WHEN 'Fish' THEN 180 " +
                            "WHEN 'Cooked Meals' THEN 90 " +
                            "WHEN 'Vegetables' THEN 240 " +
                            "WHEN 'Fruits' THEN 240 " +
                            "ELSE 180 END"
            );

            // Drop old table
            database.execSQL("DROP TABLE IF EXISTS inventory_items");

            // Rename new table
            database.execSQL("ALTER TABLE inventory_items_new RENAME TO inventory_items");

            // Create index
            database.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_items_id ON inventory_items(id)");
        }
    };

    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Log.d(TAG, "Performing migration from 6 to 7");
            // Empty migration to handle version bump
        }
    };

    static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Log.d(TAG, "Performing migration from 7 to 8 - Adding tags table");
            
            // Create tags table to match Room entity expectations
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS tags (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "name TEXT, " +
                "isDefault INTEGER NOT NULL)"
            );
            
            // Insert default tags
            String[] defaultTags = {"Raw", "Cooked", "Leftover", "Meal Prep", "Breakfast", 
                                   "Lunch", "Dinner", "Dessert", "Snack", "Organic", 
                                   "Veggie", "Fruit", "Meat", "Seafood"};
            
            for (String tag : defaultTags) {
                database.execSQL("INSERT OR IGNORE INTO tags (name, isDefault) VALUES ('" + tag + "', 1)");
            }
        }
    };
    
    static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Log.d(TAG, "Performing migration from 8 to 9 - Clean migration for tags");
            // Empty migration - tags table should already exist from migration 7->8
            // This is just to handle version increment for schema correction
        }
    };
    
    static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Log.d(TAG, "Performing migration from 9 to 10 - Ensure clean tags table");
            
            // Drop existing tags table if it exists (clean slate)
            database.execSQL("DROP TABLE IF EXISTS tags");
            
            // Create tags table with correct schema
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS tags (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "name TEXT, " +
                "isDefault INTEGER NOT NULL)"
            );
            
            // Insert default tags
            String[] defaultTags = {"Raw", "Cooked", "Leftover", "Meal Prep", "Breakfast", 
                                   "Lunch", "Dinner", "Dessert", "Snack", "Organic", 
                                   "Veggie", "Fruit", "Meat", "Seafood"};
            
            for (String tag : defaultTags) {
                database.execSQL("INSERT INTO tags (name, isDefault) VALUES ('" + tag + "', 1)");
            }
        }
    };

    public static InventoryDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (InventoryDatabase.class) {
                if (INSTANCE == null) {
                    try {
                        INSTANCE = Room.databaseBuilder(
                                        context.getApplicationContext(),
                                        InventoryDatabase.class,
                                        DATABASE_NAME)
                                .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                                .fallbackToDestructiveMigration() // As a last resort
                                .addCallback(new RoomDatabase.Callback() {
                                    @Override
                                    public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                        super.onCreate(db);
                                        Log.d(TAG, "Database created");
                                    }

                                    @Override
                                    public void onOpen(@NonNull SupportSQLiteDatabase db) {
                                        super.onOpen(db);
                                        Log.d(TAG, "Database opened");
                                    }
                                })
                                .build();

                    } catch (Exception e) {
                        Log.e(TAG, "Error creating database", e);
                        throw e;
                    }
                }
            }
        }
        return INSTANCE;
    }

    public static void destroyInstance() {
        INSTANCE = null;
    }
}
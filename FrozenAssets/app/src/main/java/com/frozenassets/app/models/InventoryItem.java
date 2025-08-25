package com.frozenassets.app.models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import androidx.room.Ignore;

import com.frozenassets.app.utils.DateConverter;
import com.frozenassets.app.utils.ListConverter;

import java.util.Date;
import java.util.List;
import java.util.Locale;

@Entity(tableName = "inventory_items", indices = {@Index("id")})
public class InventoryItem {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @NonNull
    @ColumnInfo(name = "name")
    private String name;

    @NonNull
    @ColumnInfo(name = "category")
    private String category;

    @ColumnInfo(name = "quantity")
    private int quantity;

    @ColumnInfo(name = "notes")
    private String notes;

    @ColumnInfo(name = "maxFreezeDays")
    private int maxFreezeDays;

    @TypeConverters(DateConverter.class)
    @ColumnInfo(name = "dateFrozen")
    private Date dateFrozen;

    @TypeConverters(DateConverter.class)
    @ColumnInfo(name = "expirationDate")
    private Date expirationDate;

    @TypeConverters(ListConverter.class)
    @ColumnInfo(name = "tags")
    private List<String> tags;

    @ColumnInfo(name = "weight")
    private String weight;

    @ColumnInfo(name = "weightUnit")
    private String weightUnit;

    // Default constructor for Room
    public InventoryItem() {}

    // Main constructor
    @Ignore
    public InventoryItem(String name, String category, int quantity,
                         Date dateFrozen, Date expirationDate, List<String> tags,
                         String weight, String weightUnit, int maxFreezeDays) {
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.dateFrozen = dateFrozen;
        this.expirationDate = expirationDate;
        this.tags = tags;
        this.weight = weight;
        this.weightUnit = weightUnit;
        this.maxFreezeDays = maxFreezeDays;
    }

    // Constructor without maxFreezeDays
    @Ignore
    public InventoryItem(String name, String category, int quantity,
                         Date dateFrozen, Date expirationDate, List<String> tags,
                         String weight, String weightUnit) {
        this(name, category, quantity, dateFrozen, expirationDate, tags,
                weight, weightUnit,
                (int)(FoodCategory.getDurationForCategory(category) / (24 * 60 * 60 * 1000)));
    }

    // Constructor without weight and unit
    @Ignore
    public InventoryItem(String name, String category, int quantity,
                         Date dateFrozen, Date expirationDate, List<String> tags) {
        this(name, category, quantity, dateFrozen, expirationDate, tags, null, null);
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    @NonNull
    public String getName() { return name; }
    public void setName(@NonNull String name) { this.name = name; }

    @NonNull
    public String getCategory() { return category; }
    public void setCategory(@NonNull String category) { this.category = category; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public int getMaxFreezeDays() { return maxFreezeDays; }
    public void setMaxFreezeDays(int maxFreezeDays) { this.maxFreezeDays = maxFreezeDays; }

    public Date getDateFrozen() { return dateFrozen; }
    public void setDateFrozen(Date dateFrozen) { this.dateFrozen = dateFrozen; }

    public Date getExpirationDate() { return expirationDate; }
    public void setExpirationDate(Date expirationDate) { this.expirationDate = expirationDate; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getWeight() { return weight; }
    public void setWeight(String weight) { this.weight = weight; }

    public String getWeightUnit() { return weightUnit; }
    public void setWeightUnit(String weightUnit) { this.weightUnit = weightUnit; }

    // Helper method to get formatted weight and unit for display
    public String getFormattedWeight() {
        if (weight == null || weightUnit == null) {
            return null;
        }
        return String.format(Locale.getDefault(), "%s %s", weight, weightUnit);
    }

    @Override
    public String toString() {
        return "InventoryItem{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", quantity=" + quantity +
                ", dateFrozen=" + dateFrozen +
                ", expirationDate=" + expirationDate +
                ", tags=" + tags +
                ", weight='" + weight + '\'' +
                ", weightUnit='" + weightUnit + '\'' +
                ", maxFreezeDays=" + maxFreezeDays +
                '}';
    }
}
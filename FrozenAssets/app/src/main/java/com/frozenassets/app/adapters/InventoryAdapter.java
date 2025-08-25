package com.frozenassets.app.adapters;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.frozenassets.app.R;
import com.frozenassets.app.models.InventoryItem;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class InventoryAdapter extends ListAdapter<InventoryItem, InventoryAdapter.InventoryViewHolder> {
    private final OnItemClickListener listener;
    private final OnItemLongClickListener longClickListener;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    
    // Multi-select state management
    private boolean isMultiSelectMode = false;
    private final Set<Integer> selectedItems = new HashSet<>();

    public interface OnItemClickListener {
        void onItemClick(InventoryItem item);
    }
    
    public interface OnItemLongClickListener {
        boolean onItemLongClick(InventoryItem item, int position);
    }

    // Multi-select methods
    public void setMultiSelectMode(boolean enabled) {
        isMultiSelectMode = enabled;
        if (!enabled) {
            selectedItems.clear();
        }
        notifyDataSetChanged();
    }
    
    public boolean isMultiSelectMode() {
        return isMultiSelectMode;
    }
    
    public void toggleItemSelection(int position) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position);
        } else {
            selectedItems.add(position);
        }
        notifyItemChanged(position);
    }
    
    public boolean isItemSelected(int position) {
        return selectedItems.contains(position);
    }
    
    public int getSelectedItemCount() {
        return selectedItems.size();
    }
    
    public Set<Integer> getSelectedPositions() {
        return new HashSet<>(selectedItems);
    }
    
    public void selectAll() {
        selectedItems.clear();
        for (int i = 0; i < getItemCount(); i++) {
            selectedItems.add(i);
        }
        notifyDataSetChanged();
    }
    
    public void deselectAll() {
        selectedItems.clear();
        notifyDataSetChanged();
    }
    
    public void clearSelection() {
        selectedItems.clear();
        isMultiSelectMode = false;
        notifyDataSetChanged();
    }

    public InventoryAdapter(OnItemClickListener listener, OnItemLongClickListener longClickListener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.longClickListener = longClickListener;
    }

    private static final DiffUtil.ItemCallback<InventoryItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<InventoryItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull InventoryItem oldItem, @NonNull InventoryItem newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull InventoryItem oldItem, @NonNull InventoryItem newItem) {
            // Null-safe string comparison
            if (!safeEquals(oldItem.getName(), newItem.getName()) ||
                !safeEquals(oldItem.getCategory(), newItem.getCategory()) ||
                oldItem.getQuantity() != newItem.getQuantity()) {
                return false;
            }
            
            // Null-safe date comparison
            if (!safeEquals(oldItem.getDateFrozen(), newItem.getDateFrozen()) ||
                !safeEquals(oldItem.getExpirationDate(), newItem.getExpirationDate())) {
                return false;
            }
            
            // Null-safe optional field comparison
            return safeEquals(oldItem.getWeight(), newItem.getWeight()) &&
                   safeEquals(oldItem.getNotes(), newItem.getNotes());
        }
        
        private boolean safeEquals(Object obj1, Object obj2) {
            if (obj1 == null && obj2 == null) return true;
            if (obj1 == null || obj2 == null) return false;
            return obj1.equals(obj2);
        }
    };

    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inventory, parent, false);
        return new InventoryViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position) {
        InventoryItem currentItem = getItem(position);
        if (currentItem == null) {
            return;
        }
        MaterialCardView cardView = holder.itemView.findViewById(R.id.item_card);
        TextView countdownView = holder.itemView.findViewById(R.id.text_expiration_countdown);
        CheckBox checkBox = holder.itemView.findViewById(R.id.checkbox_select);
        View selectionOverlay = holder.itemView.findViewById(R.id.selection_overlay);
        View selectionBorder = holder.itemView.findViewById(R.id.selection_border);

        long daysUntilExpiration = getDaysUntilExpiration(currentItem.getExpirationDate());
        int backgroundColor;
        int textColor;

        if (daysUntilExpiration < 0) {
            backgroundColor = ContextCompat.getColor(holder.context, R.color.expiration_expired);
            textColor = ContextCompat.getColor(holder.context, R.color.text_expired);
            countdownView.setText(String.format(Locale.getDefault(), "%d DAYS\nEXPIRED",
                    Math.abs(daysUntilExpiration)));
        } else if (daysUntilExpiration <= 14) {
            backgroundColor = ContextCompat.getColor(holder.context, R.color.expiration_critical);
            textColor = ContextCompat.getColor(holder.context, R.color.text_critical);
            countdownView.setText(String.format(Locale.getDefault(), "EXPIRES IN\n%d DAYS",
                    daysUntilExpiration));
        } else if (daysUntilExpiration <= 60) {
            backgroundColor = ContextCompat.getColor(holder.context, R.color.expiration_warning);
            textColor = ContextCompat.getColor(holder.context, R.color.text_warning);
            countdownView.setText(String.format(Locale.getDefault(), "EXPIRES IN\n%d DAYS",
                    daysUntilExpiration));
        } else {
            backgroundColor = ContextCompat.getColor(holder.context, R.color.expiration_normal);
            textColor = ContextCompat.getColor(holder.context, R.color.text_primary);
            countdownView.setText(String.format(Locale.getDefault(), "EXPIRES IN\n%d DAYS",
                    daysUntilExpiration));
        }

        cardView.setCardBackgroundColor(backgroundColor);
        countdownView.setTextColor(textColor);
        countdownView.setVisibility(View.VISIBLE);
        
        // Handle multi-select UI state
        boolean isSelected = isItemSelected(position);
        if (isMultiSelectMode) {
            checkBox.setVisibility(View.VISIBLE);
            checkBox.setChecked(isSelected);
            selectionOverlay.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            selectionBorder.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        } else {
            checkBox.setVisibility(View.GONE);
            selectionOverlay.setVisibility(View.GONE);
            selectionBorder.setVisibility(View.GONE);
        }
        
        holder.bind(currentItem, listener, longClickListener, position, isMultiSelectMode, isSelected);
    }

    private long getDaysUntilExpiration(Date expirationDate) {
        if (expirationDate == null) {
            return Long.MAX_VALUE; // Treat null as never expires
        }
        long now = System.currentTimeMillis();
        long diff = expirationDate.getTime() - now;
        return TimeUnit.MILLISECONDS.toDays(diff);
    }

    static class InventoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameText;
        private final TextView categoryText;
        private final TextView quantityText;
        private final TextView frozenDateText;
        private final TextView expiryDateText;
        private final TextView expirationCountdown;
        private final LinearLayout backgroundLayout;
        private final CheckBox checkBox;
        private final View selectionOverlay;
        private final View selectionBorder;
        private final Context context;

        public InventoryViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();
            nameText = itemView.findViewById(R.id.text_name);
            categoryText = itemView.findViewById(R.id.text_category);
            quantityText = itemView.findViewById(R.id.text_quantity);
            frozenDateText = itemView.findViewById(R.id.text_frozen_date);
            expiryDateText = itemView.findViewById(R.id.text_expiry_date);
            expirationCountdown = itemView.findViewById(R.id.text_expiration_countdown);
            backgroundLayout = itemView.findViewById(R.id.item_background);
            checkBox = itemView.findViewById(R.id.checkbox_select);
            selectionOverlay = itemView.findViewById(R.id.selection_overlay);
            selectionBorder = itemView.findViewById(R.id.selection_border);
        }

        public void bind(final InventoryItem item, final OnItemClickListener listener, 
                         final OnItemLongClickListener longClickListener, final int position, 
                         final boolean isMultiSelectMode, final boolean isSelected) {
            if (item == null) return;

            try {
                // Setup name with unit
                StringBuilder nameBuilder = new StringBuilder(item.getName() != null ? item.getName() : "Unknown Item");
                if (item.getWeight() != null && !item.getWeight().trim().isEmpty()) {
                    nameBuilder.append(" (").append(item.getWeight()).append(")");
                }
                nameText.setText(nameBuilder.toString());

                categoryText.setText(context.getString(R.string.category_label, 
                    item.getCategory() != null ? item.getCategory() : "Unknown"));
                quantityText.setText(context.getString(R.string.quantity_label, item.getQuantity()));
                
                // Null-safe date formatting
                if (item.getDateFrozen() != null) {
                    frozenDateText.setText(context.getString(R.string.frozen_on, dateFormat.format(item.getDateFrozen())));
                } else {
                    frozenDateText.setText(context.getString(R.string.frozen_on, "Unknown"));
                }
                
                if (item.getExpirationDate() != null) {
                    expiryDateText.setText(context.getString(R.string.expires_on, dateFormat.format(item.getExpirationDate())));
                } else {
                    expiryDateText.setText(context.getString(R.string.expires_on, "Unknown"));
                }

                // Set click listeners based on multi-select mode
                if (isMultiSelectMode) {
                    // In multi-select mode, clicks toggle selection
                    itemView.setOnClickListener(v -> {
                        int adapterPosition = getAdapterPosition();
                        if (adapterPosition != RecyclerView.NO_POSITION) {
                            // Get reference to the adapter instance from the parent
                            RecyclerView recyclerView = (RecyclerView) itemView.getParent();
                            if (recyclerView != null && recyclerView.getAdapter() instanceof InventoryAdapter) {
                                ((InventoryAdapter) recyclerView.getAdapter()).toggleItemSelection(adapterPosition);
                            }
                        }
                    });
                } else {
                    // Normal mode, clicks open item details
                    itemView.setOnClickListener(v -> {
                        if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                            listener.onItemClick(item);
                        }
                    });
                }
                
                // Set long click listener to activate multi-select
                itemView.setOnLongClickListener(v -> {
                    if (longClickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                        return longClickListener.onItemLongClick(item, getAdapterPosition());
                    }
                    return false;
                });
                
            } catch (Exception e) {
                // Log error but don't crash
                android.util.Log.e("InventoryAdapter", "Error binding item: " + item, e);
                nameText.setText("Error loading item");
                categoryText.setText("");
                quantityText.setText("");
                frozenDateText.setText("");
                expiryDateText.setText("");
            }
        }
    }
}
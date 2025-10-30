# Frozen Assets

A modern Android application for managing frozen food inventory, helping you track what's in your freezer and never let food go to waste.

## Overview

Frozen Assets is a comprehensive freezer inventory management app that helps you:
- Track frozen food items with automatic expiration date calculations
- Organize items by categories and custom tags
- Get alerts for items that are expiring soon
- Monitor quantities and weights of stored items
- Search and sort your entire freezer inventory
<br>
<br>
<img width="30%" alt="Screenshot_20251030_160521" src="https://github.com/user-attachments/assets/a48a04bf-c167-4ca0-a433-8a8d1e23be75" />
<br><br>
<img width="30%" alt="Screenshot_20251030_160650" src="https://github.com/user-attachments/assets/5514d229-87e2-4a62-b886-4c4b9821d47f" />
<br><br>
<img width="30%" alt="Screenshot_20251030_160717" src="https://github.com/user-attachments/assets/c583f69b-0e7c-4f80-85a5-eade43b30d93" />
<br><br>
<img width="30%" alt="Screenshot_20251030_161049" src="https://github.com/user-attachments/assets/e38dba81-0e65-4e03-ab70-d8f7238b4d92" />
<br>
<br>

## Features

### Core Functionality
- **Smart Expiration Tracking**: Automatically calculates expiration dates based on freeze date and recommended storage duration
- **Category Management**: Organize items into predefined categories (Chicken, Beef, Pork, Fish, Cooked Meals, Vegetables, Fruits, Other)
- **Custom Tags**: Create and manage custom tags for flexible organization
- **Expiring Soon View**: Quick access to items that need to be consumed soon
- **Quantity & Weight Tracking**: Monitor both count and weight/volume of stored items

### User Interface
- **Material Design 3**: Modern, clean interface following Material Design guidelines
- **Navigation Drawer**: Easy navigation between categories and views
- **Sorting Options**: Sort items by expiration date (ascending/descending)
- **Multi-Select**: Bulk operations for efficient item management
- **Floating Action Button**: Quick access to add new items

### Data Management
- **Local Database**: Room database for reliable offline storage
- **Import/Export**: File permissions for data backup and restore capabilities
- **Bulk Delete**: Select and delete multiple items at once
- **Search Functionality**: Find items quickly across your inventory

## Technical Details

### Built With
- **Language**: Java
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: Room Persistence Library
- **UI Framework**: Material Components for Android

### Key Dependencies
- AndroidX Libraries (AppCompat, RecyclerView, Lifecycle)
- Room Database (v2.6.1)
- Material Components (v1.12.0)
- Firebase Crashlytics & Analytics
- Gson for JSON serialization
- Navigation Component

### Project Structure
```
com.frozenassets.app/
├── activities/          # UI activities
│   ├── MainActivity.java
│   ├── AllItemsActivity.java
│   ├── AddItemActivity.java
│   ├── ItemDetailActivity.java
│   ├── CategoryActivity.java
│   ├── EatSoonActivity.java
│   ├── TagsActivity.java
│   └── SettingsActivity.java
├── adapters/           # RecyclerView adapters
│   ├── InventoryAdapter.java
│   └── TagAdapter.java
├── database/           # Room database components
│   ├── InventoryDatabase.java
│   └── TagDao.java
├── models/            # Data models
│   ├── InventoryItem.java
│   ├── Tag.java
│   ├── SortOrder.java
│   └── FoodCategory.java
├── ViewModels/        # ViewModels for MVVM
│   └── InventoryViewModel.java
└── utils/            # Helper classes
    ├── DateConverter.java
    └── ListConverter.java
```

## Usage

### Adding Items
1. Tap the floating action button (+) on the main screen
2. Enter item details:
   - Name
   - Category
   - Quantity
   - Freeze date
   - Weight (optional)
   - Tags (optional)
3. The app automatically calculates the expiration date based on category

### Managing Tags
1. Open the tags management screen from the Add Item activity
2. Create custom tags for organizing items
3. Use multi-select to delete multiple tags at once
4. Assign tags to items for flexible organization

### Viewing Expiring Items
- The main screen defaults to "Eat Soon" view
- Shows items ordered by expiration date
- Toggle sort order using the sort button in the toolbar

### Categories
Navigate through the drawer menu to view items by category:
- Chicken
- Beef
- Pork
- Fish
- Cooked Meals
- Vegetables
- Fruits
- Other
- All Items

## Database Schema

### InventoryItem
- `id` (Primary Key, Auto-generated)
- `name` (String, Required)
- `category` (String, Required)
- `quantity` (Integer)
- `notes` (String)
- `maxFreezeDays` (Integer)
- `dateFrozen` (Date)
- `expirationDate` (Date)
- `tags` (List<String>)
- `weight` (String)
- `weightUnit` (String)

### Tag
- `id` (Primary Key, Auto-generated)
- `name` (String, Required, Unique)
- `isDefault` (Boolean)



---

**Note**: This app is designed to help reduce food waste by tracking frozen items. Recommended freeze durations are general guidelines - always use your best judgment when consuming frozen foods.

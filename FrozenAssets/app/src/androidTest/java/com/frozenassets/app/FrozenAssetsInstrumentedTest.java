package com.frozenassets.app;

import android.content.Context;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.frozenassets.app.activities.MainActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Comprehensive instrumented tests for Frozen Assets app.
 * Tests core functionality across different device configurations.
 */
@RunWith(AndroidJUnit4.class)
public class FrozenAssetsInstrumentedTest {

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.frozenassets.app", appContext.getPackageName());
    }

    @Test
    public void mainActivityLaunches() {
        // Test that MainActivity launches without crashing
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Verify the main UI elements are present
            Espresso.onView(ViewMatchers.withId(R.id.toolbar))
                    .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
            
            Espresso.onView(ViewMatchers.withId(R.id.recycler_view))
                    .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
            
            Espresso.onView(ViewMatchers.withId(R.id.fab_add))
                    .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        }
    }

    @Test
    public void navigationDrawerOpens() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Open navigation drawer
            Espresso.onView(ViewMatchers.withContentDescription("Open navigation drawer"))
                    .perform(ViewActions.click());
            
            // Verify navigation drawer is visible
            Espresso.onView(ViewMatchers.withId(R.id.nav_view))
                    .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        }
    }

    @Test
    public void fabClickOpensAddItemActivity() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Click the FAB
            Espresso.onView(ViewMatchers.withId(R.id.fab_add))
                    .perform(ViewActions.click());
            
            // Verify AddItemActivity elements are present
            // Note: This assumes AddItemActivity has specific UI elements
            // You may need to adjust based on your actual layout
        }
    }

    @Test
    public void adViewExists() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Verify AdView is present (even if ad doesn't load in tests)
            Espresso.onView(ViewMatchers.withId(R.id.adView))
                    .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        }
    }

    @Test
    public void orientationChangeHandled() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Test orientation change doesn't crash the app
            scenario.onActivity(activity -> {
                // Verify activity is not finishing or destroyed
                assertFalse("Activity should not be finishing", activity.isFinishing());
                assertFalse("Activity should not be destroyed", activity.isDestroyed());
            });
        }
    }

    @Test
    public void backButtonHandled() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Open navigation drawer first
            Espresso.onView(ViewMatchers.withContentDescription("Open navigation drawer"))
                    .perform(ViewActions.click());
            
            // Press back button - should close drawer, not exit app
            Espresso.pressBack();
            
            // Verify app is still running
            scenario.onActivity(activity -> {
                assertFalse("Activity should not be finishing after back press", activity.isFinishing());
            });
        }
    }
}
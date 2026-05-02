package com.fogalarm

import android.content.Intent
import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmActivityTest {

    private fun launchAlarm(): ActivityScenario<AlarmActivity> {
        val intent = Intent(ApplicationProvider.getApplicationContext(), AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("fog_start_ms", System.currentTimeMillis() + 3_600_000L)
        }
        return ActivityScenario.launch(intent)
    }

    @Test
    fun alarmScreenShowsFogAlert() {
        launchAlarm().use {
            onView(withText("FOG ALERT")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun dismissButtonIsVisible() {
        launchAlarm().use {
            onView(withId(R.id.btn_dismiss)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun fogTimeTextIsDisplayed() {
        launchAlarm().use {
            onView(withId(R.id.alarm_fog_time)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun dismissButtonFinishesActivity() {
        launchAlarm().use { scenario ->
            onView(withId(R.id.btn_dismiss)).perform(click())
            scenario.onActivity { activity ->
                assert(activity.isFinishing)
            }
        }
    }

    @Test
    fun activityHasKeepScreenOnFlag() {
        launchAlarm().use { scenario ->
            scenario.onActivity { activity ->
                val flags = activity.window.attributes.flags
                assert(flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0) {
                    "Expected FLAG_KEEP_SCREEN_ON to be set"
                }
            }
        }
    }

    @Test
    fun activityHasShowWhenLockedFlag() {
        launchAlarm().use { scenario ->
            scenario.onActivity { activity ->
                val flags = activity.window.attributes.flags
                @Suppress("DEPRECATION")
                assert(flags and WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED != 0) {
                    "Expected FLAG_SHOW_WHEN_LOCKED to be set"
                }
            }
        }
    }
}

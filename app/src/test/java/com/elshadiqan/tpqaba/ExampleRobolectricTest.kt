package com.elshadiqan.tpqaba

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

  @Test
  fun testAppNameIsCorrect() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("TPQ Abu Bakar Amin", appName)
  }

  @Test
  fun testMainActivityLaunch() {
    try {
      ActivityScenario.launch(MainActivity::class.java).use { scenario ->
        scenario.onActivity { activity ->
          println("MainActivity launched successfully in test!")
        }
      }
    } catch (e: Throwable) {
      e.printStackTrace()
      throw e
    }
  }
}

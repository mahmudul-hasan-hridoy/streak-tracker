package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(instrumentedPackages = ["androidx.loader.content"]) // workaround for some Robolectric versions
class MainActivityTest {

    @Test
    fun testMainActivityStarts() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
    }
}

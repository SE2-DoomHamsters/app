package com.doomhamsters

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@OptIn(ExperimentalCoroutinesApi::class)
class LobbyViewModelFactoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val createdViewModels = mutableListOf<LobbyViewModel>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        createdViewModels.forEach { vm ->
            LobbyViewModel::class.java.getDeclaredMethod("onCleared")
                .apply { isAccessible = true }
                .invoke(vm)
        }
        createdViewModels.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `create throws IllegalArgumentException for incompatible ViewModel class`() {
        val mockContext = mockk<Context>(relaxed = true)
        val factory = LobbyViewModelFactory(mockContext)
        class AnotherViewModel : ViewModel()
        assertThrows<IllegalArgumentException> {
            factory.create(AnotherViewModel::class.java)
        }
    }

    @Test
    fun `create returns a configured LobbyViewModel`() {
        val mockPrefs = mockk<SharedPreferences>(relaxed = true)
        val mockEditor = mockk<SharedPreferences.Editor>(relaxed = true)
        val mockContext = mockk<Context>(relaxed = true)
        every { mockContext.applicationContext } returns mockContext
        every { mockContext.getSharedPreferences(any(), any()) } returns mockPrefs
        every { mockPrefs.getString(any(), any()) } returns null
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor

        val factory = LobbyViewModelFactory(mockContext)
        val vm = factory.create(LobbyViewModel::class.java)

        assertNotNull(vm)
        createdViewModels.add(vm)
    }
}

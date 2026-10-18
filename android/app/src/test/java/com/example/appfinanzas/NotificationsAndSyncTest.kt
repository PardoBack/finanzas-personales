package com.example.appfinanzas

import com.example.appfinanzas.data.UserPreferencesRepository
import com.example.appfinanzas.data.repository.FinancialContext
import com.example.appfinanzas.data.repository.GeminiRepository
import com.example.appfinanzas.data.repository.GoogleDriveSyncRepository
import com.example.appfinanzas.data.repository.SyncResult
import com.example.appfinanzas.domain.model.FinancialNotification
import com.example.appfinanzas.domain.model.NotificationPriority
import com.example.appfinanzas.domain.model.NotificationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MockGeminiRepository : GeminiRepository {
    override suspend fun generateFinancialAdvice(
        userPrompt: String,
        context: FinancialContext
    ): Result<String> {
        return if (userPrompt.isNotBlank()) {
            Result.success("Hola ${context.userName}, tu disponible es ${context.disponibleReal}")
        } else {
            Result.failure(Exception("Empty prompt"))
        }
    }
}

class MockGoogleDriveSyncRepository : GoogleDriveSyncRepository {
    override suspend fun syncAllDataToDrive(): Result<SyncResult> {
        return Result.success(
            SyncResult(
                success = true,
                syncedMovementsCount = 10,
                syncedFixedCount = 3,
                syncedGoalsCount = 2,
                message = "Sincronización exitosa"
            )
        )
    }

    override fun getAppsScriptTemplateCode(): String {
        return "function doPost(e) { return ContentService.createTextOutput('OK'); }"
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsAndSyncTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testFinancialNotificationDataModel() {
        val notification = FinancialNotification(
            id = "test_1",
            title = "🚨 Cuota Próxima",
            message = "Tienes $100.000 pendientes",
            type = NotificationType.FIXED_EXPENSE_DUE,
            priority = NotificationPriority.HIGH,
            actionType = "PAY_FIXED",
            targetId = 1
        )

        assertEquals("test_1", notification.id)
        assertEquals(NotificationType.FIXED_EXPENSE_DUE, notification.type)
        assertEquals(NotificationPriority.HIGH, notification.priority)
        assertEquals(1, notification.targetId)
    }

    @Test
    fun testMockGeminiRepositoryAdvice() = runTest {
        val repo = MockGeminiRepository()
        val context = FinancialContext(userName = "Marlon", disponibleReal = 500000.0)
        val result = repo.generateFinancialAdvice("¿Cómo voy?", context)

        assertTrue(result.isSuccess)
        val text = result.getOrNull()
        assertNotNull(text)
        assertTrue(text!!.contains("Marlon"))
    }

    @Test
    fun testMockGoogleDriveSyncRepository() = runTest {
        val syncRepo = MockGoogleDriveSyncRepository()
        val result = syncRepo.syncAllDataToDrive()

        assertTrue(result.isSuccess)
        val syncRes = result.getOrNull()
        assertEquals(10, syncRes?.syncedMovementsCount)
        assertTrue(syncRepo.getAppsScriptTemplateCode().contains("doPost"))
    }
}

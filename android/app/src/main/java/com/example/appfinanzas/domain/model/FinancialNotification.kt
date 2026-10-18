package com.example.appfinanzas.domain.model

import java.util.UUID

enum class NotificationPriority {
    HIGH, MEDIUM, LOW
}

enum class NotificationType {
    FIXED_EXPENSE_DUE,
    BUDGET_OVERBUDGET,
    ANT_EXPENSE_ALERT,
    AI_RECOMMENDATION
}

data class FinancialNotification(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val type: NotificationType,
    val priority: NotificationPriority = NotificationPriority.MEDIUM,
    val dateText: String = "Ahora",
    val isRead: Boolean = false,
    val actionType: String? = null, // "PAY_FIXED", "NAVIGATE_BUDGETS", "NAVIGATE_AI", "NAVIGATE_MOVEMENTS"
    val targetId: String? = null,
    val targetCategory: String? = null
)

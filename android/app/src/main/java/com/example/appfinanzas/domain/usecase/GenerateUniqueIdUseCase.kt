package com.example.appfinanzas.domain.usecase

import com.example.appfinanzas.data.constants.SheetConstants
import java.util.UUID
import javax.inject.Inject

class GenerateUniqueIdUseCase @Inject constructor() {

    operator fun invoke(prefix: String = SheetConstants.PREFIX_MOVEMENT): String {
        return execute(prefix)
    }

    fun execute(prefix: String = SheetConstants.PREFIX_MOVEMENT): String {
        val timestamp = System.currentTimeMillis()
        val randomPart = UUID.randomUUID().toString().take(6).uppercase()
        return "$prefix$timestamp-$randomPart"
    }
}

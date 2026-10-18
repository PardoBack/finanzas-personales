package com.example.appfinanzas

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.appfinanzas.data.repository.SecurityRepository
import com.example.appfinanzas.ui.FinanceApp
import com.example.appfinanzas.ui.theme.FinanzasProTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var securityRepository: SecurityRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Lock app initially if security is enabled
        lifecycleScope.launch {
            if (securityRepository.isSecurityEnabled.first()) {
                securityRepository.lockApp()
            }
        }

        // Lock app when returning from background
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                lifecycleScope.launch {
                    if (securityRepository.isSecurityEnabled.first()) {
                        securityRepository.lockApp()
                    }
                }
            }
        })

        setContent {
            FinanzasProTheme {
                FinanceApp(securityRepository = securityRepository)
            }
        }
    }
}

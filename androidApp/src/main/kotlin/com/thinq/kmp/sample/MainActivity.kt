package com.thinq.kmp.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.thinq.kmp.sample.repository.LlmRepository
import com.thinq.kmp.sample.ui.ChatScreen
import com.thinq.kmp.sample.ui.theme.KmpLlmSampleTheme
import com.thinq.kmp.sample.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: ChatViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewModel
        val repository = LlmRepository()
        viewModel = ChatViewModel(repository, lifecycleScope)

        setContent {
            KmpLlmSampleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatScreen(viewModel = viewModel)
                }
            }
        }
    }
}

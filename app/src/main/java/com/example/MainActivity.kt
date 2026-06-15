package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: IPTVViewModel = viewModel()
        LocalizedLayout(viewModel = viewModel) {
          val currentStream by viewModel.currentStream.collectAsState()

          Box(modifier = Modifier.fillMaxSize()) {
            if (currentStream != null) {
              // Immersive Video Player
              val activeStream = currentStream!!
              val streamUrl = ApiClient.getStreamUrl(activeStream.streamId)
              
              BackHandler {
                viewModel.selectStream(null)
              }
              
              VideoPlayerView(
                streamUrl = streamUrl,
                channelName = activeStream.name ?: "Live Channel",
                streamId = activeStream.streamId,
                viewModel = viewModel,
                onBack = { viewModel.selectStream(null) },
                modifier = Modifier.fillMaxSize()
              )
            } else {
              // Dashboard and directory browser
              MainUiScreens(
                viewModel = viewModel,
                onPlayStream = { stream ->
                  viewModel.selectStream(stream)
                }
              )
            }
          }
        }
      }
    }
  }
}

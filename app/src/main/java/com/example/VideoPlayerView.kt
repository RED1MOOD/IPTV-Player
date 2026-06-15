package com.example

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.*
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class AspectRatioPreset(val label: String) {
    FIT("Fit"),
    STRETCH("Stretch"),
    ZOOM("Zoom"),
    RATIO_16_9("16:9"),
    RATIO_4_3("4:3")
}

@SuppressLint("ModifierParameter")
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    streamUrl: String,
    channelName: String,
    streamId: Int,
    viewModel: IPTVViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val systemActivity = context as? Activity
    val coroutineScope = rememberCoroutineScope()

    // Keep track of retry attempts for stream reconnection
    var retryCount by remember { mutableStateOf(0) }
    var isReconnecting by remember { mutableStateOf(false) }

    // ExoPlayer state
    var playerInstance by remember { mutableStateOf<ExoPlayer?>(null) }
    var playWhenReadyState by remember { mutableStateOf(true) }
    var isBufferLoading by remember { mutableStateOf(true) }
    var streamErrState by remember { mutableStateOf<String?>(null) }

    // Interface toggles & states
    var showPlayerControls by remember { mutableStateOf(true) }
    var isScreenControlsLocked by remember { mutableStateOf(false) }
    var currentAspectRatioPreset by remember { mutableStateOf(AspectRatioPreset.FIT) }
    
    // Collapsible list sidebar state (inside player)
    var isSidebarOpen by remember { mutableStateOf(false) }

    // Swipe feedback metrics
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxAudioVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    var currentSeekVolume by remember { mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }
    var isVolumeOverlayVisible by remember { mutableStateOf(false) }

    val initialBrightnessValue = remember {
        systemActivity?.window?.attributes?.screenBrightness?.let { if (it < 0) 0.5f else it } ?: 0.5f
    }
    var currentSeekBrightness by remember { mutableStateOf(initialBrightnessValue) }
    var isBrightnessOverlayVisible by remember { mutableStateOf(false) }

    // Track selections
    var showTrackSelectorDialog by remember { mutableStateOf(false) }
    val currentAudioTracks = remember { mutableStateListOf<Pair<Int, String>>() }
    var activeAudioTrackIndex by remember { mutableStateOf(-1) }

    // Advanced Adjustments and Massive IPTV Tools States
    var showAdvancedSettings by remember { mutableStateOf(false) }
    var playbackSpeedSelected by remember { mutableStateOf(1.0f) }
    var maxQualityHeightSelected by remember { mutableStateOf(Int.MAX_VALUE) }
    var sleepTimerSecondsLeft by remember { mutableStateOf<Int?>(null) }
    var audioPitchSelected by remember { mutableStateOf(1.0f) }
    var playerVolumeBoosted by remember { mutableStateOf(1.0f) }

    // Timeline elements
    var currentShowProgram by remember { mutableStateOf<EpgListing?>(null) }
    var currentProgramProgress by remember { mutableStateOf(0f) }

    // Launch side-effects to load EPG details
    LaunchedEffect(streamId, viewModel.currentEpgListings.collectAsState().value) {
        currentShowProgram = viewModel.getCurrentEpgProgram()
        currentProgramProgress = currentShowProgram?.let { viewModel.getProgressPercentage(it) } ?: 0f
    }

    // Keep the screen always on while active and force true full-screen immersion
    DisposableEffect(systemActivity) {
        val window = systemActivity?.window
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            if (window != null) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, true)
                val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Direct Reactive Player Settings Synchronization (Playback Speed & Voice Pitch Tuning)
    LaunchedEffect(playerInstance, playbackSpeedSelected, audioPitchSelected) {
        playerInstance?.let { exo ->
            try {
                exo.playbackParameters = PlaybackParameters(playbackSpeedSelected, audioPitchSelected)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Volume Boost up to 150% level
    LaunchedEffect(playerInstance, playerVolumeBoosted) {
        playerInstance?.let { exo ->
            try {
                exo.volume = playerVolumeBoosted
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Quality Selection by Restricting Max Video Size Constraints (Bitrate limiting)
    LaunchedEffect(playerInstance, maxQualityHeightSelected) {
        playerInstance?.let { exo ->
            try {
                val parameters = exo.trackSelectionParameters
                    .buildUpon()
                    .setMaxVideoSize(
                        if (maxQualityHeightSelected == Int.MAX_VALUE) Int.MAX_VALUE else (maxQualityHeightSelected * 16 / 9),
                        maxQualityHeightSelected
                    )
                    .build()
                exo.trackSelectionParameters = parameters
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Precise Sleep Timer Countdown Executor handles auto-turn off sleep schedule.
    LaunchedEffect(sleepTimerSecondsLeft) {
        val seconds = sleepTimerSecondsLeft ?: return@LaunchedEffect
        if (seconds > 0) {
            delay(1000)
            sleepTimerSecondsLeft = seconds - 1
        } else {
            try {
                playerInstance?.pause()
                playWhenReadyState = false
                sleepTimerSecondsLeft = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Initialize ExoPlayer
    DisposableEffect(streamUrl) {
        isBufferLoading = true
        streamErrState = null
        retryCount = 0
        isReconnecting = false

        val exoPlayer = ExoPlayer.Builder(context)
            .setSeekParameters(SeekParameters.CLOSEST_SYNC)
            .build()
            .apply {
                playWhenReady = playWhenReadyState
                val mediaItem = MediaItem.fromUri(streamUrl)
                setMediaItem(mediaItem)
                prepare()
            }

        val playerListener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBufferLoading = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    retryCount = 0
                    isReconnecting = false
                    streamErrState = null

                    // Enumerate Audio Tracks
                    currentAudioTracks.clear()
                    val tracksGroup = exoPlayer.currentTracks
                    var index = 0
                    for (trackGroup in tracksGroup.groups) {
                        if (trackGroup.type == C.TRACK_TYPE_AUDIO) {
                            for (i in 0 until trackGroup.length) {
                                val format = trackGroup.getTrackFormat(i)
                                val label = format.language?.uppercase() ?: "Track ${index + 1}"
                                currentAudioTracks.add(Pair(index, label))
                                if (trackGroup.isTrackSelected(i)) {
                                    activeAudioTrackIndex = index
                                }
                                index++
                            }
                        }
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (retryCount < 3) {
                    retryCount++
                    isReconnecting = true
                    coroutineScope.launch {
                        delay(2000)
                        exoPlayer.seekTo(exoPlayer.currentPosition)
                        exoPlayer.prepare()
                        exoPlayer.play()
                    }
                } else {
                    isReconnecting = false
                    streamErrState = "Playback error: ${error.localizedMessage} (Retries exhausted)"
                }
            }
        }

        exoPlayer.addListener(playerListener)
        playerInstance = exoPlayer

        onDispose {
            exoPlayer.removeListener(playerListener)
            exoPlayer.release()
            playerInstance = null
        }
    }

    // Dismiss controls timer
    LaunchedEffect(showPlayerControls, isScreenControlsLocked) {
        if (showPlayerControls && !isScreenControlsLocked) {
            delay(5000)
            showPlayerControls = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isScreenControlsLocked) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isVolumeOverlayVisible = false
                        isBrightnessOverlayVisible = false
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            delay(1200)
                            isVolumeOverlayVisible = false
                            isBrightnessOverlayVisible = false
                        }
                    },
                    onDrag = { change, dragAmount ->
                        if (isScreenControlsLocked) return@detectDragGestures
                        change.consume()

                        val isRightSideSwipe = change.position.x > (size.width / 2)
                        val dragFraction = -dragAmount.y / size.height

                        if (isRightSideSwipe) {
                            // volume swipe
                            isVolumeOverlayVisible = true
                            val volStep = dragFraction * maxAudioVolume
                            val targetVol = (currentSeekVolume + volStep).coerceIn(0f, maxAudioVolume.toFloat())
                            currentSeekVolume = targetVol
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol.roundToInt(), 0)
                        } else {
                            // brightness swipe
                            isBrightnessOverlayVisible = true
                            val brightStep = dragFraction
                            val targetBright = (currentSeekBrightness + brightStep).coerceIn(0.01f, 1f)
                            currentSeekBrightness = targetBright
                            systemActivity?.let { activity ->
                                val layoutAttrs = activity.window.attributes
                                layoutAttrs.screenBrightness = targetBright
                                activity.window.attributes = layoutAttrs
                            }
                        }
                    }
                )
            }
            .clickable {
                showPlayerControls = !showPlayerControls
            }
    ) {
        // --- ExoPlayer view component ---
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.player = playerInstance
                
                // Map Custom Aspect Ratio to ExoPlayer ResizeMode
                when (currentAspectRatioPreset) {
                    AspectRatioPreset.FIT -> {
                        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                    AspectRatioPreset.STRETCH -> {
                        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                    }
                    AspectRatioPreset.ZOOM -> {
                        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                    // For fixed ratios we let the player crop/adjust layout bounds
                    AspectRatioPreset.RATIO_16_9, AspectRatioPreset.RATIO_4_3 -> {
                        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (currentAspectRatioPreset == AspectRatioPreset.RATIO_16_9) {
                        Modifier.aspectRatio(16f / 9f)
                    } else if (currentAspectRatioPreset == AspectRatioPreset.RATIO_4_3) {
                        Modifier.aspectRatio(4f / 3f)
                    } else {
                        Modifier
                    }
                )
                .align(Alignment.Center)
        )

        // --- Left Sidebar (Channel switching sidebar without quitting) ---
        AnimatedVisibility(
            visible = isSidebarOpen && !isScreenControlsLocked,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it }),
            modifier = Modifier
                .width(300.dp)
                .fillMaxHeight()
                .background(Color.Black.copy(alpha = 0.85f))
                .align(Alignment.CenterStart)
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "Quick Switcher",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { isSidebarOpen = false }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close switcher", tint = Color.White)
                    }
                }

                // Grid/List of current category channels
                val sidebarStreams = viewModel.filteredStreams.collectAsState().value
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(sidebarStreams) { streamItem ->
                        val isCurrent = streamItem.streamId == streamId
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .clickable {
                                    viewModel.selectStream(streamItem)
                                }
                                .padding(8.dp)
                        ) {
                            AsyncImage(
                                model = streamItem.streamIcon,
                                contentDescription = streamItem.name,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.DarkGray)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = streamItem.name ?: "Channel",
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 14.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // --- Right Sidebar (Advanced Player Options: Playback speed, audio boost, quality limit, sleep clock) ---
        AnimatedVisibility(
            visible = showAdvancedSettings && !isScreenControlsLocked,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier
                .width(320.dp)
                .fillMaxHeight()
                .background(Color(0xFF141218).copy(alpha = 0.95f))
                .align(Alignment.CenterEnd)
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Tune",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Advanced IPTV Tools",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "أدوات التشغيل المتقدمة",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(
                        onClick = { showAdvancedSettings = false },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close advanced options",
                            tint = Color.White
                        )
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(bottom = 16.dp))

                // 1. Playback Speed Selector (مغير السرعة)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Playback Speed | سرعة التشغيل",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val speedsList = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            speedsList.forEach { speed ->
                                val isSelected = playbackSpeedSelected == speed
                                val speedStr = if (speed == 1.0f) "Normal" else "${speed}x"
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else Color.White.copy(alpha = 0.08f)
                                        )
                                        .clickable { playbackSpeedSelected = speed }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = speedStr,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Audio Booster Amplifier (مضخم ومقوي الصوت)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Audio Amplification | مضخم الصوت",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${(playerVolumeBoosted * 100).roundToInt()}%",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (playerVolumeBoosted > 1.0f) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                                contentDescription = "Volume Boost Indicator",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Slider(
                                value = playerVolumeBoosted,
                                onValueChange = { playerVolumeBoosted = it },
                                valueRange = 0.0f..1.5f,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            text = "Reduces quiet audio & boosts output dynamically.",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }
                }

                // 3. Voice Pitch Tuning (نبرة تردد الصوت)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Voice Pitch | نبرة التردد",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val pitchLabel = when {
                                audioPitchSelected < 1.0f -> "Deep عميق"
                                audioPitchSelected > 1.0f -> "Sharp حاد"
                                else -> "Normal عادي"
                            }
                            Text(
                                text = pitchLabel,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Slider(
                            value = audioPitchSelected,
                            onValueChange = { audioPitchSelected = it },
                            valueRange = 0.8f..1.2f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 4. Maximum Quality Constraints (جودة المقطع)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Resolution Limit | جودة البث الأقصى",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val qualityLevelsValues = listOf(
                            Pair(Int.MAX_VALUE, "Auto / تلقائي"),
                            Pair(1080, "1080p FHD"),
                            Pair(720, "720p HD"),
                            Pair(480, "480p SD"),
                            Pair(360, "360p Low")
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            qualityLevelsValues.forEach { qLevel ->
                                val isSelected = maxQualityHeightSelected == qLevel.first
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else Color.White.copy(alpha = 0.04f)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { maxQualityHeightSelected = qLevel.first }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = qLevel.second,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Active resolution",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Intelligent Sleep Timer (مؤقت النوم التلقائي)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sleep Timer | مؤقت النوم",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (sleepTimerSecondsLeft != null) {
                                val mins = sleepTimerSecondsLeft!! / 60
                                val secs = sleepTimerSecondsLeft!! % 60
                                Text(
                                    text = String.format("%02d:%02d", mins, secs),
                                    color = Color.Yellow,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val timerOptionMinutes = listOf(
                            Pair(null, "Off"),
                            Pair(5, "5m"),
                            Pair(15, "15m"),
                            Pair(30, "30m"),
                            Pair(60, "1h"),
                            Pair(120, "2h")
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            timerOptionMinutes.forEach { option ->
                                val isSelected = if (option.first == null) sleepTimerSecondsLeft == null
                                                 else sleepTimerSecondsLeft != null && (sleepTimerSecondsLeft!! / 60) == option.first
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) Color.Yellow
                                            else Color.White.copy(alpha = 0.08f)
                                        )
                                        .clickable {
                                            sleepTimerSecondsLeft = option.first?.times(60)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = option.second,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // 6. Quick Aspect Ratio Switcher (أبعاد الشاشة)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Aspect Ratio | أبعاد الشاشة",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AspectRatioPreset.values().forEach { pr ->
                                val isSelected = currentAspectRatioPreset == pr
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else Color.White.copy(alpha = 0.08f)
                                        )
                                        .clickable { currentAspectRatioPreset = pr }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = pr.label,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Volume Swipe HUD indicator ---
        if (isVolumeOverlayVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (currentSeekVolume == 0f) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                        contentDescription = "Volume Indicator",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(100.dp)
                            .background(Color.White.copy(alpha = 0.3f), CircleShape)
                    ) {
                        val progressFraction = if (maxAudioVolume > 0) currentSeekVolume / maxAudioVolume else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(progressFraction)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }

        // --- Brightness Swipe HUD indicator ---
        if (isBrightnessOverlayVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Brightness6,
                        contentDescription = "Brightness Indicator",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(100.dp)
                            .background(Color.White.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(currentSeekBrightness)
                                .background(Color.Yellow, CircleShape)
                                .align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }

        // --- Buffer status spinner ---
        if (isBufferLoading || isReconnecting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 5.dp)
                    if (isReconnecting) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Auto-reconnecting... ($retryCount/3)",
                            color = Color.Yellow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // --- Stream error card ---
        if (streamErrState != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable { showPlayerControls = true },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .padding(24.dp)
                        .widthIn(max = 400.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error notification",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Streaming Error",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = streamErrState ?: "Unknown",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                playerInstance?.apply {
                                    prepare()
                                    play()
                                }
                                streamErrState = null
                                retryCount = 0
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onError)
                        ) {
                            Text("Retry Connection", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // --- Interactive Screen Overlay Controls ---
        AnimatedVisibility(
            visible = showPlayerControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -40 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -40 }),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.75f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.75f)
                            )
                        )
                    )
            ) {
                // Top controls overlay
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = channelName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        currentShowProgram?.let {
                            Text(
                                text = "Playing: ${it.title ?: "Unknown Program"}",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (!isScreenControlsLocked) {
                        // Quick switch lists trigger button
                        IconButton(
                            onClick = { isSidebarOpen = !isSidebarOpen },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Sidebar selection", tint = Color.White)
                        }

                        // Track / Language selector button
                        IconButton(
                            onClick = { showTrackSelectorDialog = true },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(Icons.Default.Audiotrack, contentDescription = "Audio track", tint = Color.White)
                        }

                        // Aspect ratio selector toggle
                        IconButton(
                            onClick = {
                                val values = AspectRatioPreset.values()
                                val nextIndex = (currentAspectRatioPreset.ordinal + 1) % values.size
                                currentAspectRatioPreset = values[nextIndex]
                            },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(Icons.Default.AspectRatio, contentDescription = "Aspect ratio", tint = Color.White)
                        }

                        // Advanced options toggle (Settings gear)
                        IconButton(
                            onClick = { showAdvancedSettings = !showAdvancedSettings },
                            modifier = Modifier.background(
                                if (showAdvancedSettings) MaterialTheme.colorScheme.primary
                                else Color.Black.copy(alpha = 0.4f),
                                CircleShape
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Advanced Settings",
                                tint = if (showAdvancedSettings) Color.Black else Color.White
                            )
                        }
                    }
                }

                // Middle HUD display labels (locked screen indicators, aspect info)
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!isScreenControlsLocked) {
                        Text(
                            text = "Aspect: ${currentAspectRatioPreset.label}",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Bottom controls panel
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    
                    // --- EPG Timeline Progress details ---
                    if (!isScreenControlsLocked && currentShowProgram != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = viewModel.formatRawTimestamp(currentShowProgram?.startTimestamp),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                            LinearProgressIndicator(
                                progress = { currentProgramProgress },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )
                            Text(
                                text = viewModel.formatRawTimestamp(currentShowProgram?.stopTimestamp),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Lock button
                        IconButton(
                            onClick = {
                                isScreenControlsLocked = !isScreenControlsLocked
                                if (isScreenControlsLocked) {
                                    showPlayerControls = false
                                }
                            },
                            modifier = Modifier.background(
                                if (isScreenControlsLocked) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.4f),
                                CircleShape
                            )
                        ) {
                            Icon(
                                imageVector = if (isScreenControlsLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Lock Controls",
                                tint = if (isScreenControlsLocked) Color.Black else Color.White
                            )
                        }

                        if (!isScreenControlsLocked) {
                            // Video player controls row (Rewind, Play, Fast Forward)
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 10s Rewind
                                IconButton(
                                    onClick = {
                                        playerInstance?.let {
                                            val newPos = (it.currentPosition - 10000).coerceAtLeast(0)
                                            it.seekTo(newPos)
                                        }
                                    },
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp)
                                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = Color.White)
                                }

                                // Play/Pause
                                LargeFloatingActionButton(
                                    onClick = {
                                        playerInstance?.let { exo ->
                                            if (exo.isPlaying) {
                                                exo.pause()
                                                playWhenReadyState = false
                                            } else {
                                                exo.play()
                                                playWhenReadyState = true
                                            }
                                        }
                                    },
                                    shape = CircleShape,
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.Black,
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        imageVector = if (playerInstance?.isPlaying == true) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play state toggle",
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                // 10s Fast forward
                                IconButton(
                                    onClick = {
                                        playerInstance?.let {
                                            val newPos = (it.currentPosition + 10000).coerceAtMost(it.duration)
                                            it.seekTo(newPos)
                                        }
                                    },
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp)
                                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Forward10, contentDescription = "Skip 10s", tint = Color.White)
                                }
                            }

                            // Favorite Toggle Button
                            val currentStreamVal = viewModel.currentStream.collectAsState().value
                            val isFav = currentStreamVal?.let { viewModel.isFavorite(it.streamId) } ?: false
                            IconButton(
                                onClick = {
                                    currentStreamVal?.let { viewModel.toggleFavorite(it) }
                                },
                                modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite Channel Click",
                                    tint = if (isFav) Color.Red else Color.White
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Lock screen persistent mini override
        if (isScreenControlsLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
            ) {
                IconButton(
                    onClick = {
                        isScreenControlsLocked = false
                        showPlayerControls = true
                    },
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(24.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .align(Alignment.TopStart)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Active locked indicator", tint = Color.Black)
                }
            }
        }
    }

    // --- Audio track selection Modal Dialog ---
    if (showTrackSelectorDialog) {
        AlertDialog(
            onDismissRequest = { showTrackSelectorDialog = false },
            title = { Text("Select Audio Stream Language") },
            text = {
                if (currentAudioTracks.isEmpty()) {
                    Text("No alternative tracks present in this stream resource")
                } else {
                    LazyColumn {
                        items(currentAudioTracks) { track ->
                            val isSelectedIdx = track.first == activeAudioTrackIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelectedIdx) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        playerInstance?.let { exo ->
                                            // Select audio track parameter overrides
                                            val parameters = exo.trackSelectionParameters
                                                .buildUpon()
                                                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                                                .build()
                                            exo.trackSelectionParameters = parameters
                                            activeAudioTrackIndex = track.first
                                        }
                                        showTrackSelectorDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = track.second,
                                    fontSize = 16.sp,
                                    fontWeight = if (isSelectedIdx) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSelectedIdx) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTrackSelectorDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

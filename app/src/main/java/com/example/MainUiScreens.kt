package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

enum class NavSection(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    LIVE_TV("Live TV", Icons.Default.LiveTv),
    MOVIES("Movies", Icons.Default.Movie),
    SERIES("Series", Icons.Default.Tv),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun MainUiScreens(
    viewModel: IPTVViewModel,
    onPlayStream: (stream: LiveStream) -> Unit
) {
    val isAuthValid by viewModel.isAuthValid.collectAsState()
    val isAuthLoading by viewModel.isAuthLoading.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isAuthLoading) {
            IPTVLoadingSplashScreen(viewModel)
        } else if (!isAuthValid) {
            IPTVLoginUtilityScreen(viewModel)
        } else {
            IPTVDatabaseMainContent(viewModel, onPlayStream)
        }
    }
}

// --- 1. Splash Screen ---
@Composable
fun IPTVLoadingSplashScreen(viewModel: IPTVViewModel) {
    val error by viewModel.authError.collectAsState()
    val lang by viewModel.appLanguage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Glowing Logo Icon
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "App Logo",
                modifier = Modifier.size(130.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = LanguageTranslation.translate("splash_title", lang),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = LanguageTranslation.translate("splash_sub", lang),
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        if (error == null) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = LanguageTranslation.translate("splash_authenticating", lang),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp
            )
        } else {
            // Authentication/server connection failed. Provide detailed diagnostic message & retry link.
            Text(
                text = error ?: "An error occurred",
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.authenticateAndLoad("Pq47z4Fajgpk", "Yhtawy9aBW8N") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(LanguageTranslation.translate("splash_retry", lang), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- 2. Backup Login Setup ---
@Composable
fun IPTVLoginUtilityScreen(viewModel: IPTVViewModel) {
    var username by remember { mutableStateOf("Pq47z4Fajgpk") }
    var password by remember { mutableStateOf("Yhtawy9aBW8N") }
    var hostUrl by remember { mutableStateOf("http://elattar-tv.org:8080") }
    val errorState by viewModel.authError.collectAsState()
    val lang by viewModel.appLanguage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(androidx.compose.foundation.rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "App Logo",
                modifier = Modifier.size(110.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = LanguageTranslation.translate("login_title", lang),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = LanguageTranslation.translate("login_sub", lang),
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = hostUrl,
            onValueChange = { hostUrl = it },
            label = { Text(LanguageTranslation.translate("login_host_url", lang)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(LanguageTranslation.translate("login_username", lang)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(LanguageTranslation.translate("login_password", lang)) },
            visualTransformation = PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (errorState != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorState ?: "Failed to connect",
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.authenticateAndLoad(username, password) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(LanguageTranslation.translate("login_button", lang), color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

// --- Helper Extension for NavSection ---
fun NavSection.getLocalizedLabel(lang: String): String {
    val key = when (this) {
        NavSection.LIVE_TV -> "live_tv"
        NavSection.MOVIES -> "movies"
        NavSection.SERIES -> "series"
        NavSection.SETTINGS -> "settings"
    }
    return LanguageTranslation.translate(key, lang)
}

// --- 3. Main Dashboard ---
@Composable
fun IPTVDatabaseMainContent(
    viewModel: IPTVViewModel,
    onPlayStream: (stream: LiveStream) -> Unit
) {
    var selectedSection by remember { mutableStateOf(NavSection.LIVE_TV) }
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600
    val lang by viewModel.appLanguage.collectAsState()

    if (isWideScreen) {
        // Responsive Side Navigation Rail
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(
                containerColor = Color(0xFF1D1B20),
                modifier = Modifier.fillMaxHeight()
            ) {
                Spacer(modifier = Modifier.weight(1f))
                NavSection.values().forEach { section ->
                    NavigationRailItem(
                        selected = selectedSection == section,
                        onClick = { selectedSection = section },
                        icon = { Icon(section.icon, contentDescription = section.getLocalizedLabel(lang)) },
                        label = { Text(section.getLocalizedLabel(lang)) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = Color(0xFF1D192B),
                            selectedTextColor = MaterialTheme.colorScheme.secondary,
                            indicatorColor = MaterialTheme.colorScheme.secondary,
                            unselectedIconColor = Color.White.copy(alpha = 0.6f),
                            unselectedTextColor = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                DashboardContentBySection(selectedSection, viewModel, onPlayStream, isWideScreen)
            }
        }
    } else {
        // Portable Phone Bottom Navigation Bar
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF1D1B20)
                ) {
                    NavSection.values().forEach { section ->
                        NavigationBarItem(
                            selected = selectedSection == section,
                            onClick = { selectedSection = section },
                            icon = { Icon(section.icon, contentDescription = section.getLocalizedLabel(lang)) },
                            label = { Text(section.getLocalizedLabel(lang), fontSize = 11.sp, fontWeight = if (selectedSection == section) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF1D192B),
                                selectedTextColor = MaterialTheme.colorScheme.secondary,
                                indicatorColor = MaterialTheme.colorScheme.secondary,
                                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                                unselectedTextColor = Color.White.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                DashboardContentBySection(selectedSection, viewModel, onPlayStream, isWideScreen)
            }
        }
    }
}

@Composable
fun DashboardContentBySection(
    section: NavSection,
    viewModel: IPTVViewModel,
    onPlayStream: (stream: LiveStream) -> Unit,
    isWideScreen: Boolean
) {
    val lang by viewModel.appLanguage.collectAsState()
    when (section) {
        NavSection.LIVE_TV -> LiveTVSectionGrid(viewModel, onPlayStream, isWideScreen)
        NavSection.MOVIES -> CinemaSectionPlaceholder(LanguageTranslation.translate("movies", lang))
        NavSection.SERIES -> CinemaSectionPlaceholder(LanguageTranslation.translate("series", lang))
        NavSection.SETTINGS -> IPTVSettingsPanel(viewModel)
    }
}

// --- Helper functions for translation lookup ---
fun getCategoryDisplayName(category: ChannelCategory, lang: String): String {
    return when (category.categoryId) {
        "-1" -> LanguageTranslation.translate("all_channels", lang)
        "-2" -> LanguageTranslation.translate("favorites", lang)
        "-3" -> LanguageTranslation.translate("last_watched", lang)
        else -> category.categoryName ?: ""
    }
}

// --- 4. Live TV Directory Screens ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LiveTVSectionGrid(
    viewModel: IPTVViewModel,
    onPlayStream: (stream: LiveStream) -> Unit,
    isWideScreen: Boolean
) {
    val categories by viewModel.categories.collectAsState()
    val activeCategory by viewModel.selectedCategory.collectAsState()
    val channelsList by viewModel.filteredStreams.collectAsState()
    val searchInput by viewModel.searchQuery.collectAsState()
    val recentsFlowList by viewModel.recentsList.collectAsState()
    val isLoadingChannels by viewModel.isLoading.collectAsState()
    val lang by viewModel.appLanguage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header & Search
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = LanguageTranslation.translate("live_tv", lang),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Server elattar-tv.org • " + LanguageTranslation.translate("active", lang),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Simple search text field
            OutlinedTextField(
                value = searchInput,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text(LanguageTranslation.translate("search_hint", lang), fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF16171E),
                    unfocusedContainerColor = Color(0xFF16171E),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                ),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                shape = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                modifier = Modifier
                    .width(if (isWideScreen) 280.dp else 160.dp)
                    .height(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recents Row (featured/last watched channels) if any exist
        if (recentsFlowList.isNotEmpty()) {
            Text(
                text = LanguageTranslation.translate("last_watched", lang),
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                items(recentsFlowList) { recent ->
                    Row(
                        modifier = Modifier
                            .width(180.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                            .clickable {
                                val match = viewModel.allStreams.value.firstOrNull { it.streamId == recent.streamId }
                                if (match != null) {
                                    onPlayStream(match)
                                }
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = recent.streamIcon,
                            contentDescription = recent.name,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.DarkGray)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = recent.name,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (isLoadingChannels) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(LanguageTranslation.translate("splash_authenticating", lang), color = Color.White.copy(alpha = 0.5f))
                }
            }
        } else {
            // Adaptive design for grid channels vs categories
            if (isWideScreen) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    // Category Sidebar (Left)
                    Column(
                        modifier = Modifier
                            .width(220.dp)
                            .padding(end = 16.dp)
                    ) {
                        Text(
                            text = if (lang == "ar") "الفئات الرئيسيّة" else "Categories",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(categories) { category ->
                                val isSelected = activeCategory?.categoryId == category.categoryId
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                        .clickable { viewModel.selectCategory(category) }
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = getCategoryDisplayName(category, lang),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    // Main Active Grid (Right)
                    Box(modifier = Modifier.weight(1f)) {
                        ChannelGridList(channelsList, viewModel, onPlayStream, isWideScreen, searchInput)
                    }
                }
            } else {
                // Mobile layout with top horizontal category scroll combined with grid channels below
                ScrollableTabRow(
                    selectedTabIndex = categories.indexOfFirst { it.categoryId == activeCategory?.categoryId }.coerceAtLeast(0),
                    containerColor = Color.Transparent,
                    indicator = { tabPositions ->
                        val currentIdx = categories.indexOfFirst { it.categoryId == activeCategory?.categoryId }.coerceAtLeast(0)
                        if (tabPositions.isNotEmpty() && currentIdx < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[currentIdx]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    divider = {},
                    edgePadding = 0.dp,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    categories.forEach { category ->
                        val isSelected = activeCategory?.categoryId == category.categoryId
                        Tab(
                            selected = isSelected,
                            onClick = { viewModel.selectCategory(category) },
                            text = {
                                Text(
                                    text = getCategoryDisplayName(category, lang),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            }
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    ChannelGridList(channelsList, viewModel, onPlayStream, isWideScreen, searchInput)
                }
            }
        }
    }
}

@Composable
fun ChannelGridList(
    list: List<LiveStream>,
    viewModel: IPTVViewModel,
    onPlayStream: (stream: LiveStream) -> Unit,
    isWideScreen: Boolean,
    searchQuery: String
) {
    val lang by viewModel.appLanguage.collectAsState()
    if (list.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Empty",
                    tint = Color.Gray,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                val emptyMsg = if (searchQuery.isNotBlank()) {
                    LanguageTranslation.translate("no_channels_found", lang) + " '$searchQuery'"
                } else {
                    LanguageTranslation.translate("empty_category", lang)
                }
                Text(emptyMsg, color = Color.White.copy(alpha = 0.4f), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (isWideScreen) 3 else 2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(list, key = { it.streamId }) { streamItem ->
                ChannelGridItem(streamItem, viewModel, onPlayStream)
            }
        }
    }
}

@Composable
fun ChannelGridItem(
    item: LiveStream,
    viewModel: IPTVViewModel,
    onPlay: (stream: LiveStream) -> Unit
) {
    val isFav = viewModel.isFavorite(item.streamId)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay(item) }
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = item.streamIcon,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize()
                )

                // Favorite badge overlay
                IconButton(
                    onClick = { viewModel.toggleFavorite(item) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(28.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite selection",
                        tint = if (isFav) Color.Red else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.name ?: "Unknown channel",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Dynamic progress and subtext EPG indicators
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ID: ${item.streamId}",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.4f)
                )
                Text(
                    text = "TS Stream",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}

// --- 5. Settings Screen ---
@Composable
fun IPTVSettingsPanel(viewModel: IPTVViewModel) {
    val expiryStr by viewModel.accountExpiry.collectAsState()
    val formatsStr by viewModel.allowedFormats.collectAsState()
    val favoritesCount = viewModel.favoritesList.collectAsState().value.size
    val lang by viewModel.appLanguage.collectAsState()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = LanguageTranslation.translate("app_config_panel", lang),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = LanguageTranslation.translate("account_info_sub", lang),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // --- Language Selector Card ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = LanguageTranslation.translate("lang_selection_title", lang),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = LanguageTranslation.translate("lang_selection_sub", lang),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // English option
                        val isEn = lang == "en"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isEn) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f))
                                .border(1.dp, if (isEn) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .clickable { viewModel.setAppLanguage("en") }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "English 🇺🇸",
                                color = if (isEn) MaterialTheme.colorScheme.primary else Color.White,
                                fontWeight = if (isEn) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }

                        // Arabic option
                        val isAr = lang == "ar"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isAr) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f))
                                .border(1.dp, if (isAr) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .clickable { viewModel.setAppLanguage("ar") }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "العربية 🇸🇦",
                                color = if (isAr) MaterialTheme.colorScheme.primary else Color.White,
                                fontWeight = if (isAr) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = LanguageTranslation.translate("account_privileges", lang),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsDetailItem(LanguageTranslation.translate("username", lang), "Pq47z4Fajgpk")
                    SettingsDetailItem(LanguageTranslation.translate("base_server", lang), "http://elattar-tv.org:8080")
                    SettingsDetailItem(LanguageTranslation.translate("expiration_date", lang), expiryStr ?: "Never")
                    SettingsDetailItem(LanguageTranslation.translate("format_protocols", lang), formatsStr ?: "HLS, TS")
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = LanguageTranslation.translate("local_persisted", lang),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsDetailItem(LanguageTranslation.translate("fav_count", lang), "$favoritesCount " + LanguageTranslation.translate("channels_saved", lang))
                    SettingsDetailItem(LanguageTranslation.translate("db_state", lang), LanguageTranslation.translate("db_stable_log", lang))
                }
            }
        }

        item {
            Button(
                onClick = { viewModel.authenticateAndLoad("Pq47z4Fajgpk", "Yhtawy9aBW8N") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(LanguageTranslation.translate("sync_button", lang), color = Color.Black)
            }
        }
    }
}

@Composable
fun SettingsDetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun CinemaSectionPlaceholder(titleName: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.SlowMotionVideo,
                contentDescription = "Feature upcoming",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = titleName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Premium selection lists are being indexed by Xtream API nodes.",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

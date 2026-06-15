package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

class IPTVViewModel(application: Application) : AndroidViewModel(application) {

    private val database = IPTVDatabase.getDatabase(application)
    private val repository = IPTVRepository(database.iptvDao())

    // --- Language Preference Support ---
    private val _appLanguage = MutableStateFlow(getSavedLanguage())
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private fun getSavedLanguage(): String {
        val sp = getApplication<Application>().getSharedPreferences("iptv_prefs", Application.MODE_PRIVATE)
        return sp.getString("app_lang", null) ?: if (Locale.getDefault().language == "ar") "ar" else "en"
    }

    fun setAppLanguage(lang: String) {
        val sp = getApplication<Application>().getSharedPreferences("iptv_prefs", Application.MODE_PRIVATE)
        sp.edit().putString("app_lang", lang).apply()
        _appLanguage.value = lang
    }

    // --- State Observables ---
    private val _isAuthLoading = MutableStateFlow(true)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _isAuthValid = MutableStateFlow(false)
    val isAuthValid: StateFlow<Boolean> = _isAuthValid.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _accountExpiry = MutableStateFlow<String?>("N/A")
    val accountExpiry: StateFlow<String?> = _accountExpiry.asStateFlow()

    private val _allowedFormats = MutableStateFlow<String?>("ts")
    val allowedFormats: StateFlow<String?> = _allowedFormats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _categories = MutableStateFlow<List<ChannelCategory>>(emptyList())
    val categories: StateFlow<List<ChannelCategory>> = _categories.asStateFlow()

    private val _allStreams = MutableStateFlow<List<LiveStream>>(emptyList())
    val allStreams: StateFlow<List<LiveStream>> = _allStreams.asStateFlow()

    private val _selectedCategory = MutableStateFlow<ChannelCategory?>(null)
    val selectedCategory: StateFlow<ChannelCategory?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Room Favorites & Recents observed reactively
    val favoritesList: StateFlow<List<FavoriteChannel>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentsList: StateFlow<List<RecentChannel>> = repository.recents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently playing state
    private val _currentStream = MutableStateFlow<LiveStream?>(null)
    val currentStream: StateFlow<LiveStream?> = _currentStream.asStateFlow()

    private val _currentEpgListings = MutableStateFlow<List<EpgListing>>(emptyList())
    val currentEpgListings: StateFlow<List<EpgListing>> = _currentEpgListings.asStateFlow()

    private val _isEpgLoading = MutableStateFlow(false)
    val isEpgLoading: StateFlow<Boolean> = _isEpgLoading.asStateFlow()

    // Dynamic filtering combo of streams
    val filteredStreams: StateFlow<List<LiveStream>> = combine(
        _allStreams,
        _selectedCategory,
        _searchQuery,
        favoritesList,
        recentsList
    ) { streams, category, query, favs, recents ->
        var list = when (category?.categoryId) {
            "-2" -> { // Favorites
                val favIds = favs.map { it.streamId }.toSet()
                streams.filter { it.streamId in favIds }
            }
            "-3" -> { // Recents
                val recentIds = recents.map { it.streamId }
                // Sort streams based on recents order
                recentIds.mapNotNull { id -> streams.find { it.streamId == id } }
            }
            "-1", null -> streams // All Channels
            else -> streams.filter { it.categoryId == category.categoryId }
        }

        if (query.isNotBlank()) {
            list = list.filter {
                it.name?.lowercase(Locale.ROOT)?.contains(query.lowercase(Locale.ROOT)) == true
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Defined static/virtual categories
    val specialCategories = listOf(
        ChannelCategory("-1", "📺 All Channels", 0),
        ChannelCategory("-2", "⭐ Favorites", 0),
        ChannelCategory("-3", "🕒 Last Watched", 0)
    )

    init {
        // Automatically run authentication checks on launch
        authenticateAndLoad("Pq47z4Fajgpk", "Yhtawy9aBW8N")
    }

    fun authenticateAndLoad(usernameStr: String, passwordStr: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            try {
                val authWrapper = ApiClient.service.authenticate(usernameStr, passwordStr)
                if (authWrapper.userInfo?.auth == 1) {
                    _isAuthValid.value = true
                    
                    // Parse expiry
                    val expirySecs = authWrapper.userInfo.expDate?.toLongOrNull()
                    if (expirySecs != null) {
                        val date = java.util.Date(expirySecs * 1000)
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        _accountExpiry.value = sdf.format(date)
                    } else {
                        _accountExpiry.value = authWrapper.userInfo.status ?: "Active"
                    }

                    // Formats
                    _allowedFormats.value = authWrapper.userInfo.allowedOutputFormats?.joinToString(", ") ?: "ts"
                    
                    // Proceed to load content
                    loadContent(usernameStr, passwordStr)
                } else {
                    _isAuthValid.value = false
                    _authError.value = "Authentication failed: check credentials."
                }
            } catch (e: Exception) {
                _isAuthValid.value = false
                _authError.value = "Connection timeout or server unreachable. Error: ${e.localizedMessage}"
            } finally {
                _isAuthLoading.value = false
            }
        }
    }

    private suspend fun loadContent(usernameStr: String, passwordStr: String) {
        _isLoading.value = true
        try {
            // Fetch live categories
            val apiCats = ApiClient.service.getLiveCategories(usernameStr, passwordStr)
            _categories.value = specialCategories + apiCats
            
            // Set default selected category to "All Channels"
            _selectedCategory.value = specialCategories.first()

            // Fetch live streams
            val apiStreams = ApiClient.service.getLiveStreams(usernameStr, passwordStr)
            _allStreams.value = apiStreams
        } catch (e: Exception) {
            _authError.value = "Failed to load channels: ${e.localizedMessage}"
        } finally {
            _isLoading.value = false
        }
    }

    fun selectCategory(category: ChannelCategory?) {
        _selectedCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- Stream & Player Controls ---
    fun selectStream(stream: LiveStream?) {
        _currentStream.value = stream
        if (stream != null) {
            // Load EPG
            loadStreamEpg(stream.streamId)
            // Add to Recents database
            viewModelScope.launch {
                repository.addRecent(stream)
            }
        }
    }

    fun isCurrentlyPlaying(streamId: Int): Boolean {
        return _currentStream.value?.streamId == streamId
    }

    // Toggle Favorite helper
    fun toggleFavorite(stream: LiveStream) {
        viewModelScope.launch {
            val favsSet = favoritesList.value.map { it.streamId }.toSet()
            if (stream.streamId in favsSet) {
                repository.removeFavorite(stream.streamId)
            } else {
                repository.addFavorite(stream)
            }
        }
    }

    fun isFavorite(streamId: Int): Boolean {
        return favoritesList.value.any { it.streamId == streamId }
    }

    private fun loadStreamEpg(streamId: Int) {
        viewModelScope.launch {
            _isEpgLoading.value = true
            _currentEpgListings.value = emptyList()
            try {
                val epgWrapper = ApiClient.service.getShortEpg("Pq47z4Fajgpk", "Yhtawy9aBW8N", streamId)
                val listings = epgWrapper.epgListings ?: emptyList()
                
                // Decode fields "title" and "description" since they come Base64 encoded from the API
                val decodedListings = listings.map { item ->
                    item.copy(
                        title = Base64Decoder.decode(item.title),
                        description = Base64Decoder.decode(item.description)
                    )
                }
                _currentEpgListings.value = decodedListings
            } catch (e: Exception) {
                // Ignore or handle
                _currentEpgListings.value = emptyList()
            } finally {
                _isEpgLoading.value = false
            }
        }
    }

    // Utility for fetching current show stats
    fun getCurrentEpgProgram(): EpgListing? {
        val now = System.currentTimeMillis() / 1000
        return _currentEpgListings.value.firstOrNull { item ->
            val start = item.startTimestamp?.toLongOrNull() ?: 0L
            val stop = item.stopTimestamp?.toLongOrNull() ?: 0L
            now in start..stop
        }
    }

    fun getProgressPercentage(item: EpgListing): Float {
        val start = item.startTimestamp?.toLongOrNull() ?: return 0f
        val stop = item.stopTimestamp?.toLongOrNull() ?: return 0f
        val now = System.currentTimeMillis() / 1000
        if (now < start) return 0f
        if (now > stop) return 1f
        val total = stop - start
        if (total <= 0L) return 0f
        return (now - start).toFloat() / total
    }

    fun formatRawTimestamp(secondsStr: String?): String {
        val secs = secondsStr?.toLongOrNull() ?: return ""
        val sdf = java.text.SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(java.util.Date(secs * 1000))
    }
}

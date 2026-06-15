package com.example

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface IPTVDao {
    @Query("SELECT * FROM favorite_channels ORDER BY addedAt DESC")
    fun getFavorites(): Flow<List<FavoriteChannel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteChannel)

    @Query("DELETE FROM favorite_channels WHERE streamId = :streamId")
    suspend fun deleteFavorite(streamId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_channels WHERE streamId = :streamId)")
    fun observeIsFavorite(streamId: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_channels WHERE streamId = :streamId)")
    suspend fun isFavorite(streamId: Int): Boolean

    @Query("SELECT * FROM recent_channels ORDER BY watchedAt DESC LIMIT 30")
    fun getRecents(): Flow<List<RecentChannel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecent(recent: RecentChannel)

    @Query("DELETE FROM recent_channels WHERE streamId = :streamId")
    suspend fun deleteRecent(streamId: Int)

    @Query("DELETE FROM recent_channels")
    suspend fun clearRecents()
}

@Database(entities = [FavoriteChannel::class, RecentChannel::class], version = 1, exportSchema = false)
abstract class IPTVDatabase : RoomDatabase() {
    abstract fun iptvDao(): IPTVDao

    companion object {
        @Volatile
        private var INSTANCE: IPTVDatabase? = null

        fun getDatabase(context: Context): IPTVDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    IPTVDatabase::class.java,
                    "iptv_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class IPTVRepository(private val dao: IPTVDao) {
    val favorites: Flow<List<FavoriteChannel>> = dao.getFavorites()
    val recents: Flow<List<RecentChannel>> = dao.getRecents()

    suspend fun addFavorite(channel: LiveStream) {
        dao.insertFavorite(
            FavoriteChannel(
                streamId = channel.streamId,
                name = channel.name ?: "Unknown Channel",
                streamIcon = channel.streamIcon,
                categoryId = channel.categoryId
            )
        )
    }

    suspend fun removeFavorite(streamId: Int) {
        dao.deleteFavorite(streamId)
    }

    fun observeIsFavorite(streamId: Int): Flow<Boolean> = dao.observeIsFavorite(streamId)

    suspend fun isFavorite(streamId: Int): Boolean = dao.isFavorite(streamId)

    suspend fun addRecent(channel: LiveStream) {
        dao.insertRecent(
            RecentChannel(
                streamId = channel.streamId,
                name = channel.name ?: "Unknown Channel",
                streamIcon = channel.streamIcon,
                categoryId = channel.categoryId,
                watchedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeRecent(streamId: Int) {
        dao.deleteRecent(streamId)
    }

    suspend fun clearRecents() {
        dao.clearRecents()
    }
}

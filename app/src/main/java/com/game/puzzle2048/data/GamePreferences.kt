package com.game.puzzle2048.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.game.puzzle2048.model.GameState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 游戏数据持久化
 */
class GamePreferences(private val context: Context) {
    
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "game_2048_prefs")
    
    companion object {
        private val BEST_SCORE = intPreferencesKey("best_score")
        private val TOTAL_GAMES = intPreferencesKey("total_games")
        private val TOTAL_MOVES = intPreferencesKey("total_moves")
        private val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        private val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        private val THEME = stringPreferencesKey("theme")
    }
    
    suspend fun getBestScore(): Int {
        return context.dataStore.data.map { preferences ->
            preferences[BEST_SCORE] ?: 0
        }.first()
    }
    
    suspend fun saveBestScore(score: Int) {
        context.dataStore.edit { preferences ->
            preferences[BEST_SCORE] = score
        }
    }
    
    suspend fun getTotalGames(): Int {
        return context.dataStore.data.map { preferences ->
            preferences[TOTAL_GAMES] ?: 0
        }.first()
    }
    
    suspend fun saveTotalGames(games: Int) {
        context.dataStore.edit { preferences ->
            preferences[TOTAL_GAMES] = games
        }
    }
    
    suspend fun getTotalMoves(): Int {
        return context.dataStore.data.map { preferences ->
            preferences[TOTAL_MOVES] ?: 0
        }.first()
    }
    
    suspend fun saveTotalMoves(moves: Int) {
        context.dataStore.edit { preferences ->
            preferences[TOTAL_MOVES] = moves
        }
    }
    
    suspend fun isSoundEnabled(): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[SOUND_ENABLED] ?: true
        }.first()
    }
    
    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SOUND_ENABLED] = enabled
        }
    }
    
    suspend fun isVibrationEnabled(): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[VIBRATION_ENABLED] ?: true
        }.first()
    }
    
    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VIBRATION_ENABLED] = enabled
        }
    }
    
    suspend fun getTheme(): String {
        return context.dataStore.data.map { preferences ->
            preferences[THEME] ?: "classic"
        }.first()
    }
    
    suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME] = theme
        }
    }
    
    suspend fun saveGameState(state: GameState) {
        // TODO: 实现游戏状态保存
    }
}

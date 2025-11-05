package com.game.puzzle2048.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.game.puzzle2048.data.GamePreferences
import com.game.puzzle2048.engine.GameEngine
import com.game.puzzle2048.model.Direction
import com.game.puzzle2048.model.GameState
import kotlinx.coroutines.launch

/**
 * 游戏 ViewModel
 */
class GameViewModel(private val preferences: GamePreferences) : ViewModel() {
    
    private val gameEngine = GameEngine()
    
    private val _gameState = MutableLiveData<GameState>()
    val gameState: LiveData<GameState> = _gameState
    
    private val _showWinDialog = MutableLiveData<Boolean>()
    val showWinDialog: LiveData<Boolean> = _showWinDialog
    
    private val _showGameOverDialog = MutableLiveData<Boolean>()
    val showGameOverDialog: LiveData<Boolean> = _showGameOverDialog
    
    private val _canUndo = MutableLiveData<Boolean>()
    val canUndo: LiveData<Boolean> = _canUndo
    
    private val _hint = MutableLiveData<Direction?>()
    val hint: LiveData<Direction?> = _hint
    
    init {
        viewModelScope.launch {
            val bestScore = preferences.getBestScore()
            gameEngine.setBestScore(bestScore)
            updateGameState()
        }
    }
    
    fun move(direction: Direction) {
        val moved = gameEngine.move(direction)
        if (moved) {
            updateGameState()
            
            val state = gameEngine.getGameState()
            
            // 保存最高分
            if (state.score > state.bestScore) {
                viewModelScope.launch {
                    preferences.saveBestScore(state.score)
                }
            }
            
            // 检查游戏状态
            if (state.isWon && !state.isGameOver) {
                _showWinDialog.value = true
            } else if (state.isGameOver) {
                _showGameOverDialog.value = true
                
                // 保存游戏统计
                viewModelScope.launch {
                    val totalGames = preferences.getTotalGames() + 1
                    preferences.saveTotalGames(totalGames)
                    
                    val totalMoves = preferences.getTotalMoves() + state.moveCount
                    preferences.saveTotalMoves(totalMoves)
                    
                    if (state.score > preferences.getBestScore()) {
                        preferences.saveBestScore(state.score)
                    }
                }
            }
            
            // 自动保存游戏
            saveGame()
        }
        
        _canUndo.value = gameEngine.canUndo()
    }
    
    fun restart() {
        gameEngine.restart()
        updateGameState()
        _canUndo.value = false
        saveGame()
    }
    
    fun undo() {
        if (gameEngine.undo()) {
            updateGameState()
            _canUndo.value = gameEngine.canUndo()
            saveGame()
        }
    }
    
    fun keepPlaying() {
        gameEngine.keepPlaying()
        updateGameState()
        _showWinDialog.value = false
    }
    
    fun getHint() {
        val direction = gameEngine.getHint()
        _hint.value = direction
    }
    
    fun clearHint() {
        _hint.value = null
    }
    
    private fun updateGameState() {
        _gameState.value = gameEngine.getGameState()
    }
    
    private fun saveGame() {
        viewModelScope.launch {
            val state = gameEngine.getGameState()
            preferences.saveGameState(state)
        }
    }
    
    fun loadGame() {
        viewModelScope.launch {
            // TODO: 实现游戏状态加载
            updateGameState()
        }
    }
}

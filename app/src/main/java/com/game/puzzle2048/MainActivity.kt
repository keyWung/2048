package com.game.puzzle2048

import android.content.Context
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.game.puzzle2048.data.GamePreferences
import com.game.puzzle2048.databinding.ActivityMainBinding
import com.game.puzzle2048.model.Direction
import com.game.puzzle2048.viewmodel.GameViewModel
import com.game.puzzle2048.viewmodel.GameViewModelFactory
import kotlinx.coroutines.launch

/**
 * 主界面 Activity
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: GameViewModel
    private lateinit var preferences: GamePreferences
    private var vibrator: Vibrator? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupViewModel()
        setupViews()
        setupObservers()
        
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "2048"
    }
    
    private fun setupViewModel() {
        preferences = GamePreferences(this)
        val factory = GameViewModelFactory(preferences)
        viewModel = ViewModelProvider(this, factory)[GameViewModel::class.java]
    }
    
    private fun setupViews() {
        // 设置游戏视图监听
        binding.gameView.setOnMoveListener { direction ->
            viewModel.move(direction)
            vibrate()
        }
        
        // 重新开始按钮
        binding.btnRestart.setOnClickListener {
            showRestartDialog()
        }
        
        // 撤销按钮
        binding.btnUndo.setOnClickListener {
            viewModel.undo()
            vibrate()
        }
        
        // 提示按钮
        binding.btnHint.setOnClickListener {
            viewModel.getHint()
        }
    }
    
    private fun setupObservers() {
        // 观察游戏状态
        viewModel.gameState.observe(this) { state ->
            binding.gameView.updateGameState(state)
            binding.tvScore.text = state.score.toString()
            binding.tvBestScore.text = state.bestScore.toString()
            binding.tvMoves.text = "移动: ${state.moveCount}"
        }
        
        // 观察撤销按钮状态
        viewModel.canUndo.observe(this) { canUndo ->
            binding.btnUndo.isEnabled = canUndo
            binding.btnUndo.alpha = if (canUndo) 1.0f else 0.5f
        }
        
        // 观察胜利对话框
        viewModel.showWinDialog.observe(this) { show ->
            if (show) {
                showWinDialog()
            }
        }
        
        // 观察游戏结束对话框
        viewModel.showGameOverDialog.observe(this) { show ->
            if (show) {
                showGameOverDialog()
            }
        }
        
        // 观察提示
        viewModel.hint.observe(this) { direction ->
            direction?.let {
                showHint(it)
                viewModel.clearHint()
            }
        }
    }
    
    private fun showRestartDialog() {
        AlertDialog.Builder(this)
            .setTitle("重新开始")
            .setMessage("确定要重新开始游戏吗？当前进度将丢失。")
            .setPositiveButton("确定") { _, _ ->
                viewModel.restart()
                vibrate()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showWinDialog() {
        AlertDialog.Builder(this)
            .setTitle("🎉 恭喜！")
            .setMessage("你达到了 2048！\n\n要继续挑战更高分数吗？")
            .setPositiveButton("继续游戏") { _, _ ->
                viewModel.keepPlaying()
            }
            .setNegativeButton("重新开始") { _, _ ->
                viewModel.restart()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showGameOverDialog() {
        val state = viewModel.gameState.value
        AlertDialog.Builder(this)
            .setTitle("游戏结束")
            .setMessage("得分: ${state?.score ?: 0}\n移动次数: ${state?.moveCount ?: 0}")
            .setPositiveButton("重新开始") { _, _ ->
                viewModel.restart()
            }
            .setNegativeButton("取消", null)
            .setCancelable(false)
            .show()
    }
    
    private fun showHint(direction: Direction) {
        val directionText = when (direction) {
            Direction.UP -> "向上 ↑"
            Direction.DOWN -> "向下 ↓"
            Direction.LEFT -> "向左 ←"
            Direction.RIGHT -> "向右 →"
        }
        Toast.makeText(this, "建议: $directionText", Toast.LENGTH_SHORT).show()
    }
    
    private fun vibrate() {
        lifecycleScope.launch {
            if (preferences.isVibrationEnabled()) {
                vibrator?.let {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        it.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(50)
                    }
                }
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_statistics -> {
                showStatistics()
                true
            }
            R.id.action_settings -> {
                showSettings()
                true
            }
            R.id.action_about -> {
                showAbout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showStatistics() {
        lifecycleScope.launch {
            val totalGames = preferences.getTotalGames()
            val totalMoves = preferences.getTotalMoves()
            val bestScore = preferences.getBestScore()
            val avgMoves = if (totalGames > 0) totalMoves / totalGames else 0
            
            AlertDialog.Builder(this@MainActivity)
                .setTitle("游戏统计")
                .setMessage(
                    """
                    总游戏次数: $totalGames
                    总移动次数: $totalMoves
                    平均移动次数: $avgMoves
                    最高分: $bestScore
                    """.trimIndent()
                )
                .setPositiveButton("确定", null)
                .show()
        }
    }
    
    private fun showSettings() {
        lifecycleScope.launch {
            val soundEnabled = preferences.isSoundEnabled()
            val vibrationEnabled = preferences.isVibrationEnabled()
            
            val items = arrayOf("音效", "震动")
            val checkedItems = booleanArrayOf(soundEnabled, vibrationEnabled)
            
            AlertDialog.Builder(this@MainActivity)
                .setTitle("设置")
                .setMultiChoiceItems(items, checkedItems) { _, which, isChecked ->
                    lifecycleScope.launch {
                        when (which) {
                            0 -> preferences.setSoundEnabled(isChecked)
                            1 -> preferences.setVibrationEnabled(isChecked)
                        }
                    }
                }
                .setPositiveButton("确定", null)
                .show()
        }
    }
    
    private fun showAbout() {
        AlertDialog.Builder(this)
            .setTitle("关于 2048")
            .setMessage(
                """
                版本: 1.0.0
                
                经典 2048 益智游戏
                
                玩法:
                • 滑动屏幕移动方块
                • 相同数字的方块会合并
                • 达到 2048 即可获胜
                
                高级功能:
                • 撤销功能
                • 智能提示
                • 自动保存
                • 游戏统计
                
                © 2024 Game2048
                """.trimIndent()
            )
            .setPositiveButton("确定", null)
            .show()
    }
}

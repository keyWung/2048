package com.game.puzzle2048.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import com.game.puzzle2048.model.Direction
import com.game.puzzle2048.model.GameState
import com.game.puzzle2048.model.Tile
import kotlin.math.abs
import kotlin.math.min

/**
 * 自定义 2048 游戏视图
 * 支持手势操作和精美动画
 */
class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var gameState: GameState? = null
    private var onMoveListener: ((Direction) -> Unit)? = null
    
    // 绘制相关
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // 尺寸相关
    private var cellSize = 0f
    private var cellMargin = 0f
    private var gridSize = 4
    private var gridStartX = 0f
    private var gridStartY = 0f
    
    // 动画相关
    private val animatingTiles = mutableMapOf<String, TileAnimation>()
    private var isAnimating = false
    
    // 手势检测
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true
        
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null) return false
            
            val diffX = e2.x - e1.x
            val diffY = e2.y - e1.y
            
            if (abs(diffX) > abs(diffY)) {
                // 水平滑动
                if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        onMoveListener?.invoke(Direction.RIGHT)
                    } else {
                        onMoveListener?.invoke(Direction.LEFT)
                    }
                    return true
                }
            } else {
                // 垂直滑动
                if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        onMoveListener?.invoke(Direction.DOWN)
                    } else {
                        onMoveListener?.invoke(Direction.UP)
                    }
                    return true
                }
            }
            return false
        }
    })
    
    init {
        setupPaints()
    }
    
    private fun setupPaints() {
        gridPaint.color = Color.parseColor("#BBADA0")
        gridPaint.style = Paint.Style.FILL
        
        backgroundPaint.color = Color.parseColor("#FAF8EF")
        backgroundPaint.style = Paint.Style.FILL
        
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateDimensions()
    }
    
    private fun calculateDimensions() {
        val size = min(width, height).toFloat()
        val padding = size * 0.05f
        cellMargin = size * 0.015f
        
        val gridWidth = size - padding * 2
        cellSize = (gridWidth - cellMargin * (gridSize + 1)) / gridSize
        
        gridStartX = (width - gridWidth) / 2f
        gridStartY = (height - gridWidth) / 2f
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // 绘制背景
        canvas.drawColor(Color.parseColor("#FAF8EF"))
        
        // 绘制网格背景
        drawGridBackground(canvas)
        
        // 绘制方块
        gameState?.let { state ->
            drawTiles(canvas, state.grid)
        }
    }
    
    private fun drawGridBackground(canvas: Canvas) {
        val cornerRadius = cellSize * 0.05f
        
        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                val left = gridStartX + cellMargin + j * (cellSize + cellMargin)
                val top = gridStartY + cellMargin + i * (cellSize + cellMargin)
                
                val rect = RectF(left, top, left + cellSize, top + cellSize)
                gridPaint.color = Color.parseColor("#CDC1B4")
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, gridPaint)
            }
        }
    }
    
    private fun drawTiles(canvas: Canvas, grid: Array<Array<Tile>>) {
        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                val tile = grid[i][j]
                if (!tile.isEmpty()) {
                    drawTile(canvas, tile)
                }
            }
        }
    }
    
    private fun drawTile(canvas: Canvas, tile: Tile) {
        val key = "${tile.row}-${tile.col}"
        val animation = animatingTiles[key]
        
        val scale = animation?.scale ?: 1f
        val alpha = animation?.alpha ?: 255
        
        val left = gridStartX + cellMargin + tile.col * (cellSize + cellMargin)
        val top = gridStartY + cellMargin + tile.row * (cellSize + cellMargin)
        
        val centerX = left + cellSize / 2
        val centerY = top + cellSize / 2
        
        val scaledSize = cellSize * scale
        val scaledLeft = centerX - scaledSize / 2
        val scaledTop = centerY - scaledSize / 2
        
        val rect = RectF(
            scaledLeft,
            scaledTop,
            scaledLeft + scaledSize,
            scaledTop + scaledSize
        )
        
        val cornerRadius = scaledSize * 0.05f
        
        // 绘制方块背景
        tilePaint.color = getTileColor(tile.value)
        tilePaint.alpha = alpha
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, tilePaint)
        
        // 绘制数字
        textPaint.color = getTextColor(tile.value)
        textPaint.alpha = alpha
        textPaint.textSize = getTextSize(tile.value, scaledSize)
        
        val text = tile.value.toString()
        val textY = centerY - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(text, centerX, textY, textPaint)
    }
    
    private fun getTileColor(value: Int): Int {
        return when (value) {
            2 -> Color.parseColor("#EEE4DA")
            4 -> Color.parseColor("#EDE0C8")
            8 -> Color.parseColor("#F2B179")
            16 -> Color.parseColor("#F59563")
            32 -> Color.parseColor("#F67C5F")
            64 -> Color.parseColor("#F65E3B")
            128 -> Color.parseColor("#EDCF72")
            256 -> Color.parseColor("#EDCC61")
            512 -> Color.parseColor("#EDC850")
            1024 -> Color.parseColor("#EDC53F")
            2048 -> Color.parseColor("#EDC22E")
            else -> Color.parseColor("#3C3A32")
        }
    }
    
    private fun getTextColor(value: Int): Int {
        return if (value <= 4) {
            Color.parseColor("#776E65")
        } else {
            Color.parseColor("#F9F6F2")
        }
    }
    
    private fun getTextSize(value: Int, cellSize: Float): Float {
        return when {
            value < 100 -> cellSize * 0.5f
            value < 1000 -> cellSize * 0.45f
            value < 10000 -> cellSize * 0.4f
            else -> cellSize * 0.35f
        }
    }
    
    /**
     * 更新游戏状态并触发动画
     */
    fun updateGameState(newState: GameState, animate: Boolean = true) {
        val oldState = gameState
        gameState = newState
        
        if (animate && oldState != null) {
            animateChanges(oldState, newState)
        } else {
            invalidate()
        }
    }
    
    private fun animateChanges(oldState: GameState, newState: GameState) {
        animatingTiles.clear()
        
        // 为新方块添加弹出动画
        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                val tile = newState.grid[i][j]
                if (!tile.isEmpty() && tile.isNew) {
                    val key = "${tile.row}-${tile.col}"
                    startPopAnimation(key)
                }
            }
        }
        
        if (animatingTiles.isEmpty()) {
            invalidate()
        }
    }
    
    private fun startPopAnimation(key: String) {
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 200
        animator.interpolator = OvershootInterpolator()
        
        val animation = TileAnimation()
        animatingTiles[key] = animation
        
        animator.addUpdateListener { valueAnimator ->
            val value = valueAnimator.animatedValue as Float
            animation.scale = value
            animation.alpha = (value * 255).toInt()
            invalidate()
        }
        
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                animatingTiles.remove(key)
            }
        })
        
        animator.start()
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }
    
    fun setOnMoveListener(listener: (Direction) -> Unit) {
        onMoveListener = listener
    }
    
    fun setGridSize(size: Int) {
        gridSize = size
        calculateDimensions()
        invalidate()
    }
    
    private data class TileAnimation(
        var scale: Float = 1f,
        var alpha: Int = 255
    )
    
    companion object {
        private const val SWIPE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100
    }
}

package com.game.puzzle2048.engine

import com.game.puzzle2048.model.Direction
import com.game.puzzle2048.model.GameState
import com.game.puzzle2048.model.Tile
import kotlin.random.Random

/**
 * 2048 游戏核心引擎
 * 支持基础玩法和高级特性：
 * - 标准 4x4 网格
 * - 撤销功能
 * - 自动保存
 * - 游戏统计
 */
class GameEngine(private val gridSize: Int = 4) {
    
    private var grid: Array<Array<Tile>> = Array(gridSize) { row ->
        Array(gridSize) { col ->
            Tile(0, row, col)
        }
    }
    
    private var score: Int = 0
    private var bestScore: Int = 0
    private var moveCount: Int = 0
    private var isGameOver: Boolean = false
    private var isWon: Boolean = false
    private var hasWonBefore: Boolean = false
    
    // 撤销功能：保存上一步状态
    private val history = mutableListOf<GameSnapshot>()
    private val maxHistorySize = 5
    
    data class GameSnapshot(
        val grid: Array<Array<Tile>>,
        val score: Int,
        val moveCount: Int
    )
    
    init {
        initGame()
    }
    
    /**
     * 初始化游戏
     */
    fun initGame() {
        grid = Array(gridSize) { row ->
            Array(gridSize) { col ->
                Tile(0, row, col)
            }
        }
        score = 0
        moveCount = 0
        isGameOver = false
        isWon = false
        history.clear()
        
        // 添加两个初始方块
        addRandomTile()
        addRandomTile()
    }
    
    /**
     * 重新开始游戏
     */
    fun restart() {
        initGame()
    }
    
    /**
     * 执行移动
     */
    fun move(direction: Direction): Boolean {
        if (isGameOver) return false
        
        // 保存当前状态用于撤销
        saveSnapshot()
        
        var moved = false
        val vector = getVector(direction)
        val traversals = buildTraversals(direction)
        
        // 清除合并标记
        prepareTiles()
        
        // 遍历网格并移动方块
        for (x in traversals.x) {
            for (y in traversals.y) {
                val tile = grid[x][y]
                if (tile.isEmpty()) continue
                
                val positions = findFarthestPosition(x, y, vector)
                val next = if (positions.next != null) {
                    grid[positions.next.first][positions.next.second]
                } else null
                
                // 检查是否可以合并
                if (next != null && !next.isEmpty() && next.value == tile.value && next.mergedFrom == null) {
                    val merged = Tile(tile.value * 2, positions.next!!.first, positions.next.second)
                    merged.mergedFrom = Pair(tile, next)
                    
                    grid[merged.row][merged.col] = merged
                    grid[tile.row][tile.col] = Tile(0, tile.row, tile.col)
                    
                    score += merged.value
                    
                    // 检查是否达到 2048
                    if (merged.value == 2048 && !hasWonBefore) {
                        isWon = true
                        hasWonBefore = true
                    }
                    
                    moved = true
                } else {
                    // 移动到最远位置
                    moveTile(tile, positions.farthest.first, positions.farthest.second)
                    if (tile.row != x || tile.col != y) {
                        moved = true
                    }
                }
            }
        }
        
        if (moved) {
            addRandomTile()
            moveCount++
            
            if (!movesAvailable()) {
                isGameOver = true
            }
            
            if (score > bestScore) {
                bestScore = score
            }
        } else {
            // 如果没有移动，移除刚才保存的快照
            if (history.isNotEmpty()) {
                history.removeAt(history.size - 1)
            }
        }
        
        return moved
    }
    
    /**
     * 撤销上一步
     */
    fun undo(): Boolean {
        if (history.isEmpty()) return false
        
        val snapshot = history.removeAt(history.size - 1)
        grid = snapshot.grid.map { row ->
            row.map { it.copy() }.toTypedArray()
        }.toTypedArray()
        score = snapshot.score
        moveCount = snapshot.moveCount
        isGameOver = false
        
        return true
    }
    
    /**
     * 获取提示（找到最佳移动方向）
     */
    fun getHint(): Direction? {
        val directions = listOf(Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT)
        var bestDirection: Direction? = null
        var maxScore = -1
        
        for (direction in directions) {
            val testEngine = clone()
            if (testEngine.move(direction)) {
                val score = testEngine.evaluateBoard()
                if (score > maxScore) {
                    maxScore = score
                    bestDirection = direction
                }
            }
        }
        
        return bestDirection
    }
    
    /**
     * 评估当前棋盘状态（用于提示功能）
     */
    private fun evaluateBoard(): Int {
        var score = 0
        
        // 空格数量
        val emptyCount = grid.flatten().count { it.isEmpty() }
        score += emptyCount * 100
        
        // 单调性（相邻方块值相近更好）
        for (i in 0 until gridSize) {
            for (j in 0 until gridSize - 1) {
                if (!grid[i][j].isEmpty() && !grid[i][j + 1].isEmpty()) {
                    score -= Math.abs(grid[i][j].value - grid[i][j + 1].value)
                }
                if (!grid[j][i].isEmpty() && !grid[j + 1][i].isEmpty()) {
                    score -= Math.abs(grid[j][i].value - grid[j + 1][i].value)
                }
            }
        }
        
        // 最大值在角落
        val corners = listOf(
            grid[0][0], grid[0][gridSize - 1],
            grid[gridSize - 1][0], grid[gridSize - 1][gridSize - 1]
        )
        val maxValue = grid.flatten().maxOfOrNull { it.value } ?: 0
        if (corners.any { it.value == maxValue }) {
            score += 1000
        }
        
        return score
    }
    
    /**
     * 克隆当前游戏状态
     */
    private fun clone(): GameEngine {
        val clone = GameEngine(gridSize)
        clone.grid = grid.map { row ->
            row.map { it.copy() }.toTypedArray()
        }.toTypedArray()
        clone.score = score
        clone.bestScore = bestScore
        clone.moveCount = moveCount
        clone.isGameOver = isGameOver
        clone.isWon = isWon
        return clone
    }
    
    /**
     * 保存当前状态快照
     */
    private fun saveSnapshot() {
        val snapshot = GameSnapshot(
            grid = grid.map { row ->
                row.map { it.copy() }.toTypedArray()
            }.toTypedArray(),
            score = score,
            moveCount = moveCount
        )
        
        history.add(snapshot)
        
        // 限制历史记录大小
        if (history.size > maxHistorySize) {
            history.removeAt(0)
        }
    }
    
    /**
     * 添加随机方块
     */
    private fun addRandomTile() {
        val availableCells = mutableListOf<Pair<Int, Int>>()
        
        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                if (grid[i][j].isEmpty()) {
                    availableCells.add(Pair(i, j))
                }
            }
        }
        
        if (availableCells.isNotEmpty()) {
            val randomCell = availableCells[Random.nextInt(availableCells.size)]
            val value = if (Random.nextFloat() < 0.9f) 2 else 4
            grid[randomCell.first][randomCell.second] = Tile(
                value = value,
                row = randomCell.first,
                col = randomCell.second,
                isNew = true
            )
        }
    }
    
    /**
     * 获取移动向量
     */
    private fun getVector(direction: Direction): Pair<Int, Int> {
        return when (direction) {
            Direction.UP -> Pair(-1, 0)
            Direction.DOWN -> Pair(1, 0)
            Direction.LEFT -> Pair(0, -1)
            Direction.RIGHT -> Pair(0, 1)
        }
    }
    
    /**
     * 构建遍历顺序
     */
    private fun buildTraversals(direction: Direction): Traversals {
        val x = (0 until gridSize).toMutableList()
        val y = (0 until gridSize).toMutableList()
        
        if (direction == Direction.DOWN) x.reverse()
        if (direction == Direction.RIGHT) y.reverse()
        
        return Traversals(x, y)
    }
    
    data class Traversals(val x: List<Int>, val y: List<Int>)
    
    /**
     * 清除合并标记
     */
    private fun prepareTiles() {
        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                grid[i][j].mergedFrom = null
                grid[i][j].isNew = false
            }
        }
    }
    
    /**
     * 查找最远可移动位置
     */
    private fun findFarthestPosition(
        row: Int,
        col: Int,
        vector: Pair<Int, Int>
    ): Positions {
        var previous = Pair(row, col)
        var current = Pair(row + vector.first, col + vector.second)
        
        while (withinBounds(current.first, current.second) &&
            grid[current.first][current.second].isEmpty()
        ) {
            previous = current
            current = Pair(current.first + vector.first, current.second + vector.second)
        }
        
        return Positions(
            farthest = previous,
            next = if (withinBounds(current.first, current.second)) current else null
        )
    }
    
    data class Positions(
        val farthest: Pair<Int, Int>,
        val next: Pair<Int, Int>?
    )
    
    /**
     * 检查坐标是否在边界内
     */
    private fun withinBounds(row: Int, col: Int): Boolean {
        return row >= 0 && row < gridSize && col >= 0 && col < gridSize
    }
    
    /**
     * 移动方块
     */
    private fun moveTile(tile: Tile, newRow: Int, newCol: Int) {
        grid[tile.row][tile.col] = Tile(0, tile.row, tile.col)
        tile.row = newRow
        tile.col = newCol
        grid[newRow][newCol] = tile
    }
    
    /**
     * 检查是否还有可用移动
     */
    private fun movesAvailable(): Boolean {
        return cellsAvailable() || tileMatchesAvailable()
    }
    
    /**
     * 检查是否有空格
     */
    private fun cellsAvailable(): Boolean {
        return grid.any { row -> row.any { it.isEmpty() } }
    }
    
    /**
     * 检查是否有可合并的方块
     */
    private fun tileMatchesAvailable(): Boolean {
        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                val tile = grid[i][j]
                if (!tile.isEmpty()) {
                    // 检查四个方向
                    val directions = listOf(
                        Pair(-1, 0), Pair(1, 0),
                        Pair(0, -1), Pair(0, 1)
                    )
                    
                    for (direction in directions) {
                        val newRow = i + direction.first
                        val newCol = j + direction.second
                        
                        if (withinBounds(newRow, newCol)) {
                            val other = grid[newRow][newCol]
                            if (other.value == tile.value) {
                                return true
                            }
                        }
                    }
                }
            }
        }
        return false
    }
    
    /**
     * 获取当前游戏状态
     */
    fun getGameState(): GameState {
        return GameState(
            grid = grid,
            score = score,
            bestScore = bestScore,
            isGameOver = isGameOver,
            isWon = isWon,
            moveCount = moveCount
        )
    }
    
    /**
     * 设置最高分
     */
    fun setBestScore(score: Int) {
        bestScore = score
    }
    
    /**
     * 继续游戏（赢了之后继续玩）
     */
    fun keepPlaying() {
        isWon = false
    }
    
    /**
     * 获取网格大小
     */
    fun getGridSize(): Int = gridSize
    
    /**
     * 是否可以撤销
     */
    fun canUndo(): Boolean = history.isNotEmpty()
}

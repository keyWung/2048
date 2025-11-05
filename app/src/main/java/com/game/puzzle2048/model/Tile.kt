package com.game.puzzle2048.model

/**
 * 表示游戏中的一个方块
 */
data class Tile(
    var value: Int = 0,
    var row: Int = 0,
    var col: Int = 0,
    var mergedFrom: Pair<Tile, Tile>? = null,
    var isNew: Boolean = false
) {
    fun isEmpty(): Boolean = value == 0
    
    fun copy(): Tile {
        return Tile(value, row, col, mergedFrom, isNew)
    }
}

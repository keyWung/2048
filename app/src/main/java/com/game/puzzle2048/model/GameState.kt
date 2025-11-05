package com.game.puzzle2048.model

/**
 * 游戏状态
 */
data class GameState(
    val grid: Array<Array<Tile>>,
    val score: Int,
    val bestScore: Int,
    val isGameOver: Boolean,
    val isWon: Boolean,
    val moveCount: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GameState

        if (!grid.contentDeepEquals(other.grid)) return false
        if (score != other.score) return false
        if (bestScore != other.bestScore) return false
        if (isGameOver != other.isGameOver) return false
        if (isWon != other.isWon) return false

        return true
    }

    override fun hashCode(): Int {
        var result = grid.contentDeepHashCode()
        result = 31 * result + score
        result = 31 * result + bestScore
        result = 31 * result + isGameOver.hashCode()
        result = 31 * result + isWon.hashCode()
        return result
    }
}

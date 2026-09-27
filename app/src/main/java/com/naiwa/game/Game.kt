package com.naiwa.game

import kotlin.random.Random

enum class Direction { LEFT, RIGHT, UP, DOWN }
data class Motion(val from: Int, val to: Int, val value: Int)
data class Turn(val changed: Boolean, val motions: List<Motion> = emptyList(), val merged: Set<Int> = emptySet(), val gained: Int = 0, val spawned: Int = -1)

class Game(private val random: Random = Random.Default) {
    var cells = IntArray(16)
    var score = 0
    val won get() = cells.any { it == 2048 }
    val lost get() = !won && cells.none { it == 0 } && (0..15).all { i ->
        (i % 4 == 3 || cells[i] != cells[i + 1]) && (i / 4 == 3 || cells[i] != cells[i + 4])
    }
    fun reset() { cells = IntArray(16); score = 0; spawn(); spawn() }
    private fun spawn(): Int {
        val empty = cells.indices.filter { cells[it] == 0 }
        if (empty.isEmpty()) return -1
        val index = empty[random.nextInt(empty.size)]
        cells[index] = if (random.nextInt(10) == 0) 4 else 2
        return index
    }
    fun move(direction: Direction): Turn {
        if (won || lost) return Turn(false)
        val next = IntArray(16)
        val motions = mutableListOf<Motion>()
        val merged = mutableSetOf<Int>()
        var gained = 0
        for (line in 0..3) {
            val positions = (0..3).map { n -> when(direction) {
                Direction.LEFT -> line * 4 + n
                Direction.RIGHT -> line * 4 + 3 - n
                Direction.UP -> n * 4 + line
                Direction.DOWN -> (3 - n) * 4 + line
            } }
            val occupied = positions.filter { cells[it] != 0 }
            var read = 0; var write = 0
            while (read < occupied.size) {
                val source = occupied[read]; val dest = positions[write++]
                val value = cells[source]
                motions.add(Motion(source, dest, value))
                if (read + 1 < occupied.size && cells[occupied[read + 1]] == value) {
                    motions.add(Motion(occupied[read + 1], dest, value))
                    next[dest] = value * 2; gained += value * 2; merged.add(dest); read += 2
                } else { next[dest] = value; read++ }
            }
        }
        if (cells.contentEquals(next)) return Turn(false)
        cells = next; score += gained
        val spawned = if (won) -1 else spawn()
        return Turn(true, motions, merged, gained, spawned)
    }
}

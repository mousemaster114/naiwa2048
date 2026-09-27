package com.naiwa.game

import android.content.Context
import android.media.MediaPlayer
import kotlin.random.Random

class GameAudio(private val context: Context) {
    var enabled = true
    private var player: MediaPlayer? = null
    fun stop() { player?.release(); player = null }
    private fun play(resource: Int) {
        stop()
        if (!enabled) return
        player = MediaPlayer.create(context, resource)?.also { p ->
            p.setOnCompletionListener { if (player === it) stop() }
            p.start()
        }
    }
    fun merge() = play(if (Random.nextBoolean()) R.raw.merge_one else R.raw.merge_two)
    fun win() = play(R.raw.end)
}

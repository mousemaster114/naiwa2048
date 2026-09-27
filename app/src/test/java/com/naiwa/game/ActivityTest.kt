package com.naiwa.game

import android.content.Context
import android.media.MediaPlayer
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowMediaPlayer
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class ActivityTest {
    private val context get()=RuntimeEnvironment.getApplication()
    @Before fun clear() {
        context.getSharedPreferences("game",Context.MODE_PRIVATE).edit().clear().commit()
        ShadowMediaPlayer.setMediaInfoProvider { ShadowMediaPlayer.MediaInfo(10000,0) }
    }
    private fun seed(vararg cells: Int) { context.getSharedPreferences("game",Context.MODE_PRIVATE).edit().putString("cells",IntArray(16){cells.getOrElse(it){0}}.joinToString(",")).putInt("score",80).commit() }
    private fun descendants(view: View): List<View> = listOf(view)+if(view is ViewGroup)(0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else emptyList()
    @Test fun restoresScoreAndPersistsSoundSetting() {
        seed(2,4)
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity=controller.get()
        val views=descendants(activity.window.decorView)
        assertTrue(views.filterIsInstance<TextView>().any { it.text.toString()=="80" })
        views.filterIsInstance<Button>().first { it.text.toString()=="声音 · 开" }.performClick()
        assertFalse(context.getSharedPreferences("game",0).getBoolean("sound",true))
        controller.pause().stop().destroy()
    }
    @Test fun winningSwipeShowsCorrectDialogAndDoesNotReplayOnResume() {
        seed(1024,1024)
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity=controller.get()
        val board=descendants(activity.window.decorView).filterIsInstance<BoardView>().single()
        board.onMove(Direction.LEFT)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        val dialog=ShadowDialog.getLatestDialog()
        assertNotNull(dialog)
        assertTrue(descendants(dialog.window!!.decorView).filterIsInstance<TextView>().any { it.text.toString()=="恭喜你合成了大奶蛋" })
        val audioField=MainActivity::class.java.getDeclaredField("audio").apply { isAccessible=true }
        val audio=audioField.get(activity)
        val playerField=GameAudio::class.java.getDeclaredField("player").apply { isAccessible=true }
        controller.pause(); assertNull(playerField.get(audio))
        controller.resume(); shadowOf(Looper.getMainLooper()).idle()
        assertSame(dialog,ShadowDialog.getLatestDialog()); assertNull(playerField.get(audio))
        controller.pause().stop().destroy()
    }
    @Test fun audioOffAndStopReleasePlayback() {
        val audio=GameAudio(context)
        val field=GameAudio::class.java.getDeclaredField("player").apply { isAccessible=true }
        audio.merge(); assertTrue((field.get(audio) as MediaPlayer).isPlaying)
        audio.stop(); assertNull(field.get(audio))
        audio.enabled=false; audio.merge(); assertNull(field.get(audio)); audio.win(); assertNull(field.get(audio))
    }
    @Test fun multipleMergesInOneSwipePlayOnlyOneSound() {
        seed(2,2,2,2)
        var plays=0
        ShadowMediaPlayer.setCreateListener { _,_->plays++ }
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        val board=descendants(controller.get().window.decorView).filterIsInstance<BoardView>().single()
        board.onMove(Direction.LEFT)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        assertEquals(1,plays)
        controller.pause().stop().destroy()
    }
    @Test fun smallScreenBoardStaysSquare() {
        val board=BoardView(context,Game().apply { reset() })
        board.measure(View.MeasureSpec.makeMeasureSpec(280,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(220,View.MeasureSpec.AT_MOST))
        assertEquals(220,board.measuredWidth); assertEquals(220,board.measuredHeight)
    }
}

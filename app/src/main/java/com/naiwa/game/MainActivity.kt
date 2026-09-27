package com.naiwa.game

import android.app.*
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.view.*
import android.view.animation.OvershootInterpolator
import android.widget.*

class MainActivity : Activity() {
    private val game=Game()
    private val prefs by lazy { getSharedPreferences("game",MODE_PRIVATE) }
    private lateinit var audio: GameAudio
    private lateinit var board: BoardView
    private lateinit var score: TextView
    private lateinit var best: TextView
    private lateinit var gain: TextView
    private var bestScore=0
    private var terminalShown=false
    private var foreground=false
    private var endDialog: Dialog?=null
    private var pendingDirection: Direction?=null
    private val brown=Color.rgb(86,55,25)
    private fun dp(n: Int)=(n*resources.displayMetrics.density).toInt()
    private fun shape(color: Int, radius: Float=20f)=GradientDrawable().apply { setColor(color); cornerRadius=dp(radius.toInt()).toFloat() }
    private fun label(text: String, size: Float, bold: Boolean=false)=TextView(this).apply {
        this.text=text; textSize=size; setTextColor(brown); gravity=Gravity.CENTER
        if(bold) typeface=Typeface.create("sans-serif-rounded",Typeface.BOLD)
    }
    private fun button(text: String, click: ()->Unit)=Button(this).apply {
        this.text=text; isAllCaps=false; textSize=15f; setTextColor(brown); typeface=Typeface.DEFAULT_BOLD
        background=RippleDrawable(ColorStateList.valueOf(0x33B96D26),shape(0xFFFFE294.toInt()),null)
        stateListAnimator=null; setOnClickListener { animate().scaleX(.96f).scaleY(.96f).setDuration(60).withEndAction { animate().scaleX(1f).scaleY(1f).setDuration(120).start() }.start(); click() }
    }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        audio=GameAudio(this); audio.enabled=prefs.getBoolean("sound",true); bestScore=prefs.getInt("best",0)
        val saved=prefs.getString("cells",null)?.split(",")?.mapNotNull { it.toIntOrNull() }
        if(saved?.size==16 && saved.all { it==0 || it in listOf(2,4,8,16,32,64,128,256,512,1024,2048) }) { game.cells=saved.toIntArray(); game.score=prefs.getInt("score",0) } else game.reset()
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER_HORIZONTAL; setPadding(dp(22),dp(12),dp(22),dp(14)); setBackgroundColor(0xFFFFF5D6.toInt()) }
        setContentView(root)
        root.addView(label("奶 蛋 合 成 俱 乐 部",11f).apply { letterSpacing=.12f },LinearLayout.LayoutParams(-1,dp(23)))
        root.addView(label("合成大奶蛋",32f,true),LinearLayout.LayoutParams(-1,dp(53)))
        root.addView(label("轻轻一滑，快乐加倍",13f),LinearLayout.LayoutParams(-1,dp(28)))
        val stats=LinearLayout(this).apply { gravity=Gravity.CENTER; orientation=LinearLayout.HORIZONTAL }
        fun stat(title: String): TextView {
            val column=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER; background=shape(0xFFFFEABC.toInt(),16f) }
            column.addView(label(title,11f)); val value=label("0",25f,true); column.addView(value)
            stats.addView(column,LinearLayout.LayoutParams(0,dp(70),1f).apply { setMargins(dp(4),0,dp(4),0) }); return value
        }
        score=stat("本局得分"); best=stat("最高纪录"); root.addView(stats,LinearLayout.LayoutParams(-1,dp(70)))
        gain=label("目标 · 2048",13f,true); root.addView(gain,LinearLayout.LayoutParams(-1,dp(32)))
        val holder=FrameLayout(this)
        board=BoardView(this,game)
        holder.addView(board,FrameLayout.LayoutParams(-1,-1,Gravity.CENTER))
        root.addView(holder,LinearLayout.LayoutParams(-1,0,1f))
        root.addView(label("相同数字碰一碰，合出你的大奶蛋",12f),LinearLayout.LayoutParams(-1,dp(40)))
        val controls=LinearLayout(this)
        val sound=button(if(audio.enabled) "声音 · 开" else "声音 · 关") {}
        sound.setOnClickListener { audio.enabled=!audio.enabled; if(!audio.enabled)audio.stop(); sound.text=if(audio.enabled)"声音 · 开" else "声音 · 关"; save() }
        controls.addView(sound,LinearLayout.LayoutParams(0,dp(52),1f).apply { marginEnd=dp(10) })
        controls.addView(button("重新开始") { AlertDialog.Builder(this).setTitle("重新开始？").setMessage("当前这局的进度将清空，最高纪录会保留。").setNegativeButton("继续玩",null).setPositiveButton("重新开始") { _,_->reset() }.show() },LinearLayout.LayoutParams(0,dp(52),1f))
        root.addView(controls)
        root.addView(label("创意：hzr\n制作：ytw",11f),LinearLayout.LayoutParams(-1,dp(38)))
        updateScores()
        board.onMove={ direction -> moveDirection(direction) }
    }
    private fun moveDirection(direction: Direction) {
        if(!foreground || game.won || game.lost) { pendingDirection=null; return }
        if(board.busy) { pendingDirection=direction; return }
        val result=game.move(direction)
        if(!result.changed)return
        save()
        board.animateTurn(result,{
            updateScores()
            if(result.gained>0) {
                gain.text="+${result.gained}  太棒啦！"
                gain.translationY=dp(8).toFloat()
                gain.animate().translationY(0f).setDuration(220).start()
            }
        },{
            gain.text="目标 · 2048"
            if(game.won || game.lost) { pendingDirection=null; showTerminal(true) }
            else {
                val next=pendingDirection
                pendingDirection=null
                if(next!=null)moveDirection(next)
            }
        })
        if(result.gained>0)audio.merge()
    }
    private fun updateScores() { bestScore=maxOf(bestScore,game.score); score.text=game.score.toString(); best.text=bestScore.toString() }
    private fun save() { bestScore=maxOf(bestScore,game.score); prefs.edit().putString("cells",game.cells.joinToString(",")).putInt("score",game.score).putInt("best",bestScore).putBoolean("sound",audio.enabled).apply() }
    private fun reset() { pendingDirection=null; board.finishAnimation(); audio.stop(); endDialog?.dismiss(); endDialog=null; terminalShown=false; game.reset(); gain.text="目标 · 2048"; updateScores(); board.invalidate(); save() }
    private fun showTerminal(playSound: Boolean) {
        if(!foreground || terminalShown || (!game.won && !game.lost))return
        terminalShown=true
        val won=game.won
        val dialog=Dialog(this); endDialog=dialog; dialog.setCancelable(false)
        val frame=FrameLayout(this)
        val content=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER; setPadding(dp(22),dp(25),dp(22),dp(22)); background=shape(0xFFFFF5D6.toInt(),28f) }
        content.addView(label(if(won)"恭喜你合成了大奶蛋" else "差一点点，再来一局！",22f,true))
        content.addView(label("本局得分  ${game.score}",14f),LinearLayout.LayoutParams(-1,dp(34)))
        if(won) content.addView(ImageView(this).apply { setImageResource(R.drawable.picture); scaleType=ImageView.ScaleType.FIT_CENTER; contentDescription="大奶蛋" },LinearLayout.LayoutParams(-1,dp(210)))
        content.addView(button("再来一局") { reset() },LinearLayout.LayoutParams(-1,dp(52)).apply { topMargin=dp(16) })
        frame.addView(content)
        if(won) frame.addView(ConfettiView(this),FrameLayout.LayoutParams(-1,-1))
        dialog.setContentView(frame); dialog.window?.setBackgroundDrawableResource(android.R.color.transparent); dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels*.88f).toInt(),-2)
        content.scaleX=.8f; content.scaleY=.8f; content.alpha=0f
        content.animate().alpha(1f).scaleX(1f).scaleY(1f).setInterpolator(OvershootInterpolator(.9f)).setDuration(380).start()
        if(won && playSound)audio.win()
    }
    override fun onResume() { super.onResume(); foreground=true; updateScores(); gain.text="目标 · 2048"; board.post { showTerminal(false) } }
    override fun onPause() { foreground=false; pendingDirection=null; board.finishAnimation(); audio.stop(); save(); super.onPause() }
    override fun onDestroy() { endDialog?.dismiss(); audio.stop(); super.onDestroy() }
}

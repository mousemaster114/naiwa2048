package com.naiwa.game

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.*

class BoardView(context: Context, private val game: Game) : View(context) {
    var onMove: (Direction) -> Unit = {}
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val picture = BitmapFactory.decodeResource(resources, R.drawable.picture)
    private var turn: Turn? = null
    private var progress = 1f
    private var popping = false
    private var animator: ValueAnimator? = null
    var busy = false; private set
    private var downX = 0f; private var downY = 0f
    private val brown = Color.rgb(86,55,25)
    init { contentDescription = "4乘4棋盘，上下左右滑动合成数字"; isFocusable = true }
    override fun onMeasure(w: Int, h: Int) { val size = min(MeasureSpec.getSize(w), MeasureSpec.getSize(h)); setMeasuredDimension(size,size) }
    private val gap get() = width * .025f
    private val cell get() = (width - gap * 5) / 4
    private fun rect(i: Int): RectF { val x = gap + i%4*(cell+gap); val y=gap+i/4*(cell+gap); return RectF(x,y,x+cell,y+cell) }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.color=Color.rgb(235,204,139); canvas.drawRoundRect(0f,0f,width.toFloat(),height.toFloat(),gap*2,gap*2,paint)
        for(i in 0..15) { paint.color=Color.rgb(247,226,179); canvas.drawRoundRect(rect(i),gap*1.3f,gap*1.3f,paint) }
        val move = turn
        if (busy && !popping && move != null) {
            for (m in move.motions) {
                val a=rect(m.from); val b=rect(m.to)
                a.offset((b.left-a.left)*progress,(b.top-a.top)*progress)
                tile(canvas,a,m.value,1f,false)
            }
        } else {
            for(i in 0..15) if(game.cells[i]!=0) {
                val merge = popping && move?.merged?.contains(i)==true
                val scale = if(popping && move?.spawned==i) .45f+.55f*progress else if(merge) 1f+.13f*sin(progress*PI).toFloat() else 1f
                tile(canvas,rect(i),game.cells[i],scale,merge)
            }
        }
    }
    private fun tile(c: Canvas, r: RectF, value: Int, scale: Float, glow: Boolean) {
        c.save(); c.scale(scale,scale,r.centerX(),r.centerY())
        val level=Integer.numberOfTrailingZeros(value)
        paint.color=Color.rgb(255,(237-level*7).coerceAtLeast(158),(188-level*10).coerceAtLeast(70))
        if(glow) { paint.setShadowLayer(gap,0f,0f,Color.rgb(245,189,79)) }
        c.drawRoundRect(r,gap*1.3f,gap*1.3f,paint); paint.clearShadowLayer()
        c.save(); val path=Path(); path.addRoundRect(r,gap*1.3f,gap*1.3f,Path.Direction.CW); c.clipPath(path)
        val side=min(picture.width,picture.height); val source=Rect((picture.width-side)/2,(picture.height-side)/2,(picture.width+side)/2,(picture.height+side)/2)
        paint.alpha=65; c.drawBitmap(picture,source,r,paint); paint.alpha=255
        paint.color=Color.argb(38,255,245,214); c.drawRect(r,paint); c.restore()
        paint.color=brown; paint.typeface=Typeface.create("sans-serif-rounded",Typeface.BOLD)
        paint.textAlign=Paint.Align.CENTER; paint.textSize=cell*(if(value>=1000) .34f else if(value>=100) .42f else .49f)
        c.drawText(value.toString(),r.centerX(),r.centerY()-(paint.ascent()+paint.descent())/2,paint)
        c.restore()
    }
    fun animateTurn(result: Turn, merged: () -> Unit, done: () -> Unit) {
        turn=result; busy=true; popping=false
        animator=ValueAnimator.ofFloat(0f,1f).apply {
            duration=140; interpolator=DecelerateInterpolator()
            addUpdateListener { progress=it.animatedValue as Float; invalidate() }
            addListener(object: android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if(!busy) return
                    merged(); popping=true
                    animator=ValueAnimator.ofFloat(0f,1f).apply {
                        duration=180
                        addUpdateListener { progress=it.animatedValue as Float; invalidate() }
                        addListener(object: android.animation.AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: android.animation.Animator) { if(!busy)return; busy=false; popping=false; turn=null; invalidate(); done() }
                        }); start()
                    }
                }
            }); start()
        }
    }
    fun finishAnimation() { busy=false; animator?.cancel(); turn=null; popping=false; invalidate() }
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when(e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { downX=e.x; downY=e.y; return true }
            MotionEvent.ACTION_UP -> {
                val dx=e.x-downX; val dy=e.y-downY
                if(!busy && max(abs(dx),abs(dy))>24*resources.displayMetrics.density) onMove(if(abs(dx)>abs(dy)) { if(dx>0) Direction.RIGHT else Direction.LEFT } else { if(dy>0) Direction.DOWN else Direction.UP })
                performClick(); return true
            }
            MotionEvent.ACTION_CANCEL -> return true
        }; return true
    }
    override fun performClick(): Boolean { super.performClick(); return true }
}

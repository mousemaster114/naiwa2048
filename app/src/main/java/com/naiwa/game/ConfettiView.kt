package com.naiwa.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import android.view.View
import kotlin.random.Random

class ConfettiView(context: Context): View(context) {
    private val start=SystemClock.uptimeMillis()
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val particles=List(45) { floatArrayOf(Random.nextFloat(),Random.nextFloat(),Random.nextFloat()) }
    override fun onDraw(canvas: Canvas) {
        val time=(SystemClock.uptimeMillis()-start)/2400f
        if(time>1)return
        particles.forEachIndexed { i,p ->
            paint.color=if(i%2==0) 0xFFF5BD4F.toInt() else 0xFFB96D26.toInt(); paint.alpha=((1-time)*220).toInt()
            val x=p[0]*width; val y=(time*(1+p[1])-.25f)*height
            canvas.save(); canvas.rotate(time*360+p[2]*180,x,y); canvas.drawRect(x,y,x+5*resources.displayMetrics.density,y+9*resources.displayMetrics.density,paint); canvas.restore()
        }; postInvalidateOnAnimation()
    }
}

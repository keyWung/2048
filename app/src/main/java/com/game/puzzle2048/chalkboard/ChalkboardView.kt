package com.game.puzzle2048.chalkboard

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.io.ByteArrayOutputStream
import java.lang.Math.abs

class ChalkboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 绘制黑板背景的 Bitmap 和 Canvas
    private lateinit var extraCanvas: Canvas
    private lateinit var extraBitmap: Bitmap

    // 用于绘制路径的 Paint 对象
    // 这里只定义基本的 Paint 属性，不要在这里设置 Shader
    private val drawPaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 10f
        alpha = 200
    }

    // 绘制黑板背景的 Paint 对象
    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#3B3C36") // 深灰色黑板
    }

    // 用于存储当前绘制路径的 Path 对象
    private var path = Path()
    // 存储所有已绘制路径和其对应 Paint 的列表，用于清除和重绘
    private val paths = mutableListOf<Pair<Path, Paint>>()

    // 用于平滑曲线的缓存点
    private var motionTouchEventX = 0f
    private var motionTouchEventY = 0f
    private var currentX = 0f
    private var currentY = 0f
    private val touchTolerance = 4f // 触摸容忍度，用于平滑曲线

    // 粉笔纹理 Bitmap，需要在 View 初始化后加载或生成
    private var chalkTextureBitmap: Bitmap? = null

    init {
        // 加载或生成粉笔纹理
        loadChalkTexture()
    }

    private fun loadChalkTexture() {
        val textureSize = 120
        // 创建一个透明底的 Bitmap
        chalkTextureBitmap = Bitmap.createBitmap(textureSize, textureSize, Bitmap.Config.ARGB_8888)
        val tempCanvas = Canvas(chalkTextureBitmap!!)

        val noisePaint = Paint().apply {
            color = Color.WHITE // 【修改 1】改成白色，这样画出来的才是白粉笔
            alpha = 255         // 【修改 2】透明度设为 255 (不透明)，让颗粒更清晰
            // 如果觉得太生硬，可以设为 200 左右
        }

        val random = java.util.Random()
        for (i in 0 until textureSize) {
            for (j in 0 until textureSize) {
                // 【修改 3】增加密度。
                // 之前的 30% 可能太稀疏，改成 50% 或 60% 让粉笔线条更连续
                if (random.nextInt(100) < 50) {
                    tempCanvas.drawPoint(i.toFloat(), j.toFloat(), noisePaint)
                }
            }
        }

        // 应用 Shader
        if (chalkTextureBitmap != null) {
            drawPaint.shader = BitmapShader(
                chalkTextureBitmap!!,
                Shader.TileMode.REPEAT,
                Shader.TileMode.REPEAT
            )
        }
    }


    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        if (::extraBitmap.isInitialized) extraBitmap.recycle() // 回收旧的 bitmap

        extraBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        extraCanvas = Canvas(extraBitmap)
        // 绘制黑板背景
        extraCanvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 1. 绘制黑板背景
        canvas.drawBitmap(extraBitmap, 0f, 0f, null)

        // 2. 绘制所有已保存的路径
        for ((path, paint) in paths) {
            canvas.drawPath(path, paint)
        }
        // 3. 绘制当前正在画的路径 (确保它也在最上层)
        canvas.drawPath(path, drawPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        motionTouchEventX = event.x
        motionTouchEventY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> touchStart()
            MotionEvent.ACTION_MOVE -> touchMove()
            MotionEvent.ACTION_UP -> touchUp()
        }
        return true
    }

    private fun touchStart() {
//        path.reset()
        path.moveTo(motionTouchEventX, motionTouchEventY)
        currentX = motionTouchEventX
        currentY = motionTouchEventY
    }

    private fun touchMove() {
        val dx = abs(motionTouchEventX - currentX)
        val dy = abs(motionTouchEventY - currentY)
        if (dx >= touchTolerance || dy >= touchTolerance) {
            // 用贝塞尔曲线连接点，使线条更平滑
            path.quadTo(currentX, currentY, (motionTouchEventX + currentX) / 2, (motionTouchEventY + currentY) / 2)
            currentX = motionTouchEventX
            currentY = motionTouchEventY
        }
        // 实时更新绘制，模拟连续性
        invalidate()
    }

    private fun touchUp() {
//        path.lineTo(currentX, currentY) // 路径结束
//        // 保存当前的路径和 Paint 副本
//        // 需要克隆 Paint，因为 drawPaint 会被后续操作修改
//        val currentPaint = Paint(drawPaint)
//        currentPaint.shader = chalkTextureBitmap?.let { BitmapShader(it, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT) }
//        paths.add(Pair(path, currentPaint))
//        path = Path() // 重置 path，准备下一次绘制
       // invalidate() // 刷新视图
    }

    /**
     * 设置粉笔颜色
     * 注意：因为使用了 Shader，单纯设置 color 无效，必须使用 ColorFilter 来染色
     */
    fun setChalkColor(color: Int) {
        // 使用 PorterDuff 模式将颜色叠加到纹理上
        // SRC_IN: 取两层交集，显示上层(颜色)但在下层(纹理)形状内
        // MULTIPLY: 正片叠底，适合将白色纹理染成其他颜色
        drawPaint.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY)
        invalidate()
    }

    /**
     * 清除黑板上的所有笔迹
     */
    fun clearChalkboard() {
        paths.clear() // 清除所有保存的路径
        // 重新绘制一个干净的黑板背景
        extraCanvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        invalidate() // 刷新视图
    }

    /**
     * 获取当前的黑板内容（包括背景和笔迹）作为 Bitmap
     */
    fun getBitmap(): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        draw(canvas) // 调用 onDraw 方法将所有内容绘制到新的 Bitmap 上
        return returnedBitmap
    }
}
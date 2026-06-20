package com.ods.wallpaper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import kotlin.math.min
import kotlin.math.sin

/**
 * ODS Spotlight live wallpaper.
 *
 * Reproduces the outlined-design.com hero mechanic on the S23 Ultra lock + home screen:
 *  - the ambient backdrop is the site base image, BLURRED (the "menu" look)
 *  - a soft spotlight follows your finger and reveals the sharp crimson intelligence
 *    layer (ods_reveal) beneath, exactly like lib/spotlight.tsx
 *  - when untouched it drifts slowly so the lock screen stays alive
 *
 * Pure Canvas (no GL) so it stays light. The draw loop only runs while visible.
 */
class SpotlightWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = SpotlightEngine()

    inner class SpotlightEngine : Engine() {

        private val handler = Handler(Looper.getMainLooper())
        private val prefs = Prefs(this@SpotlightWallpaperService)

        private var base: Bitmap? = null       // sharp dark base (cover-scaled)
        private var blurred: Bitmap? = null     // blurred backdrop
        private var reveal: Bitmap? = null      // crimson layer revealed in the spotlight

        private var width = 0
        private var height = 0

        // spotlight state (current + eased target)
        private var sx = 0f
        private var sy = 0f
        private var tx = 0f
        private var ty = 0f
        private var radius = 280f
        private var touching = false
        private var startTime = System.currentTimeMillis()
        private var visible = false
        private var xOffset = 0.5f

        private val layerPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        private val dimPaint = Paint().apply { color = Color.argb(64, 0, 0, 0) }

        private val drawRunner = Runnable { drawFrame() }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            setTouchEventsEnabled(true)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
            width = w
            height = h
            radius = (min(w, h) * prefs.radiusFraction)
            sx = w / 2f; sy = h / 2f; tx = sx; ty = sy
            loadBitmaps()
        }

        override fun onVisibilityChanged(v: Boolean) {
            visible = v
            if (v) { startTime = System.currentTimeMillis(); scheduleNext() }
            else handler.removeCallbacks(drawRunner)
        }

        override fun onOffsetsChanged(
            xOffset: Float, yOffset: Float, xStep: Float, yStep: Float,
            xPixels: Int, yPixels: Int,
        ) {
            this.xOffset = xOffset
            if (!touching) drawFrame()
        }

        override fun onTouchEvent(event: MotionEvent) {
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    touching = true; tx = event.x; ty = event.y
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> touching = false
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            visible = false
            handler.removeCallbacks(drawRunner)
            recycle()
        }

        private fun scheduleNext() {
            handler.removeCallbacks(drawRunner)
            if (visible) handler.postDelayed(drawRunner, 16L) // ~60fps while visible
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) render(canvas)
            } finally {
                if (canvas != null) try { holder.unlockCanvasAndPost(canvas) } catch (_: Exception) {}
            }
            scheduleNext()
        }

        private fun render(canvas: Canvas) {
            val bg = blurred ?: return
            val rev = reveal ?: return

            // Idle drift target (slow Lissajous) when the user is not touching.
            if (!touching) {
                val t = (System.currentTimeMillis() - startTime) / 1000.0
                tx = width * (0.5f + 0.28f * sin(t * 0.35).toFloat())
                ty = height * (0.42f + 0.18f * sin(t * 0.27 + 1.3).toFloat())
            }
            // Ease current spotlight toward target (critically-damped feel).
            sx += (tx - sx) * 0.18f
            sy += (ty - sy) * 0.18f
            val targetR = if (touching) radius * 1.12f else radius
            // simple radius easing
            val curR = targetR

            // 1) ambient blurred backdrop (the website-menu look)
            canvas.drawBitmap(bg, 0f, 0f, layerPaint)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)

            // 2) reveal the crimson layer inside a soft spotlight following the finger
            val saved = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
            canvas.drawBitmap(rev, 0f, 0f, layerPaint)
            maskPaint.shader = RadialGradient(
                sx, sy, curR,
                intArrayOf(Color.WHITE, Color.WHITE, Color.TRANSPARENT),
                floatArrayOf(0f, 0.62f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), maskPaint)
            canvas.restoreToCount(saved)
        }

        // ---- bitmap loading: decode + cover-scale to the surface, build a blurred copy ----
        private fun loadBitmaps() {
            if (width == 0 || height == 0) return
            recycle()
            base = decodeCover(R.drawable.ods_base)
            reveal = decodeCover(R.drawable.ods_reveal)
            blurred = base?.let { boxBlur(it, prefs.blurPasses) }
        }

        private fun decodeCover(resId: Int): Bitmap? {
            val src = BitmapFactory.decodeResource(resources, resId) ?: return null
            val scale = maxOf(width.toFloat() / src.width, height.toFloat() / src.height)
            val sw = (src.width * scale).toInt().coerceAtLeast(1)
            val sh = (src.height * scale).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(src, sw, sh, true)
            // center-crop to the surface
            val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val c = Canvas(out)
            val left = (width - sw) / 2f
            val top = (height - sh) / 2f
            c.drawBitmap(scaled, left, top, layerPaint)
            if (scaled != src) scaled.recycle()
            src.recycle()
            return out
        }

        /** Cheap, GL-free blur: downscale → upscale a few times (approximates the menu blur). */
        private fun boxBlur(src: Bitmap, passes: Int): Bitmap {
            var work = src
            val down = 0.18f
            repeat(passes.coerceIn(1, 4)) {
                val small = Bitmap.createScaledBitmap(
                    work, (work.width * down).toInt().coerceAtLeast(1),
                    (work.height * down).toInt().coerceAtLeast(1), true,
                )
                val up = Bitmap.createScaledBitmap(small, width, height, true)
                small.recycle()
                if (work != src) work.recycle()
                work = up
            }
            return work
        }

        private fun recycle() {
            base?.recycle(); blurred?.recycle(); reveal?.recycle()
            base = null; blurred = null; reveal = null
        }
    }
}

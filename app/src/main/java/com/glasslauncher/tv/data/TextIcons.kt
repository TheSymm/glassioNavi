package com.glasslauncher.tv.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Text icons made on the TV: an app's name drawn in one of a few styles, in the same four looks as the Apple TV
 * library (default, dark, white, clear) and the same 1280 × 768 size, so they sit with those icons in the picker.
 * Original lettering only (Poppins / DejaVu Sans Mono, both bundled under their open licences).
 */
object TextIcons {
    const val W = 1280
    const val H = 768
    val VARIANTS = listOf("default", "dark", "white", "clear")

    enum class Style(val label: String, val c0: Int, val c1: Int) {
        Title("Title", 0xFF7C3AED.toInt(), 0xFF1E1B4B.toInt()),
        Pixel("Pixel", 0xFF10B981.toInt(), 0xFF0C3250.toInt()),
        Tv("Old TV", 0xFFEF4444.toInt(), 0xFF5A140A.toInt()),
    }

    private const val INK = 0xFF121214.toInt()
    private var poppins: Typeface? = null
    private var mono: Typeface? = null
    private fun poppins(ctx: Context) = poppins ?: runCatching { Typeface.createFromAsset(ctx.assets, "fonts/poppins_bold.ttf") }
        .getOrElse { Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }.also { poppins = it }
    private fun mono(ctx: Context) = mono ?: runCatching { Typeface.createFromAsset(ctx.assets, "fonts/dejavu_sans_mono_bold.ttf") }
        .getOrElse { Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) }.also { mono = it }

    /** The lettering alone, white on transparent (the "clear" look; the others are built on it). */
    fun mask(ctx: Context, text: String, style: Style): Bitmap {
        val t = text.trim().ifEmpty { "App" }
        return when (style) {
            Style.Title -> title(ctx, t)
            Style.Pixel -> pixel(ctx, t.uppercase())
            Style.Tv -> tv(ctx, t.lowercase())
        }
    }

    /** One look of the icon, built on [mask]. */
    fun look(mask: Bitmap, style: Style, variant: String): Bitmap {
        if (variant == "clear") return mask.copy(Bitmap.Config.ARGB_8888, false)
        val out = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        when (variant) {
            "dark" -> c.drawColor(INK)
            "white" -> c.drawColor(Color.WHITE)
            else -> c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), Paint().apply {
                shader = LinearGradient(0f, 0f, W.toFloat(), H.toFloat(), style.c0, style.c1, Shader.TileMode.CLAMP)
            })
        }
        val p = Paint(Paint.FILTER_BITMAP_FLAG)
        if (variant == "white") p.colorFilter = PorterDuffColorFilter(INK, PorterDuff.Mode.SRC_IN)
        c.drawBitmap(mask, 0f, 0f, p)
        return out
    }

    fun png(b: Bitmap): ByteArray = ByteArrayOutputStream().also { b.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()

    /** Saves all four looks into the icon library (Android/data/…/files/appletv/<look>/<name>.png). Blocking. */
    fun saveToLibrary(ctx: Context, name: String, mask: Bitmap, style: Style) {
        val dir = com.glasslauncher.tv.ui.appleTvDir(ctx)
        val file = name.replace(Regex("[\\\\/:*?\"<>|]+"), " ").trim().ifEmpty { "Icon" } + ".png"
        for (v in VARIANTS) {
            val b = look(mask, style, v)
            File(dir, v).apply { mkdirs() }.resolve(file).writeBytes(png(b))
            b.recycle()
        }
    }

    /** Copies the icons that ship with the app (assets/appletv) into the library, without replacing any. Blocking. */
    fun installBundled(ctx: Context) = runCatching {
        val dir = com.glasslauncher.tv.ui.appleTvDir(ctx)
        for (v in VARIANTS) {
            val names = ctx.assets.list("appletv/$v").orEmpty()
            if (names.isEmpty()) continue
            val to = File(dir, v).apply { mkdirs() }
            for (n in names) {
                val f = File(to, n)
                if (!f.exists()) ctx.assets.open("appletv/$v/$n").use { i -> f.outputStream().use { i.copyTo(it) } }
            }
        }
    }

    // ---- styles -------------------------------------------------------------------------------------------

    private fun blank() = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
    private fun white() = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

    /** Text sized to fit [maxW] × [maxH], centred on (cx, cy) by its drawn bounds. */
    private fun fitText(c: Canvas, text: String, p: Paint, maxW: Float, maxH: Float, cx: Float, cy: Float): RectF {
        p.textSize = 200f
        val r = Rect(); p.getTextBounds(text, 0, text.length, r)
        val k = minOf(maxW / r.width().coerceAtLeast(1), maxH / r.height().coerceAtLeast(1))
        p.textSize = 200f * k
        p.getTextBounds(text, 0, text.length, r)
        val x = cx - r.exactCenterX(); val y = cy - r.exactCenterY()
        c.drawText(text, x, y, p)
        return RectF(x + r.left, y + r.top, x + r.right, y + r.bottom)
    }

    /** Movie-title lettering, as large as the tile allows. */
    private fun title(ctx: Context, text: String): Bitmap {
        val b = blank(); val c = Canvas(b)
        val p = white().apply { typeface = poppins(ctx); letterSpacing = 0.02f }
        fitText(c, text, p, W - 80f, 420f, W / 2f, H / 2f)
        return b
    }

    /** Old game-screen lettering: drawn tiny without smoothing, then blown up into hard square pixels. */
    private fun pixel(ctx: Context, text: String): Bitmap {
        val p = Paint().apply { isAntiAlias = false; color = Color.WHITE; typeface = mono(ctx); textSize = 13f; letterSpacing = 1f / 13f }
        val sw = p.measureText(text).toInt() + 6
        val small = Bitmap.createBitmap(sw.coerceAtLeast(8), 22, Bitmap.Config.ARGB_8888)
        Canvas(small).drawText(text, 3f, 16f, p)
        val bb = alphaBounds(small) ?: return blank()
        val crop = Bitmap.createBitmap(small, bb.left, bb.top, bb.width(), bb.height())
        val scale = maxOf(1, minOf((W - 80) / crop.width, 380 / crop.height))
        val b = blank(); val c = Canvas(b)
        val w = crop.width * scale; val h = crop.height * scale
        val l = (W - w) / 2; val t = (H - h) / 2
        c.drawBitmap(crop, null, Rect(l, t, l + w, t + h), Paint().apply { isFilterBitmap = false })
        small.recycle(); crop.recycle()
        return b
    }

    /** Rounded lettering with old-TV scanlines through it, inside screen-corner brackets. */
    private fun tv(ctx: Context, text: String): Bitmap {
        val b = blank(); val c = Canvas(b)
        val p = white().apply { typeface = poppins(ctx); letterSpacing = -0.01f }
        val r = fitText(c, text, p, 1000f, 300f, W / 2f, H / 2f)
        // scanlines cut through the letters
        val clear = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
        var y = r.top - 6f
        while (y < r.bottom + 6f) { c.drawRect(0f, y, W.toFloat(), y + 9f, clear); y += 27f }
        // brackets just outside the screen area
        val x0 = 40f; val x1 = W - 40f; val y0 = H / 2f - 242f; val y1 = H / 2f + 242f
        val rad = 74f; val len = 136f
        val stroke = white().apply { style = Paint.Style.STROKE; strokeWidth = 20f; strokeCap = Paint.Cap.BUTT }
        fun corner(sx: Float, sy: Float, ax: Float, ay: Float) {
            c.save(); c.translate(ax, ay); c.scale(sx, sy)
            val h = stroke.strokeWidth / 2
            val path = Path().apply {
                moveTo(h, len); lineTo(h, rad)
                arcTo(RectF(h, h, h + 2 * (rad - h), h + 2 * (rad - h)), 180f, 90f)
                lineTo(len, h)
            }
            c.drawPath(path, stroke); c.restore()
        }
        corner(1f, 1f, x0, y0); corner(-1f, 1f, x1, y0); corner(1f, -1f, x0, y1); corner(-1f, -1f, x1, y1)
        return b
    }

    private fun alphaBounds(b: Bitmap): Rect? {
        var l = b.width; var t = b.height; var r = -1; var btm = -1
        val px = IntArray(b.width * b.height); b.getPixels(px, 0, b.width, 0, 0, b.width, b.height)
        for (y in 0 until b.height) for (x in 0 until b.width) if (px[y * b.width + x] ushr 24 > 127) {
            if (x < l) l = x; if (x > r) r = x; if (y < t) t = y; if (y > btm) btm = y
        }
        return if (r < 0) null else Rect(l, t, r + 1, btm + 1)
    }
}

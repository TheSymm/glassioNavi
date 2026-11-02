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
        Outline("Outline", 0xFF0EA5E9.toInt(), 0xFF0B2545.toInt()),
        Neon("Neon", 0xFFDB2777.toInt(), 0xFF2E1065.toInt()),
        Script("Script", 0xFFF59E0B.toInt(), 0xFF7C2D12.toInt()),
        Serif("Serif", 0xFF475569.toInt(), 0xFF0F172A.toInt()),
        Tall("Tall", 0xFF22C55E.toInt(), 0xFF14532D.toInt()),
        Thin("Thin", 0xFF94A3B8.toInt(), 0xFF1E293B.toInt()),
        Stencil("Stencil", 0xFF65A30D.toInt(), 0xFF1A2E05.toInt()),
        Badge("Badge", 0xFF2563EB.toInt(), 0xFF172554.toInt()),
        Shadow("Shadow", 0xFFF97316.toInt(), 0xFF431407.toInt()),
        Terminal("Terminal", 0xFF16A34A.toInt(), 0xFF052E16.toInt()),
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
            Style.Outline -> outline(ctx, t)
            Style.Neon -> neon(ctx, t)
            Style.Script -> script(t)
            Style.Serif -> serif(t.uppercase())
            Style.Tall -> tall(t.uppercase())
            Style.Thin -> thin(t.lowercase())
            Style.Stencil -> stencil(ctx, t.uppercase())
            Style.Badge -> badge(ctx, t)
            Style.Shadow -> shadow(ctx, t)
            Style.Terminal -> terminal(ctx, t.lowercase())
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

    /** Copies the icons that ship with the app (assets/appletv) into the library, refreshing them when this build's differ. Blocking. */
    fun installBundled(ctx: Context) = runCatching {
        val dir = com.glasslauncher.tv.ui.appleTvDir(ctx)
        for (v in VARIANTS) {
            val names = ctx.assets.list("appletv/$v").orEmpty()
            if (names.isEmpty()) continue
            val to = File(dir, v).apply { mkdirs() }
            for (n in names) {
                val f = File(to, n)
                val bytes = ctx.assets.open("appletv/$v/$n").use { it.readBytes() }
                if (!f.exists() || f.length() != bytes.size.toLong()) f.writeBytes(bytes)
            }
        }
    }

    // ---- styles -------------------------------------------------------------------------------------------
    // Every style keeps its lettering inside the same frame: about 70 % of the tile wide, with room around it.

    private const val BOX_W = 900f
    private const val BOX_H = 300f

    private fun blank() = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
    private fun white() = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private fun sys(family: String, bold: Boolean = false) = Typeface.create(family, if (bold) Typeface.BOLD else Typeface.NORMAL)

    /** Text sized to fit [maxW] × [maxH], centred on (cx, cy) by its drawn bounds. */
    private fun fitText(c: Canvas, text: String, p: Paint, maxW: Float, maxH: Float, cx: Float = W / 2f, cy: Float = H / 2f, draw: Boolean = true): RectF {
        p.textSize = 200f
        val r = Rect(); p.getTextBounds(text, 0, text.length, r)
        val k = minOf(maxW / r.width().coerceAtLeast(1), maxH / r.height().coerceAtLeast(1))
        p.textSize = 200f * k
        p.getTextBounds(text, 0, text.length, r)
        val x = cx - r.exactCenterX(); val y = cy - r.exactCenterY()
        if (draw) c.drawText(text, x, y, p)
        lastX = x; lastY = y
        return RectF(x + r.left, y + r.top, x + r.right, y + r.bottom)
    }
    private var lastX = 0f; private var lastY = 0f

    private fun simple(p: Paint, text: String, maxW: Float = BOX_W, maxH: Float = BOX_H): Bitmap {
        val b = blank(); fitText(Canvas(b), text, p, maxW, maxH); return b
    }

    /** Movie-title lettering. */
    private fun title(ctx: Context, text: String) = simple(white().apply { typeface = poppins(ctx); letterSpacing = 0.02f }, text, BOX_W, 340f)

    /** Old game-screen lettering: drawn tiny without smoothing, then blown up into hard square pixels. */
    private fun pixel(ctx: Context, text: String): Bitmap {
        val p = Paint().apply { isAntiAlias = false; color = Color.WHITE; typeface = mono(ctx); textSize = 13f; letterSpacing = 1f / 13f }
        val sw = p.measureText(text).toInt() + 6
        val small = Bitmap.createBitmap(sw.coerceAtLeast(8), 22, Bitmap.Config.ARGB_8888)
        Canvas(small).drawText(text, 3f, 16f, p)
        val bb = alphaBounds(small) ?: return blank()
        val crop = Bitmap.createBitmap(small, bb.left, bb.top, bb.width(), bb.height())
        val scale = maxOf(1, minOf(BOX_W.toInt() / crop.width, BOX_H.toInt() / crop.height))
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
        val r = fitText(c, text, p, 780f, 230f)
        val clear = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
        var y = r.top - 6f
        while (y < r.bottom + 6f) { c.drawRect(0f, y, W.toFloat(), y + 7f, clear); y += 22f }
        val x0 = 170f; val x1 = W - 170f; val y0 = H / 2f - 195f; val y1 = H / 2f + 195f
        val rad = 60f; val len = 110f
        val stroke = white().apply { style = Paint.Style.STROKE; strokeWidth = 16f; strokeCap = Paint.Cap.BUTT }
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

    /** Hollow letters: a clean stroke round bold type. */
    private fun outline(ctx: Context, text: String): Bitmap {
        val b = blank(); val c = Canvas(b)
        val p = white().apply { typeface = poppins(ctx); style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND }
        fitText(c, text, p.apply { strokeWidth = 0f }, BOX_W, BOX_H, draw = false)
        p.strokeWidth = (p.textSize * 0.035f).coerceIn(5f, 14f)
        c.drawText(text, lastX, lastY, p)
        return b
    }

    /** Neon tube: a soft glow behind a thin bright line. */
    private fun neon(ctx: Context, text: String): Bitmap {
        val b = blank(); val c = Canvas(b)
        val p = white().apply { typeface = sys("sans-serif-light"); style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND }
        fitText(c, text, p, BOX_W - 60f, BOX_H - 40f, draw = false)
        val w = (p.textSize * 0.03f).coerceIn(4f, 10f)
        val glow = Paint(p).apply { strokeWidth = w * 4f; alpha = 150; maskFilter = android.graphics.BlurMaskFilter(w * 4f, android.graphics.BlurMaskFilter.Blur.NORMAL) }
        c.drawText(text, lastX, lastY, glow)
        c.drawText(text, lastX, lastY, Paint(p).apply { strokeWidth = w })
        return b
    }

    /** Handwritten script (the TV's own cursive font). */
    private fun script(text: String) = simple(white().apply { typeface = sys("cursive", true) }, text, BOX_W, 320f)

    /** Classic serif capitals with wide spacing, like a book cover. */
    private fun serif(text: String) = simple(white().apply { typeface = sys("serif"); letterSpacing = 0.18f }, text, BOX_W, 170f)

    /** Tall condensed capitals, poster style. */
    private fun tall(text: String) = simple(white().apply { typeface = sys("sans-serif-condensed", true); letterSpacing = 0.02f }, text, BOX_W, 330f)

    /** Very light lowercase with air between the letters. */
    private fun thin(text: String) = simple(white().apply { typeface = sys("sans-serif-thin"); letterSpacing = 0.12f }, text, BOX_W, 200f)

    /** Stencil capitals: a band cut through the middle of the letters. */
    private fun stencil(ctx: Context, text: String): Bitmap {
        val b = blank(); val c = Canvas(b)
        val r = fitText(c, text, white().apply { typeface = poppins(ctx); letterSpacing = 0.06f }, BOX_W, 250f)
        val clear = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
        val gap = (r.height() * 0.07f).coerceAtLeast(6f)
        c.drawRect(0f, r.centerY() - gap / 2, W.toFloat(), r.centerY() + gap / 2, clear)
        return b
    }

    /** The name inside a rounded pill outline, like a label. */
    private fun badge(ctx: Context, text: String): Bitmap {
        val b = blank(); val c = Canvas(b)
        val p = white().apply { typeface = sys("sans-serif-medium"); letterSpacing = 0.04f }
        val r = fitText(c, text, p, BOX_W - 150f, 160f)
        val padX = 70f; val padY = 55f
        val box = RectF(r.left - padX, r.top - padY, r.right + padX, r.bottom + padY)
        c.drawRoundRect(box, box.height() / 2, box.height() / 2, white().apply { style = Paint.Style.STROKE; strokeWidth = 14f })
        return b
    }

    /** Bold type with a long soft drop shadow falling to the lower right. */
    private fun shadow(ctx: Context, text: String): Bitmap {
        val b = blank(); val c = Canvas(b)
        val p = white().apply { typeface = poppins(ctx) }
        fitText(c, text, p, BOX_W - 40f, 290f, cx = W / 2f - 12f, cy = H / 2f - 12f, draw = false)
        val sh = Paint(p).apply { alpha = 22 }
        for (i in 24 downTo 1) c.drawText(text, lastX + i * 1.5f, lastY + i * 1.5f, sh)
        c.drawText(text, lastX, lastY, p)
        return b
    }

    /** Command-line prompt: "> name" in monospace with a block cursor. */
    private fun terminal(ctx: Context, text: String): Bitmap {
        val b = blank(); val c = Canvas(b)
        val line = "> $text"
        val p = white().apply { typeface = mono(ctx) }
        fitText(c, line + "  ", p, BOX_W, 200f, draw = false)
        c.drawText(line, lastX, lastY, p)
        val cw = p.measureText("M")
        val cx = lastX + p.measureText("$line ")
        val fm = p.fontMetrics
        c.drawRect(cx, lastY + fm.ascent * 0.72f, cx + cw, lastY + fm.descent * 0.4f, white())
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

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
        Italic("Italic", 0xFF8B5CF6.toInt(), 0xFF2E1065.toInt()),
        Wide("Wide", 0xFF0EA5E9.toInt(), 0xFF082F49.toInt()),
        Squash("Squash", 0xFFF43F5E.toInt(), 0xFF4C0519.toInt()),
        Underline("Underline", 0xFF14B8A6.toInt(), 0xFF042F2E.toInt()),
        Strike("Strike", 0xFF64748B.toInt(), 0xFF0F172A.toInt()),
        Boxed("Boxed", 0xFF3B82F6.toInt(), 0xFF172554.toInt()),
        Inverted("Inverted", 0xFFA855F7.toInt(), 0xFF3B0764.toInt()),
        Echo("Echo", 0xFFF97316.toInt(), 0xFF431407.toInt()),
        Glitch("Glitch", 0xFFEC4899.toInt(), 0xFF1E1B4B.toInt()),
        Halftone("Halftone", 0xFFEAB308.toInt(), 0xFF713F12.toInt()),
        Stripes("Stripes", 0xFF22C55E.toInt(), 0xFF052E16.toInt()),
        SmallCaps("Small caps", 0xFF78716C.toInt(), 0xFF1C1917.toInt()),
        Casual("Casual", 0xFFFB923C.toInt(), 0xFF7C2D12.toInt()),
        Mono("Mono", 0xFF52525B.toInt(), 0xFF09090B.toInt()),
        Fade("Fade", 0xFF6366F1.toInt(), 0xFF1E1B4B.toInt()),
        Wave("Wave", 0xFF06B6D4.toInt(), 0xFF083344.toInt()),
        Stacked("Stacked", 0xFFEF4444.toInt(), 0xFF450A0A.toInt()),
        Arc("Arc", 0xFF84CC16.toInt(), 0xFF1A2E05.toInt()),
        Tilt("Tilt", 0xFFD946EF.toInt(), 0xFF4A044E.toInt()),
        Sticker("Sticker", 0xFFF59E0B.toInt(), 0xFF451A03.toInt()),
    }

    private const val INK = 0xFF121214.toInt()

    /** Background colours for the Default look (light → dark gradient). null = the style's own colours. */
    class Colours(val label: String, val c0: Int, val c1: Int)
    val COLOURS = listOf(
        Colours("Purple", 0xFF7C3AED.toInt(), 0xFF1E1B4B.toInt()),
        Colours("Blue", 0xFF2563EB.toInt(), 0xFF172554.toInt()),
        Colours("Sky", 0xFF0EA5E9.toInt(), 0xFF0B2545.toInt()),
        Colours("Teal", 0xFF14B8A6.toInt(), 0xFF134E4A.toInt()),
        Colours("Green", 0xFF22C55E.toInt(), 0xFF14532D.toInt()),
        Colours("Yellow", 0xFFEAB308.toInt(), 0xFF713F12.toInt()),
        Colours("Orange", 0xFFF97316.toInt(), 0xFF431407.toInt()),
        Colours("Red", 0xFFEF4444.toInt(), 0xFF5A140A.toInt()),
        Colours("Pink", 0xFFEC4899.toInt(), 0xFF500724.toInt()),
        Colours("Grey", 0xFF64748B.toInt(), 0xFF0F172A.toInt()),
        Colours("Black", 0xFF27272A.toInt(), 0xFF09090B.toInt()),
    )
    private var poppins: Typeface? = null
    private var mono: Typeface? = null
    private fun poppins(ctx: Context) = poppins ?: runCatching { Typeface.createFromAsset(ctx.assets, "fonts/poppins_bold.ttf") }
        .getOrElse { Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }.also { poppins = it }
    private fun mono(ctx: Context) = mono ?: runCatching { Typeface.createFromAsset(ctx.assets, "fonts/dejavu_sans_mono_bold.ttf") }
        .getOrElse { Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) }.also { mono = it }

    /** The lettering alone, white on transparent (the "clear" look; the others are built on it). */
    fun mask(ctx: Context, text: String, style: Style, dy: Int = 0): Bitmap {
        val m = draw(ctx, text, style)
        if (dy == 0) return m
        // Moved down (positive) or up (negative) by dy pixels of the 1280 × 768 icon.
        return blank().also { Canvas(it).drawBitmap(m, 0f, dy.toFloat(), null); m.recycle() }
    }

    private fun draw(ctx: Context, text: String, style: Style): Bitmap {
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
            Style.Italic -> simple(white().apply { typeface = poppins(ctx); textSkewX = -0.25f }, t, BOX_W - 40f, 320f)
            Style.Wide -> simple(white().apply { typeface = poppins(ctx); textScaleX = 1.4f; letterSpacing = 0.04f }, t.uppercase(), BOX_W, 220f)
            Style.Squash -> simple(white().apply { typeface = poppins(ctx); textScaleX = 0.62f }, t.uppercase(), 760f, 330f)
            Style.Underline -> underline(ctx, t)
            Style.Strike -> strike(t.uppercase())
            Style.Boxed -> boxed(t.uppercase())
            Style.Inverted -> inverted(ctx, t)
            Style.Echo -> echo(ctx, t)
            Style.Glitch -> glitch(ctx, t.uppercase())
            Style.Halftone -> pattern(ctx, t, dots = true)
            Style.Stripes -> pattern(ctx, t.uppercase(), dots = false)
            Style.SmallCaps -> simple(white().apply { typeface = sys("sans-serif-smallcaps"); letterSpacing = 0.1f }, t, BOX_W, 220f)
            Style.Casual -> simple(white().apply { typeface = sys("casual", true) }, t, BOX_W, 300f)
            Style.Mono -> simple(white().apply { typeface = Typeface.MONOSPACE; letterSpacing = 0.08f }, t.lowercase(), BOX_W, 200f)
            Style.Fade -> fade(ctx, t)
            Style.Wave -> wave(ctx, t)
            Style.Stacked -> stacked(ctx, t.uppercase())
            Style.Arc -> arc(ctx, t.uppercase())
            Style.Tilt -> tilt(ctx, t)
            Style.Sticker -> sticker(ctx, t)
        }
    }

    /** One look of the icon, built on [mask]. */
    fun look(mask: Bitmap, style: Style, variant: String, colours: Colours? = null): Bitmap {
        if (variant == "clear") return mask.copy(Bitmap.Config.ARGB_8888, false)
        val out = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        when (variant) {
            "dark" -> c.drawColor(INK)
            "white" -> c.drawColor(Color.WHITE)
            else -> c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), Paint().apply {
                shader = LinearGradient(0f, 0f, W.toFloat(), H.toFloat(), colours?.c0 ?: style.c0, colours?.c1 ?: style.c1, Shader.TileMode.CLAMP)
            })
        }
        val p = Paint(Paint.FILTER_BITMAP_FLAG)
        if (variant == "white") p.colorFilter = PorterDuffColorFilter(INK, PorterDuff.Mode.SRC_IN)
        c.drawBitmap(mask, 0f, 0f, p)
        return out
    }

    fun png(b: Bitmap): ByteArray = ByteArrayOutputStream().also { b.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()

    /** Saves all four looks into the icon library (Android/data/…/files/appletv/<look>/<name>.png). Blocking. */
    fun saveToLibrary(ctx: Context, name: String, mask: Bitmap, style: Style, colours: Colours? = null) {
        val dir = com.glasslauncher.tv.ui.appleTvDir(ctx)
        val file = name.replace(Regex("[\\\\/:*?\"<>|]+"), " ").trim().ifEmpty { "Icon" } + ".png"
        for (v in VARIANTS) {
            val b = look(mask, style, v, colours)
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

    // ---- the twenty added later --------------------------------------------------------------------------

    private val dstIn get() = Paint(Paint.FILTER_BITMAP_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }

    /** Bold type with a thick bar underneath. */
    private fun underline(ctx: Context, text: String): Bitmap {
        val b = blank(); val c = Canvas(b)
        val r = fitText(c, text, white().apply { typeface = poppins(ctx) }, BOX_W, 260f, cy = H / 2f - 30f)
        val h = (r.height() * 0.09f).coerceIn(10f, 26f)
        c.drawRect(r.left, r.bottom + h * 2.2f, r.right, r.bottom + h * 3.2f, white())
        return b
    }

    /** Light capitals with a line through the middle. */
    private fun strike(text: String): Bitmap {
        val b = blank(); val c = Canvas(b)
        val r = fitText(c, text, white().apply { typeface = sys("sans-serif-light"); letterSpacing = 0.1f }, BOX_W - 60f, 200f)
        val h = (r.height() * 0.07f).coerceIn(6f, 16f)
        c.drawRect(r.left - 30f, r.centerY() - h / 2, r.right + 30f, r.centerY() + h / 2, white())
        return b
    }

    /** Spaced capitals inside a square frame. */
    private fun boxed(text: String): Bitmap {
        val b = blank(); val c = Canvas(b)
        val r = fitText(c, text, white().apply { typeface = sys("sans-serif-medium"); letterSpacing = 0.16f }, BOX_W - 160f, 150f)
        val box = RectF(r.left - 70f, r.top - 60f, r.right + 70f, r.bottom + 60f)
        c.drawRect(box, white().apply { style = Paint.Style.STROKE; strokeWidth = 12f })
        return b
    }

    /** A solid rounded block with the name cut out of it. */
    private fun inverted(ctx: Context, text: String): Bitmap {
        val b = blank(); val c = Canvas(b)
        val p = white().apply { typeface = poppins(ctx) }
        val r = fitText(c, text, p, BOX_W - 150f, 200f, draw = false)
        val box = RectF(r.left - 70f, r.top - 60f, r.right + 70f, r.bottom + 60f)
        c.drawRoundRect(box, 46f, 46f, white())
        c.drawText(text, lastX, lastY, Paint(p).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) })
        return b
    }

    /** Two fainter copies trailing behind the name. */
    private fun echo(ctx: Context, text: String): Bitmap {
        val b = blank(); val c = Canvas(b)
        val p = white().apply { typeface = poppins(ctx) }
        fitText(c, text, p, BOX_W - 80f, 270f, cx = W / 2f + 20f, cy = H / 2f + 20f, draw = false)
        val step = (p.textSize * 0.06f).coerceIn(8f, 22f)
        c.drawText(text, lastX - step * 2, lastY - step * 2, Paint(p).apply { alpha = 60 })
        c.drawText(text, lastX - step, lastY - step, Paint(p).apply { alpha = 120 })
        c.drawText(text, lastX, lastY, p)
        return b
    }

    /** Capitals sliced into bands that slip sideways, like a broken signal. */
    private fun glitch(ctx: Context, text: String): Bitmap {
        val src = blank()
        val r = fitText(Canvas(src), text, white().apply { typeface = poppins(ctx) }, BOX_W - 80f, 260f)
        val b = blank(); val c = Canvas(b)
        val shifts = floatArrayOf(0f, 22f, -14f, 0f, 30f, -24f, 8f, 0f, -18f, 12f)
        val bands = shifts.size
        val h = (r.height() + 20f) / bands
        for (i in 0 until bands) {
            val top = (r.top - 10f + i * h).toInt(); val bot = (r.top - 10f + (i + 1) * h).toInt()
            c.save(); c.clipRect(0, top, W, bot); c.drawBitmap(src, shifts[i], 0f, null); c.restore()
        }
        src.recycle()
        return b
    }

    /** Bold letters filled with a dot screen, or with diagonal stripes. */
    private fun pattern(ctx: Context, text: String, dots: Boolean): Bitmap {
        val b = simple(white().apply { typeface = poppins(ctx) }, text, BOX_W, 320f)
        val pat = blank(); val pc = Canvas(pat); val w = white()
        if (dots) {
            var y = 0f; while (y < H) { var x = 0f; while (x < W) { pc.drawCircle(x, y, 5.5f, w); x += 15f }; y += 15f }
        } else {
            w.strokeWidth = 11f
            var x = -H.toFloat(); while (x < W) { pc.drawLine(x, H.toFloat(), x + H, 0f, w); x += 22f }
        }
        Canvas(b).drawBitmap(pat, 0f, 0f, dstIn)
        pat.recycle()
        return b
    }

    /** Solid at the top, fading away towards the bottom. */
    private fun fade(ctx: Context, text: String): Bitmap {
        val b = blank(); val c = Canvas(b)
        val r = fitText(c, text, white().apply { typeface = poppins(ctx) }, BOX_W, 320f)
        val g = Paint().apply {
            shader = LinearGradient(0f, r.top, 0f, r.bottom, Color.WHITE, Color.argb(40, 255, 255, 255), Shader.TileMode.CLAMP)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), g)
        return b
    }

    /** Letters bobbing up and down along a wave. */
    private fun wave(ctx: Context, text: String): Bitmap {
        val b = blank(); val c = Canvas(b)
        val p = white().apply { typeface = poppins(ctx) }
        fitText(c, text, p, BOX_W - 40f, 220f, draw = false)
        val amp = p.textSize * 0.12f
        var x = lastX
        text.forEachIndexed { i, ch ->
            c.drawText(ch.toString(), x, lastY + (kotlin.math.sin(i * 0.9) * amp).toFloat(), p)
            x += p.measureText(ch.toString())
        }
        return b
    }

    /** The name split over two lines, each as wide as the other. */
    private fun stacked(ctx: Context, text: String): Bitmap {
        val words = text.trim().split(Regex("\\s+"))
        val (a, z) = if (words.size >= 2) words.first() to words.drop(1).joinToString(" ")
            else text.length.let { n -> if (n < 4) return title(ctx, text) else text.substring(0, (n + 1) / 2) to text.substring((n + 1) / 2) }
        val b = blank(); val c = Canvas(b)
        fitText(c, a, white().apply { typeface = poppins(ctx) }, 720f, 175f, cy = H / 2f - 100f)
        fitText(c, z, white().apply { typeface = poppins(ctx) }, 720f, 175f, cy = H / 2f + 100f)
        return b
    }

    /** Capitals curving over the top of an arch. */
    private fun arc(ctx: Context, text: String): Bitmap {
        val b = blank(); val c = Canvas(b)
        val p = white().apply { typeface = poppins(ctx); letterSpacing = 0.06f }
        fitText(c, text, p, 820f, 190f, draw = false)
        val rad = 900f; val cy = H / 2f + 70f + rad
        val path = Path().apply { addArc(RectF(W / 2f - rad, cy - rad, W / 2f + rad, cy + rad), 238f, 64f) }
        val len = android.graphics.PathMeasure(path, false).length
        c.drawTextOnPath(text, path, (len - p.measureText(text)) / 2, 0f, p)
        return b
    }

    /** Bold type at a jaunty angle. */
    private fun tilt(ctx: Context, text: String): Bitmap {
        val b = blank(); val c = Canvas(b)
        c.rotate(-8f, W / 2f, H / 2f)
        fitText(c, text, white().apply { typeface = poppins(ctx) }, BOX_W - 120f, 260f)
        return b
    }

    /** Bold letters with a cut-out outline round them, like a sticker. */
    private fun sticker(ctx: Context, text: String): Bitmap {
        val b = blank(); val c = Canvas(b)
        val p = white().apply { typeface = poppins(ctx); strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND }
        fitText(c, text, p, BOX_W - 120f, 260f, draw = false)
        val w = (p.textSize * 0.05f).coerceIn(8f, 22f)
        c.drawText(text, lastX, lastY, Paint(p).apply { style = Paint.Style.STROKE; strokeWidth = w * 3.6f })
        c.drawText(text, lastX, lastY, Paint(p).apply { style = Paint.Style.STROKE; strokeWidth = w * 2f; xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) })
        c.drawText(text, lastX, lastY, p)
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

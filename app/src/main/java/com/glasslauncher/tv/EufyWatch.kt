package com.glasslauncher.tv

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.glasslauncher.tv.data.Glog
import com.glasslauncher.tv.net.NotifyServer

/**
 * Eufy doorbell without Home Assistant: the eufy Security phone app runs on the TV with its notifications on, only to
 * say "the doorbell rang". Glassio watches for that notification and shows its own doorbell pop-up with the doorbell's
 * RTSP feed (eufy app → camera settings → NAS (RTSP)), added in Settings → Cameras and chosen as the doorbell camera.
 * A ring wakes a battery doorbell and starts its RTSP stream, so the feed is live just when it is needed.
 *
 * Needs notification access, which Google TV hides: Settings → Cameras → Eufy → "Allow Glassio to see notifications".
 */
class EufyWatch : NotificationListenerService() {
    companion object {
        /** eufy Security (the older eufy Security app id, and the newer eufy app). */
        val EUFY_PKGS = setOf("com.oceanwing.battery.cam", "com.eufylife.smarthome")
        /** Words in a eufy notification that mean the doorbell rang (not motion or a person walking past). */
        private val RING = Regex("(?i)ring|doorbell|door bell|visitor|someone (is )?at (the|your) door|pressed")
        private var lastRing = 0L
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in EUFY_PKGS) return
        val app = application as? GlassApp ?: return
        val st = app.settings
        val n = sbn.notification
        val title = n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = n.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        // Kept so Settings can show what eufy sent last (handy when the words don't match).
        st.eufyLast = "$title · $text".trim(' ', '·')
        Glog.i("Eufy notification: $title / $text")
        if (!st.eufyOpen) return
        if (st.eufyWhich != "all" && !RING.containsMatchIn("$title $text")) return
        // A ring often sends two or three notifications, and Home Assistant may have shown it already: once.
        val now = System.currentTimeMillis()
        if (now - lastRing < 15_000 || now - NotifyServer.lastShown < 8_000) return
        lastRing = now
        Glog.i("Eufy: doorbell")
        runCatching { NotifyServer.show(app, org.json.JSONObject()) }.onFailure { Glog.w("Eufy doorbell", it) }
    }
}

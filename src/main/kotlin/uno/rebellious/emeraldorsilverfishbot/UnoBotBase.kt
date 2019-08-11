package uno.rebellious.emeraldorsilverfishbot

import com.gikk.twirk.Twirk
import com.gikk.twirk.events.TwirkListener
import java.io.IOException

class UnoBotBase constructor(private val twirk: Twirk) : TwirkListener {
    override fun onDisconnect() {
        try {
            if (!twirk.connect())
                twirk.close()
        } catch (e: IOException) {
            twirk.close()
        } catch (e: InterruptedException) {
        }
    }
}
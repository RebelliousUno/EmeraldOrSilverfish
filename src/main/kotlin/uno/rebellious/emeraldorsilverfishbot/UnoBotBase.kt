package uno.rebellious.emeraldorsilverfishbot

import com.gikk.twirk.Twirk
import com.gikk.twirk.events.TwirkListener
import uno.rebellious.emeraldorsilverfishbot.model.Channel
import java.io.IOException

class UnoBotBase constructor(private val twirk: Twirk, private val channel: Channel) : TwirkListener {
    override fun onDisconnect() {
        try {
            if (!twirk.connect())
                twirk.close()
        } catch (e: IOException) {
            twirk.close()
        } catch (e: InterruptedException) {
        }
        BotManager.stopTwirkForChannel(channel.channel)
        BotManager.startTwirkForChannel(channel)
    }
}
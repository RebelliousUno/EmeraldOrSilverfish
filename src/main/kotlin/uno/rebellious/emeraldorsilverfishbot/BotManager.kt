package uno.rebellious.emeraldorsilverfishbot

import com.gikk.twirk.Twirk
import com.gikk.twirk.TwirkBuilder
import com.gikk.twirk.events.TwirkListener
import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable
import io.reactivex.subjects.BehaviorSubject
import uno.rebellious.emeraldorsilverfishbot.command.CommandManager
import uno.rebellious.emeraldorsilverfishbot.database.Channel
import uno.rebellious.emeraldorsilverfishbot.database.DatabaseDAO
import uno.rebellious.emeraldorsilverfishbot.model.Settings
import uno.rebellious.emeraldorsilverfishbot.pastebin.PastebinDAO
import java.util.*

object BotManager {

    private val scanner: Observable<String> = Scanner(System.`in`).toObservable().share()
    private val SETTINGS = Settings()
    private var threadList = HashMap<String, Thread>()

    val database = DatabaseDAO()
    val pastebin = PastebinDAO(SETTINGS.pastebinDev, SETTINGS.pastebinUser)

    fun startTwirkForChannel(channel: Channel) {
        val twirkThread = Thread(Runnable {
            val shouldStop = BehaviorSubject.create<Boolean>()
            shouldStop.onNext(false)
            val nick = if (channel.nick.isBlank()) SETTINGS.nick else channel.nick
            val password = if (channel.token.isBlank()) SETTINGS.password else channel.token

            val twirk = TwirkBuilder("#${channel.channel}", nick, password)
                .setVerboseMode(true)
                .build()
            twirk.connect()
            twirk.addIrcListener(CommandManager(twirk, channel))
            twirk.addIrcListener(getOnDisconnectListener(twirk))

            scanner
                .takeUntil { it == ".quit" }
                .subscribe {
                    if (it == ".quit") {
                        println("Quitting $channel")
                        twirk.close()
                    } else {
                        twirk.channelMessage(it)
                    }
                }
        })
        twirkThread.name = channel.channel
        twirkThread.start()
        threadList[channel.channel] = twirkThread
    }

    fun stopTwirkForChannel(channel: String) {
        val thread = threadList[channel]
        thread?.interrupt()
    }

    private fun getOnDisconnectListener(twirk: Twirk): TwirkListener? {
        return UnoBotBase(twirk)
    }
}
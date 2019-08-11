/**
 * Created by rebel on 16/07/2017.
 */

package uno.rebellious.emeraldorsilverfishbot.model

import java.util.*

class Settings {
    private val props = Properties()
    private var settings: DataSettings?

    init {
        props.load(this.javaClass.classLoader.getResourceAsStream("settings.properties"))
        settings = DataSettings(
            props.getProperty("nick"),
            props.getProperty("password"),
            props.getProperty("channel"),
            props.getProperty("pastebin_dev"),
            props.getProperty("pastebin_user")
        )
    }

    val nick: String?
        get() {
            return settings?.MY_NICK
        }

    val password: String?
        get() {
            return settings?.MY_PASS
        }
    val pastebinDev: String?
        get() {
            return settings?.PASTEBIN_DEV
        }
    val pastebinUser: String?
        get() {
            return settings?.PASTEBIN_USER
        }
}

data class DataSettings(
    val MY_NICK: String,
    val MY_PASS: String,
    val CHANNEL: String,
    val PASTEBIN_DEV: String,
    val PASTEBIN_USER: String
)

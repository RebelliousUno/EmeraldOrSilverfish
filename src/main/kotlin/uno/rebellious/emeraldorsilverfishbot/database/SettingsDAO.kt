package uno.rebellious.emeraldorsilverfishbot.database

import java.sql.Connection
import java.sql.DriverManager
import java.util.*

internal class SettingsDAO(private val connectionList: HashMap<String, Connection>) : ISettings {

    var settingsDB: Connection? = null

    init {
        connectSettings()
    }

    private fun connectSettings() {
        settingsDB = DriverManager.getConnection("jdbc:sqlite:settings.db")
    }

    override fun getAllCommandList(channel: String): ArrayList<String> {
        val connection = connectionList[channel]
        val sql = "SELECT command FROM responses"
        val returnList = ArrayList<String>()
        connection?.prepareStatement(sql)?.run {
            executeQuery()
        }?.apply {
            while (next()) returnList.add(getString("command"))
        }
        return returnList
    }

    override fun leaveChannel(channel: String) {
        val sql = "DELETE FROM channels WHERE channel = ?"
        settingsDB?.prepareStatement(sql)?.apply {
            setString(1, channel)
            executeUpdate()
        }
    }

    override fun addChannel(newChannel: String, prefix: String) {
        val exists = channelExists(newChannel)
        if (exists != null && !exists) {
            val sql = "INSERT INTO channels(channel, prefix) VALUES (?, ?)"
            settingsDB?.prepareStatement(sql)?.apply {
                setString(1, newChannel)
                setString(2, prefix)
                executeUpdate()
                connect(newChannel)
            }
        }
    }

    private fun connect(channel: String) {
        val con = DriverManager.getConnection("jdbc:sqlite:${channel.toLowerCase()}.db")
        connectionList[channel] = con
    }

    override fun getPrefixForChannel(channel: String): String {
        val sql = "Select prefix from channels where channel = ?"
        return settingsDB?.prepareStatement(sql)?.run {
            setString(1, channel)
            executeQuery()
        }?.run {
            if (next()) {
                getString("prefix")
            } else {
                "!" //Default to "!"
            }
        } ?: "!"
    }

    override fun setPrefixForChannel(channel: String, prefix: String) {
        val sql = "UPDATE channels set prefix = ? where channel = ?"
        settingsDB?.prepareStatement(sql)?.apply {
            setString(1, prefix)
            setString(2, channel)
            executeUpdate()
        }
    }

    private fun channelExists(channel: String): Boolean? {
        val sql = "Select * from channels WHERE channel = ?"
        return settingsDB?.prepareStatement(sql)?.run {
            setString(1, channel)
            executeQuery()
        }?.run { next() } ?: false
    }

    fun createChannelsTable() {
        val channelList = "create table if not exists channels (" +
                "channel text, prefix text DEFAULT '!', nick text default '', token text default '')"

        settingsDB?.createStatement()?.apply {
            queryTimeout = 30
            executeUpdate(channelList)
        }
    }

    override fun getListOfChannels(): Array<Channel> {
        val channelSelect = "select * from channels"
        val list = ArrayList<Channel>()
        settingsDB?.createStatement()?.run {
            queryTimeout = 30
            executeQuery(channelSelect)
        }?.apply {
            while (next()) {
                val channel = getString("channel")
                val prefix = getString("prefix")
                val nick = getString("nick")
                val token = getString("token")
                list.add(Channel(channel, prefix, nick, token))
            }
        }
        return list.toTypedArray()
    }
}
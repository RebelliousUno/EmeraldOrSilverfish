package uno.rebellious.emeraldorsilverfishbot.database

import com.gikk.twirk.types.users.TwitchUser
import uno.rebellious.emeraldorsilverfishbot.model.VoteType
import java.sql.Connection
import java.sql.DriverManager
import java.util.*

class DatabaseDAO : IDatabase {


    private var connectionList: HashMap<String, Connection> = HashMap()
    private val gameDAO = RoundsDAO(connectionList)
    private val settingsDAO = SettingsDAO(connectionList)

    init {
        setupSettings() //Set up Settings DB
        val channelList = settingsDAO.getListOfChannels()
        connect(channelList)
        setupAllChannels()
    }

    override fun getCorrectUsersOfGameRound(channel: String, gameId: Int, roundNumber: Int) = gameDAO.getCorrectUsersOfGameRound(channel, gameId, roundNumber)
    override fun playerVote(channel: String, vote: VoteType, user: TwitchUser) = gameDAO.playerVote(channel, vote, user)
    override fun found(channel: String, found: VoteType) = gameDAO.found(channel, found)
    override fun getRoundForGame(channel: String, gameId: Int) = gameDAO.getRoundForGame(channel, gameId)
    override fun startRound(channel: String, gameId: Int, roundNumber: Int) = gameDAO.startRound(channel, gameId, roundNumber)
    override fun getOpenGames(channel: String) = gameDAO.getOpenGames(channel)
    override fun startGame(channel: String) = gameDAO.startGame(channel)
    override fun endGame(channel: String) = gameDAO.endGame(channel)
    override fun endGame(channel: String, gameId: Int) = gameDAO.endGame(channel, gameId)
    override fun getWinners(channel: String) = gameDAO.getWinners(channel)
    override fun getLastGameWinnersURL(channel: String) = gameDAO.getLastGameWinnersURL(channel)
    override fun setLastGameWinnersURL(channel: String, url: String) = gameDAO.setLastGameWinnersURL(channel, url)

    private fun setupSettings() {
        settingsDAO.createChannelsTable()
        if (settingsDAO.getListOfChannels().isEmpty()) { // Set up default Channel
            addChannel("glazedhambot", "!")
        }
    }

    private fun connect(channels: Array<Channel>) {
        channels.forEach {
            val con = DriverManager.getConnection("jdbc:sqlite:${it.channel.toLowerCase()}.db")
            connectionList[it.channel] = con
        }
    }

    private fun setupAllChannels() {
        connectionList.forEach {
            gameDAO.createGameTable(it.value)
            gameDAO.createRoundsTable(it.value)
            gameDAO.createEntryTable(it.value
            )
        }
    }

    override fun getPrefixForChannel(channel: String): String  = settingsDAO.getPrefixForChannel(channel)

    override fun setPrefixForChannel(channel: String, prefix: String) = settingsDAO.setPrefixForChannel(channel, prefix)


    override fun addChannel(newChannel: String, prefix: String) = settingsDAO.addChannel(newChannel, prefix)

    override fun leaveChannel(channel: String) = settingsDAO.leaveChannel(channel)

    override fun getAllCommandList(channel: String): ArrayList<String> = settingsDAO.getAllCommandList(channel)

    override fun getListOfChannels(): Array<Channel> = settingsDAO.getListOfChannels()

}
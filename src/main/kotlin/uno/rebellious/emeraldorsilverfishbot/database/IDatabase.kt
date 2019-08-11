package uno.rebellious.emeraldorsilverfishbot.database

import com.gikk.twirk.types.users.TwitchUser
import uno.rebellious.emeraldorsilverfishbot.model.VoteRecorded
import uno.rebellious.emeraldorsilverfishbot.model.VoteType
import java.sql.Timestamp
import java.util.*

interface IDatabase : ISettings, IGame

interface IGame {
    fun startGame(channel: String): Int
    fun endGame(channel: String): Int
    fun endGame(channel: String, gameId: Int): Int
    fun getOpenGames(channel: String): List<Int>
    fun startRound(channel: String, gameId: Int, roundNumber: Int): Int
    fun getRoundForGame(channel: String, gameId: Int): Pair<Int, Int>
    fun found(channel: String, found: VoteType): Int
    fun playerVote(channel: String, vote: VoteType, user: TwitchUser): VoteRecorded
    fun getCorrectUsersOfGameRound(channel: String, gameId: Int, roundNumber: Int): List<String>
    fun getWinners(channel: String): List<Winner>
    fun getLastGameWinnersURL(channel: String): String
    fun setLastGameWinnersURL(channel: String, url: String)
}


interface ISettings {
    fun getAllCommandList(channel: String): ArrayList<String>
    fun leaveChannel(channel: String)
    fun addChannel(newChannel: String, prefix: String = "!")
    fun getPrefixForChannel(channel: String): String
    fun setPrefixForChannel(channel: String, prefix: String)
    fun getListOfChannels(): Array<Channel>
}
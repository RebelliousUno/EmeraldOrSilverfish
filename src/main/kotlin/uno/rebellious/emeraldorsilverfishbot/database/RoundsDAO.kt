package uno.rebellious.emeraldorsilverfishbot.database

import com.gikk.twirk.types.users.TwitchUser
import uno.rebellious.emeraldorsilverfishbot.model.VoteEligibility
import uno.rebellious.emeraldorsilverfishbot.model.VoteRecorded
import uno.rebellious.emeraldorsilverfishbot.model.VoteType
import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant

class RoundsDAO(private val connectionList: HashMap<String, Connection>) : IGame {

    private fun isPlayerEligibleForRound(
        channel: String,
        gameId: Int,
        currentRound: Int,
        user: TwitchUser
    ): VoteEligibility {
        return when {
            currentRound == 1 -> VoteEligibility.ELIGIBLE
            user.userName in getUsersForGameRound(channel, gameId, currentRound) -> VoteEligibility.ALREADY_VOTED
            user.userName in getCorrectUsersOfGameRound(channel, gameId, currentRound - 1) -> VoteEligibility.ELIGIBLE
            else -> VoteEligibility.NOT_ELIGIBLE
        }
    }

    private fun getUsersForGameRound(channel: String, gameId: Int, roundNumber: Int): List<String> {
        val users = ArrayList<String>()
        val usersSQL = """select user
from rounds
JOIN entries USING(roundId)
where rounds.gameId = ? and rounds.roundNum = ?
"""

        val connection = connectionList[channel]
        connection?.prepareStatement(usersSQL)?.run {
            setInt(1, gameId)
            setInt(2, roundNumber)
            executeQuery()
        }?.run {
            while (next()) {
                users.add(getString(1))
            }
        }
        return users
    }

    override fun getLastGameWinnersURL(channel: String): String {
        val sql = "select max(gameId), winnersUrl from games where games.endTime is not null"
        return connectionList[channel]?.createStatement()?.executeQuery(sql)?.run {
            if (next())
                getString(2) ?: ""
            else
                ""
        } ?: ""
    }

    override fun getWinners(channel: String): List<Winner> {
        val sql =
            """select roundNum, games.gameId, entries.user, games.startTime, games.endTime from (select roundId, max(roundNum) as roundNum, gameId, result from rounds
GROUP by gameId) as r
natural join entries
join games on games.gameId == r.gameId
where r.result = entries.vote"""
        val connection = connectionList[channel]
        val winners = ArrayList<Winner>()
        connection?.createStatement()?.run {
            executeQuery(sql)
        }?.run {
            while (next()) {
                val roundNum = getInt(1)
                val gameId = getInt(2)
                val user = getString(3)
                val startTime = getTimestamp(4)
                val endTime = getTimestamp(5)
                winners.add(Winner(gameId, roundNum, user, startTime, endTime))
            }
        }

        return winners
    }

    override fun getCorrectUsersOfGameRound(channel: String, gameId: Int, roundNumber: Int): List<String> {
        val correctUsers = ArrayList<String>()
        val usersSQL = """select user
from rounds
JOIN entries USING(roundId)
where rounds.gameId = ? and rounds.roundNum = ?
and entries.vote = rounds.result"""

        val connection = connectionList[channel]
        connection?.prepareStatement(usersSQL)?.run {
            setInt(1, gameId)
            setInt(2, roundNumber)
            executeQuery()
        }?.run {
            while (next()) {
                correctUsers.add(getString(1))
            }
        }
        return correctUsers
    }


    override fun playerVote(channel: String, vote: VoteType, user: TwitchUser): VoteRecorded {
        val gameList = getOpenGames(channel)
        val game = if (gameList.isNotEmpty()) gameList.first() else return VoteRecorded.NO_GAME_RUNNING
        val currentRound = getRoundForGame(channel, game)

        return when (isPlayerEligibleForRound(channel, game, currentRound.first, user)) {
            VoteEligibility.ELIGIBLE -> if (recordVote(
                    channel,
                    vote,
                    user,
                    currentRound.second
                )
            ) VoteRecorded.RECORDED else VoteRecorded.ERROR
            VoteEligibility.NOT_ELIGIBLE -> VoteRecorded.INELLIGIBLE
            VoteEligibility.ALREADY_VOTED -> VoteRecorded.ALREADY_VOTED
        }
    }

    private fun recordVote(channel: String, vote: VoteType, user: TwitchUser, currentRoundId: Int): Boolean {
        val voteSQL = "INSERT into entries (roundId, user, vote) VALUES (?, ?, ?)"
        val connection = connectionList[channel]
        return connection?.prepareStatement(voteSQL)?.run {
            setInt(1, currentRoundId)
            setString(2, user.userName)
            setString(3, vote.name)
            executeUpdate()
        } ?: -1 > 0
    }

    override fun found(channel: String, found: VoteType): Int {
        val game = getOpenGames(channel).max() ?: return -1
        val round = getRoundForGame(channel, game)
        return found(channel, game, round.first, found)
    }

    private fun found(channel: String, gameId: Int, roundNumber: Int, found: VoteType): Int {
        val connection = connectionList[channel]
        val updateSQL = "Update rounds SET result = ? where gameId = ? AND roundNum = ?"
        val rowsAffected = connection?.prepareStatement(updateSQL)?.run {
            setString(1, found.name)
            setInt(2, gameId)
            setInt(3, roundNumber)
            executeUpdate()
        } ?: -1

        return if (rowsAffected > 0) {
            startRound(channel, gameId, roundNumber + 1)
            getRoundForGame(channel, gameId).first
        } else {
            -1
        }
    }


    override fun startRound(channel: String, gameId: Int, roundNumber: Int): Int {
        val startRoundSQL = "insert into rounds (gameId, roundNum) VALUES (?, ?)"
        val connections = connectionList[channel]
        return connections?.prepareStatement(startRoundSQL)?.run {
            setInt(1, gameId)
            setInt(2, roundNumber)
            executeUpdate()
        } ?: -1
    }

    override fun startGame(channel: String): Int {
        val startGameSQL = "insert into games (startTime) VALUES (?)"
        val connection = connectionList[channel]
        return connection?.prepareStatement(startGameSQL)?.run {
            setTimestamp(1, Timestamp.from(Instant.now()))
            executeUpdate()
            val id = generatedKeys
            if (id.next())
                id.getInt(1)
            else -1
        } ?: -1
    }

    override fun getRoundForGame(channel: String, gameId: Int): Pair<Int, Int> {
        val roundSQL = "SELECT MAX(roundNum), roundId from rounds where gameId = ? "
        val connection = connectionList[channel]

        return connection?.prepareStatement(roundSQL)?.run {
            setInt(1, gameId)
            executeQuery()
        }?.run {
            if (next()) {
                Pair(getInt(1), getInt(2))
            } else {
                Pair(-1, -1)
            }
        } ?: Pair(-1, -1)
    }

    override fun getOpenGames(channel: String): List<Int> {
        val openGames = ArrayList<Int>()
        val sql = "SELECT gameId from games where endTime is null"
        val connection = connectionList[channel]
        connection?.createStatement()?.run {
            executeQuery(sql)
        }?.run {
            while (next()) {
                openGames.add(getInt(1))
            }
        }
        return openGames
    }

    private fun closeRounds(channel: String, gameId: Int): Int {
        val removeOpenRoundSQL = "DELETE from rounds WHERE gameId = ? and result is NULL"
        val connection = connectionList[channel]
        return connection?.prepareStatement(removeOpenRoundSQL)?.run {
            setInt(1, gameId)
            executeUpdate()
        } ?: 0
    }

    override fun endGame(channel: String): Int {
        val games = getOpenGames(channel)
        return if (games.isNotEmpty()) {
            val affectedRows = endGame(channel, games.first())
            if (affectedRows == 1) {
                //close open rounds
                closeRounds(channel, games.first())
                games.first()
            } else {
                -1
            }
        } else {
            -1
        }
    }

    override fun endGame(channel: String, gameId: Int): Int {
        val endGameSQL = "update games set endtime = ? where gameid = ?"
        val connection = connectionList[channel]
        return connection?.prepareStatement(endGameSQL)?.run {
            setTimestamp(1, Timestamp.from(Instant.now()))
            setInt(2, gameId)
            executeUpdate()
        } ?: -1
    }

    fun createGameTable(connection: Connection) {
        val gameTableSQL =
            "CREATE TABLE IF NOT EXISTS games (gameId INTEGER PRIMARY KEY AUTOINCREMENT, startTime INT, endTime INT, winnersUrl TEXT)"
        connection.createStatement()?.apply {
            queryTimeout = 30
            executeUpdate(gameTableSQL)
        }
    }

    fun createRoundsTable(connection: Connection) {
        val roundsTableSQL =
            """CREATE TABLE IF NOT EXISTS "rounds" (
	"roundId"	INTEGER PRIMARY KEY AUTOINCREMENT,
	"gameId"	INTEGER,
	"roundNum"	INT,
	"result"	TEXT,
	FOREIGN KEY("gameId") REFERENCES "games"("gameId")
)"""
        connection.createStatement()?.apply {
            queryTimeout = 30
            executeUpdate(roundsTableSQL)
        }
    }

    fun createEntryTable(connection: Connection) {
        val entryTableSQL = """CREATE TABLE IF NOT EXISTS "entries" (
	"roundId"	INTEGER NOT NULL,
	"user"	TEXT NOT NULL,
	"vote"	TEXT,
	PRIMARY KEY("user","roundId"),
	FOREIGN KEY("roundId") REFERENCES "rounds"("roundId")
)"""
        connection.createStatement()?.apply {
            queryTimeout = 30
            executeUpdate(entryTableSQL)
        }
    }

    override fun setLastGameWinnersURL(channel: String, url: String) {
        val updateSQL =
            "UPDATE games SET winnersUrl = ? where gameId in(select max(gameId) from games where endTime is not null)"
        connectionList[channel]?.prepareStatement(updateSQL)?.run {
            setString(1, url)
            executeUpdate()
        }
    }

}

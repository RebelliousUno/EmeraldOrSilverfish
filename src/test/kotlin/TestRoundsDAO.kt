package emeraldorsilverfish.tests.dao

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.*
import uno.rebellious.emeraldorsilverfishbot.database.RoundsDAO
import java.sql.Connection
import java.sql.DriverManager

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class TestRoundsDAO {

    lateinit var roundsDAO: RoundsDAO
    private var connectionList: HashMap<String, Connection> = HashMap()
    lateinit var con: Connection
    private val channel = "test"

    @BeforeEach
    fun setUpTestDB() {
        con = DriverManager.getConnection("jdbc:sqlite:$channel.db")
        connectionList[channel] = con
        roundsDAO = RoundsDAO(connectionList)

        roundsDAO.createEntryTable(connectionList[channel]!!)
        roundsDAO.createGameTable(connectionList[channel]!!)
        roundsDAO.createRoundsTable(connectionList[channel]!!)
    }

    @AfterEach
    fun clearDownTestDB() {
        con.close()
        con = DriverManager.getConnection("jdbc:sqlite:$channel.db")
        listOf("entries", "rounds", "games").forEach {
            val dropTablesSQL = "drop table IF EXISTS $it"
            con.createStatement()?.execute(dropTablesSQL)
        }
        con.close()
    }

    @Test
    fun testDatabaseSetup() {
        val nameList = ArrayList(listOf("entries", "rounds", "games"))
        val resultList = ArrayList<String>()
        con
            .createStatement()
            .executeQuery("select name from SQLITE_MASTER")?.run {
                while (next()) {
                    resultList.add(getString(1))
                }
            }
        assert(resultList.containsAll(nameList))
    }

    @Test
    fun testRoundsTableSetup() {
        val results = getTableCol("rounds")
        val expected = listOf("roundId", "gameId", "roundNum", "result")
        assert(results.containsAll(expected))
    }

    @Test
    fun testEntriesTableSetup() {
        val results = getTableCol("entries")
        val expected = listOf("roundId", "user", "vote")
        assert(results.containsAll(expected))
    }

    @Test
    fun testGamesTableSetup() {
        val results = getTableCol("games")
        val expected = listOf("gameId", "startTime", "endTime", "winnersUrl")
        assert(results.containsAll(expected))

    }

    @Test
    fun startGame() {
        val game = roundsDAO.startGame(channel)

        assertThat(game, `is`(1))
    }

    @Test
    fun startGameWhenOneRunning() {
        //logic for game running is done in the GameCommands Class
        val game1 = roundsDAO.startGame(channel)
        val game2 = roundsDAO.startGame(channel)
        assertThat(game1, `is`(1))
        assertThat(game2, `is`(2))
    }

    @Test
    fun roundsCreated() {
        //logic for creating a round when a game is started is done in GameCommands Class
        val game = roundsDAO.startGame(channel)
        var roundPair = roundsDAO.getRoundForGame(channel, game)
        assertThat(roundPair, `is`(Pair(0, 0)))

        val round = roundsDAO.startRound(channel, game, 1)
        roundPair = roundsDAO.getRoundForGame(channel, game)
        assertThat(round, `is`(1))
        assertThat(roundPair, `is`(Pair(1, 1)))
    }

    private fun getTableCol(tableName: String): ArrayList<String> {
        val resultList = ArrayList<String>()
        con.createStatement()
            .executeQuery("select name from pragma_table_info('$tableName')")?.run {
                while (next()) {
                    resultList.add(getString(1))
                }
            }
        return resultList
    }

}
package emeraldorsilverfish.tests.dao

import org.junit.jupiter.api.*
import uno.rebellious.emeraldorsilverfishbot.database.RoundsDAO
import java.sql.Connection
import java.sql.DriverManager

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestRoundsDAO() {

    lateinit var roundsDAO: RoundsDAO
    private var connectionList: HashMap<String, Connection> = HashMap()


    @BeforeAll
    fun setUp() {
        val con = DriverManager.getConnection("jdbc:sqlite:test.db")
        connectionList["test"] = con
        roundsDAO = RoundsDAO(connectionList)
    }

    @AfterAll
    fun tearDown() {

    }

    @BeforeEach
    fun setUpTestDB() {
        roundsDAO.createEntryTable(connectionList["test"]!!)
        roundsDAO.createGameTable(connectionList["test"]!!)
        roundsDAO.createRoundsTable(connectionList["test"]!!)
    }

    @AfterEach
    fun clearDownTestDB() {
        val con = connectionList["test"]!!
        con.autoCommit = false
        listOf("entries", "rounds", "games").forEach {
            val dropTablesSQL ="drop table IF EXISTS $it"
            con.createStatement()?.execute(dropTablesSQL)
        }
        con.commit()
        con.autoCommit = true
    }

    @Test
    fun testDatabaseSetup() {
        val con = connectionList["test"]!!
        val nameList = listOf("entries", "rounds", "games")
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

    private fun getTableCol(tableName: String): ArrayList<String> {
        val resultList = ArrayList<String>()
        val con = connectionList["test"]!!
        con.createStatement()
            .executeQuery("select name from pragma_table_info('$tableName')")?.run {
                while (next()) {
                    resultList.add(getString(1))
                }
            }
        return resultList
    }

}
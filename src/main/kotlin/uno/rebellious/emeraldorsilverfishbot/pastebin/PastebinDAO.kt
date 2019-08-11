package uno.rebellious.emeraldorsilverfishbot.pastebin

import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import uno.rebellious.emeraldorsilverfishbot.database.Winner

class PastebinDAO(devKey: String?, userKey: String?) {

    //private val expiry = Pair("api_paste_expire_date", "1W")
    private val apiOption = Pair("api_option", "paste")
    private val apiDevKey = Pair("api_dev_key", devKey)
    private val apiUserKey = Pair("api_user_key", userKey)

    private val path = "https://pastebin.com/api/api_post.php"


    fun parseWinners(winners: List<Winner>) : String {
        val winnerString = StringBuilder().append("Emerald or Silverfish Winners\n")
        with (winnerString) {
            winners.forEach {
                append("${it.user}\t\tGame ${it.gameId}\t${it.startTime.toLocalDateTime().toString()}\t${it.endTime.toLocalDateTime().toString()}")
                append("\n")
            }
        }
        return winnerString.toString()
    }

    fun createPaste(pasteTitle: String, pasteText: String): String {
        val title = Pair("api_paste_name", pasteTitle)
        val text = Pair("api_paste_code", pasteText)
        val body = listOf(apiDevKey, apiOption, apiUserKey, title, text)

        val (_, _, result) = path.httpPost(body).responseString()
        return when (result) {
            is Result.Failure -> {
                "Cannot Make Paste"
            }
            is Result.Success -> {
                result.get()
            }
        }
    }

}
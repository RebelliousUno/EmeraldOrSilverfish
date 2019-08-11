package uno.rebellious.emeraldorsilverfishbot.database

import java.sql.Timestamp

data class Winner(val gameId: Int, val roundNum: Int, val user: String, val startTime: Timestamp, val endTime: Timestamp)

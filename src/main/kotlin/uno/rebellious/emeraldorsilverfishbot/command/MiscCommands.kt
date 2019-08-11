package uno.rebellious.emeraldorsilverfishbot.command

import com.gikk.twirk.Twirk
import com.gikk.twirk.types.users.TwitchUser
import uno.rebellious.emeraldorsilverfishbot.database.DatabaseDAO


class MiscCommands(private val prefix: String, private val twirk: Twirk, private val  channel: String, private val database: DatabaseDAO) : CommandList() {
    init {
        commandList.add(helpCommand())
    }

    private fun helpCommand(): Command {
        return Command(prefix, "help" , "Usage: ${prefix}help cmd - to get help for a particular command", Permission(false, false, false)) { twitchUser: TwitchUser, content: List<String> ->
            if (content.size > 1) {
                twirk.channelMessage(commandList.firstOrNull { command -> command.command == content[1] && command.canUseCommand(twitchUser) }?.helpString)
            } else {
                twirk.channelMessage("Usage: ${prefix}help cmd - to get help for a particular command")
            }
        }
    }
}

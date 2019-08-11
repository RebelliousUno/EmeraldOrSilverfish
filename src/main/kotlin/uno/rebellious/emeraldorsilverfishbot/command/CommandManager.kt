package uno.rebellious.emeraldorsilverfishbot.command

import com.gikk.twirk.Twirk
import com.gikk.twirk.events.TwirkListener
import com.gikk.twirk.types.twitchMessage.TwitchMessage
import com.gikk.twirk.types.users.TwitchUser
import uno.rebellious.emeraldorsilverfishbot.BotManager
import uno.rebellious.emeraldorsilverfishbot.database.Channel
import java.util.*

class CommandManager (private val twirk: Twirk, channel: Channel): CommandList(), TwirkListener {

    private val commands = ArrayList<CommandList>()

    private var prefix = "^"
    private val database = BotManager.database
    private val pastebin = BotManager.pastebin

    init {
        prefix = channel.prefix
        commands.add(GameCommands(prefix, twirk, channel.channel, database, pastebin))
        commands.add(MiscCommands(prefix, twirk, channel.channel, database))
        commands.forEach {
            commandList.addAll(it.commandList)
        }
        commandList.add(commandListCommand())

        twirk.channelMessage("Starting up for ${channel.channel} - prefix is ${channel.prefix}")
    }

    private fun commandListCommand(): Command {
        return Command(prefix, "cmdlist", "Usage: ${prefix}cmdlist - lists the commands for this channel", Permission(false, false, false)) { twitchUser: TwitchUser, _: List<String> ->

            val gameCommands = commands
                    .first { it is GameCommands }
                    .commandList
                    .filter { it.canUseCommand(twitchUser) }
                    .map { command -> command.prefix + command.command }
                    .sorted()
            twirk.channelMessage("Game: $gameCommands")
        }
    }

    override fun onPrivMsg(sender: TwitchUser, message: TwitchMessage) {
        val content: String = message.content.trim()
        if (!content.startsWith(prefix)) return

        val splitContent = content.split(' ', ignoreCase = true, limit = 3)
        val command = splitContent[0].toLowerCase(Locale.ENGLISH)

        commandList
                .filter { command.startsWith("${it.prefix}${it.command}") }
                .firstOrNull { it.canUseCommand(sender) }
                ?.action?.invoke(sender, splitContent) ?: run {
        }
    }
}
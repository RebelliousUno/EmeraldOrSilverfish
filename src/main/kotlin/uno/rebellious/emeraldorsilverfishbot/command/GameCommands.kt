package uno.rebellious.emeraldorsilverfishbot.command

import com.gikk.twirk.Twirk
import com.gikk.twirk.types.users.TwitchUser
import uno.rebellious.emeraldorsilverfishbot.database.DatabaseDAO
import uno.rebellious.emeraldorsilverfishbot.model.VoteRecorded
import uno.rebellious.emeraldorsilverfishbot.model.VoteType
import uno.rebellious.emeraldorsilverfishbot.pastebin.PastebinDAO

class GameCommands(
    val prefix: String,
    val twirk: Twirk,
    val channel: String,
    val database: DatabaseDAO,
    val pastebin: PastebinDAO
) : CommandList() {

    companion object {
        val ownerOnly = Permission(true, false, false)
        val modOnly = Permission(false, true, false)
        val subOnly = Permission(false, false, true)
        val anyone = Permission(false, false, false)
    }

    init {
        commandList.add(startGameCommand())
        commandList.add(endGameCommand())
        commandList.add(openGameCommand())
        commandList.add(getCurrentRoundCommand())
        commandList.add(foundCommand())
        commandList.add(emeraldCommand())
        commandList.add(silverfishCommand())
        commandList.add(winnersCommand())
    }


    private fun winnersCommand(): Command {
        return Command(prefix, "winners", "", anyone) { _: TwitchUser, _: List<String> ->
            var winnerURL = database.getLastGameWinnersURL(channel)
            if (winnerURL.isBlank()) {
                val winners = database.getWinners(channel)
                val winnerString = pastebin.parseWinners(winners)
                winnerURL = pastebin.createPaste("Winners", winnerString)
                database.setLastGameWinnersURL(channel, winnerURL)
            }
            twirk.channelMessage("Winners: $winnerURL")
        }
    }

    private fun emeraldCommand(): Command {
        return Command(prefix, "emerald", "", anyone) { user: TwitchUser, _: List<String> ->
            doEmeraldOrSilverfish(user, VoteType.EMERALD)
        }
    }

    private fun silverfishCommand(): Command {
        return Command(prefix, "silverfish", "", anyone) { user: TwitchUser, _: List<String> ->
            doEmeraldOrSilverfish(user, VoteType.SILVERFISH)
        }
    }

    private fun doEmeraldOrSilverfish(user: TwitchUser, voteType: VoteType) {
        val message = when (database.playerVote(channel, voteType, user)) {
            VoteRecorded.RECORDED -> "${user.userName}: your vote for $voteType has been recorded"
            VoteRecorded.INELLIGIBLE -> "Sorry ${user.userName}: you're not able to vote this round, wait for the next game"
            VoteRecorded.ERROR -> "Sorry ${user.userName}: something went wrong"
            VoteRecorded.ALREADY_VOTED -> "Sorry ${user.userName}: you've already voted this round, I hope you win"
            VoteRecorded.NO_GAME_RUNNING -> "Sorry ${user.userName}, there's currently no game running"
        }
        //twirk.channelMessage(message)
    }

    private fun foundCommand(): Command {
        return Command(prefix, "found", "", modOnly) { _: TwitchUser, args: List<String> ->
            if (args.size > 1) {
                try {
                    val type = VoteType.valueOf(args[1].toUpperCase())
                    val newRound = database.found(channel, type)
                    if (newRound > 0) {
                        val gameId = database.getOpenGames(channel).first()
                        val users = database.getCorrectUsersOfGameRound(channel, gameId, newRound - 1)
                        if (users.size <= 1) {
                            database.endGame(channel, gameId)
                            twirk.channelMessage("Game Over Man: $gameId")
                        }
                        twirk.channelMessage("Result is $type, round $newRound started, the following user(s) got the answer right $users")
                    } else {
                        twirk.channelMessage("Something went wrong recording result or starting new round @RebelliousUno Help")
                    }
                } catch (iae: IllegalArgumentException) {
                    twirk.channelMessage("Need to specify emerald or silverfish")
                }
            } else {
                twirk.channelMessage("Need to specify emerald or silverfish")
            }
        }
    }

    private fun getCurrentRoundCommand(): Command {
        return Command(prefix, "currentround", "", modOnly) { _: TwitchUser, _: List<String> ->
            val gameId = database.getOpenGames(channel).first()
            val round = database.getRoundForGame(channel, gameId)
            twirk.channelMessage("Game $gameId is on round $round.first")
        }
    }

    private fun openGameCommand(): Command {
        val help = "Usage: ${prefix}opengames - Starts a game of Emerald or Silver fish"
        return Command(prefix, "opengames", help, modOnly) { _: TwitchUser, _: List<String> ->
            val id = database.getOpenGames(channel)
            twirk.channelMessage("Games with id $id are open")
        }
    }

    private fun startGameCommand(): Command {
        val help = "Usage: ${prefix}startgame - Starts a game of Emerald or Silver fish"
        return Command(prefix, "startgame", help, modOnly) { _: TwitchUser, _: List<String> ->
            val openGames = database.getOpenGames(channel)
            if (openGames.isEmpty()) {
                val id = database.startGame(channel)
                if (id > 0) {
                    database.startRound(channel, id, 1)
                    twirk.channelMessage("Game started with ID: $id, get ready for round 1")
                } else {
                    twirk.channelMessage("Problem starting game")
                }
            } else {
                twirk.channelMessage("Games $openGames already in progress, finish those before starting another")
            }


        }
    }

    private fun endGameCommand(): Command {
        val help = "Usage: ${prefix}endgame - Starts a game of Emerald or Silver fish"
        return Command(prefix, "endgame", help, modOnly) { _: TwitchUser, args: List<String> ->
            val id = if (args.size > 1) {
                val gameId = Integer.parseInt(args[1])
                database.endGame(channel, gameId)
            } else {
                database.endGame(channel)
            }
            if (id > 0)
                twirk.channelMessage("Ended Game ID: $id")
            else
                twirk.channelMessage("Could not find open game")
        }
    }
}

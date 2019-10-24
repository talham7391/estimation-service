package estimationserver.party

class Party (

    val numPlayers: Int

) {

    private val players = mutableMapOf<Player, Messenger?>()
    private val partyListeners = mutableListOf<PartyListener>()

    private var rememberDisconnectedPlayers = false

    fun connectPlayer (player: Player, messenger: Messenger) {
        var exception: Exception? = null

        if (player in players) {                    // allow player to reconnect
            if (players[player] == null) {
                players[player] = messenger
            } else {
                exception = DuplicatePlayer(player)
            }
        } else {                                    // new player
            if (players.size == numPlayers) {
                exception = PartyFull(numPlayers)
            } else {
                players[player] = messenger
            }
        }

        if (exception != null) {
            throw exception
        }

        partyListeners.forEach { it.playerConnected(player) }
    }

    fun disconnectPlayer (player: Player) {
        if (player !in players) {
            return
        }
        if (rememberDisconnectedPlayers) {
            players[player] = null
        } else {
            players.remove(player)
        }

        partyListeners.forEach { it.playerDisconnected(player) }
    }

    fun getPlayers () : Set<Player> {
        return players.keys
    }

    fun getConnectedPlayers () : Set<Player> {
        return players.filter { (_, v) -> v != null }.keys
    }

    fun getRememberDisconnectedPlayers() : Boolean {
        return rememberDisconnectedPlayers
    }

    fun setRememberDisconnectedPlayers(x: Boolean) {
        rememberDisconnectedPlayers = x

        if (!rememberDisconnectedPlayers) {
            players.keys.toSet().forEach {
                if (players[it] == null) {
                    players.remove(it)
                }
            }
        }
    }

    fun sendMessageToPlayer (player: Player, message: String) : Boolean {
        vetPlayer(player)
        return players[player]!!.send(message)
    }

    fun broadcastMessage (message: String) {
        players.values.forEach { it?.send(message) }
    }

    fun receiveMessageFromPlayer (player: Player, message: String) {
        vetPlayer(player)
        val listeners = partyListeners.toList()
        listeners.forEach { it.receivedMessageFromPlayer(player, message) }
    }

    fun addPartyListener (listener: PartyListener) {
        partyListeners.add(listener)
    }

    fun removePartyListener (listener: PartyListener) {
        partyListeners.remove(listener)
    }

    private fun vetPlayer (player: Player) {
        var exception: Exception? = null

        if (player !in players) {
            exception = PlayerNotInParty(player)
        } else if (players[player] == null) {
            exception = PlayerDisconnected(player)
        }

        if (exception != null) {
            throw exception
        }
    }

}

fun Party.isFull () = getPlayers().size == numPlayers

package estimationserver.party

import kotlinx.coroutines.sync.Mutex

class Party (

    val numPlayers: Int

) {

    private val mutex = Mutex()

    private val players = mutableMapOf<Player, Messenger?>()
    private val partyListeners = mutableListOf<PartyListener>()

    private var rememberDisconnectedPlayers = false

    suspend fun connectPlayer (player: Player, messenger: Messenger) {
        mutex.lock()

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
            mutex.unlock()
            throw exception
        }

        partyListeners.forEach { it.playerConnected(player) }

        mutex.unlock()
    }

    suspend fun disconnectPlayer (player: Player) {
        mutex.lock()

        if (rememberDisconnectedPlayers) {
            players[player] = null
        } else {
            players.remove(player)
        }

        partyListeners.forEach { it.playerDisconnected(player) }

        mutex.unlock()
    }

    suspend fun getPlayers () : Set<Player> {
        mutex.lock()
        val p = players.keys
        mutex.unlock()
        return p
    }

    suspend fun getRememberDisconnectedPlayers() : Boolean {
        mutex.lock()
        val x = rememberDisconnectedPlayers
        mutex.unlock()
        return x
    }

    suspend fun setRememberDisconnectedPlayers(x: Boolean) {
        mutex.lock()
        rememberDisconnectedPlayers = x

        if (!rememberDisconnectedPlayers) {
            players.keys.toSet().forEach {
                if (players[it] == null) {
                    players.remove(it)
                }
            }
        }

        mutex.unlock()
    }

    suspend fun sendMessageToPlayer (player: Player, message: String) : Boolean {
        vetPlayer(player)
        mutex.lock()
        val res = players[player]!!.send(message)
        mutex.unlock()
        return res
    }

    suspend fun broadcastMessage (message: String) {
        mutex.lock()
        players.values.forEach { it?.send(message) }
        mutex.unlock()
    }

    suspend fun receiveMessageFromPlayer (player: Player, message: String) {
        vetPlayer(player)
        mutex.lock()
        partyListeners.forEach { it.receivedMessageFromPlayer(player, message) }
        mutex.unlock()
    }

    suspend fun addPartyListener (listener: PartyListener) {
        mutex.lock()
        partyListeners.add(listener)
        mutex.unlock()
    }

    suspend fun removePartyListener (listener: PartyListener) {
        mutex.lock()
        partyListeners.remove(listener)
        mutex.unlock()
    }

    private suspend fun vetPlayer (player: Player) {
        mutex.lock()
        var exception: Exception? = null

        if (player !in players) {
            exception = PlayerNotInParty(player)
        } else if (players[player] == null) {
            exception = PlayerDisconnected(player)
        }

        mutex.unlock()
        if (exception != null) {
            throw exception
        }
    }

}
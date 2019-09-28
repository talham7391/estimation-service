package estimationserver.party

import kotlinx.coroutines.sync.Mutex
import java.lang.Exception

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

}
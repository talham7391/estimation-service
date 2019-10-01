package estimationserver

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import estimationserver.party.Party
import estimationserver.party.Player
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

class PreGameLobby (

    party: Party

) : Phase(party) {

    val mutex = Mutex()
    val playersReady = mutableSetOf<Player>()

    override fun playerConnected(player: Player) {
        super.playerConnected(player)
        GlobalScope.launch { sendPlayerCurrentState(player) }
    }

    override fun receivedMessageFromPlayer(player: Player, message: String) {
        val objectMapper = jacksonObjectMapper()
        try {

            val request = objectMapper.readValue<BasePreGameLobbyRequest>(message)

            when (request.type) {

                "PLAYER_READY" -> {
                    val r = objectMapper.readValue<PlayerReadyRequest>(message)
                    GlobalScope.launch { handlePlayerReadyRequest(player, r) }
                }

                "START_GAME" -> {
                    val r = objectMapper.readValue<StartGameRequest>(message)
                    GlobalScope.launch { handleStartGameRequest(player, r) }
                }

            }

        } catch (e: Exception) {}
    }

    private suspend fun handlePlayerReadyRequest (player: Player, request: PlayerReadyRequest) {
        if (request.ready) {
            mutex.lock()
            playersReady.add(player)
            mutex.unlock()
        }
    }

    private suspend fun handleStartGameRequest (player: Player, request: StartGameRequest) {
        mutex.lock()
        val allPlayersReady = playersReady.size == party.numPlayers
        mutex.unlock()
        if (allPlayersReady && request.start) {
            party.removePartyListener(this)

            // transition to the game

        } else {
            val objectMapper = jacksonObjectMapper()
            val error = ErrorResponse("Not all players are ready.")
            val res = objectMapper.writeValueAsString(error)
            party.sendMessageToPlayer(player, res)
        }
    }

    private suspend fun sendPlayerCurrentState (player: Player) {
        val preGameLobbyState = PreGameLobbyState()

        preGameLobbyState.readyStatus = getPlayersReadyData()

        val objectMapper = jacksonObjectMapper()
        party.broadcastMessage(objectMapper.writeValueAsString(preGameLobbyState))
    }

    private suspend fun getPlayersReadyData () : Set<PlayerReadyData> {
        val m = mutableMapOf<Player, Boolean>()
        party.getPlayers().forEach { m[it] = false }

        mutex.lock()
        playersReady.forEach { m[it] = true }
        mutex.unlock()

        val set = mutableSetOf<PlayerReadyData>()
        for ((k, v) in m) { set.add(PlayerReadyData(k, v)) }
        return set
    }

}
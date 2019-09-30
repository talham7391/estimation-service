package estimationserver

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import estimationserver.party.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

class Lobby (

    val party: Party

) : BasePartyListener() {

    private val mutex = Mutex()

    private var playersReady = false

    override fun playerConnected (player: Player) {
        GlobalScope.launch { broadcastConnectedPlayers() }
    }

    override fun playerDisconnected (player: Player) {
        GlobalScope.launch { broadcastConnectedPlayers() }
    }

    override fun receivedMessageFromPlayer(player: Player, message: String) {
        val objectMapper = jacksonObjectMapper()
        try {

            val request = objectMapper.readValue<PlayersReadyRequest>(message)
            GlobalScope.launch { handlePlayerReadyRequest(player, request) }

        } catch (e: Exception) {}
    }

    private suspend fun handlePlayerReadyRequest (player: Player, request: PlayersReadyRequest) {
        mutex.lock()

        if (request.ready) {
            if (party.isFull()) {
                playersReady = true
                party.removePartyListener(this)

                // transition to next phase

            } else {
                val objectMapper = jacksonObjectMapper()
                val error = ErrorResponse("The party isn't full yet.")
                val res = objectMapper.writeValueAsString(error)
                party.sendMessageToPlayer(player, res)
            }
        }

        mutex.unlock()
    }

    private suspend fun broadcastConnectedPlayers () {
        mutex.lock()

        if (!playersReady) {
            val objectMapper = jacksonObjectMapper()
            party.broadcastMessage(objectMapper.writeValueAsString(ConnectedPlayersResponse(party.getPlayers())))
        }

        mutex.unlock()
    }

}

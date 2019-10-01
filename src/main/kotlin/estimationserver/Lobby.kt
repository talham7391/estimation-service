package estimationserver

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import estimationserver.party.*

class Lobby (

    party: Party

) : Phase(party) {

    override fun receivedMessageFromPlayer(player: Player, message: String) {
        val objectMapper = jacksonObjectMapper()
        try {

            val request = objectMapper.readValue<PlayersReadyRequest>(message)
            handlePlayersReadyRequest(player, request)

        } catch (e: Exception) {}
    }

    private fun handlePlayersReadyRequest (player: Player, request: PlayersReadyRequest) {
        if (request.ready) {
            if (party.isFull()) {
                party.removePartyListener(this)

                party.setRememberDisconnectedPlayers(true)
                val preGameLobby = PreGameLobby(party)
                party.addPartyListener(preGameLobby)

            } else {
                val objectMapper = jacksonObjectMapper()
                val error = ErrorResponse("The party isn't full yet.")
                val res = objectMapper.writeValueAsString(error)
                party.sendMessageToPlayer(player, res)
            }
        }
    }

}

package estimationserver

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import estimationserver.party.*
import talham7391.estimation.Estimation
import talham7391.estimation.GameDriver
import talham7391.estimation.playAnyCardInHand

class Lobby (

    party: Party

) : Phase(party) {

    init {
        party.addPartyListener(this)
    }

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

                val players = party.getPlayers().map { EstimationPlayer(it) }
                val estimation = Estimation(
                    players[0],
                    players[1],
                    players[2],
                    players[3]
//                    talham7391.estimation.Player(),
//                    talham7391.estimation.Player(),
//                    talham7391.estimation.Player()
                )
//                val driver = GameDriver(estimation)
//                driver.doInitialBidding()
//                driver.doDeclaringTrump()
//                driver.doFinalBidding()
//                repeat(12) { driver.doTrick() }

                PreGameLobby(estimation, party)

            } else {
                val objectMapper = jacksonObjectMapper()
                val error = ErrorResponse("The party isn't full yet.")
                val res = objectMapper.writeValueAsString(error)
                party.sendMessageToPlayer(player, res)
            }
        }
    }

}

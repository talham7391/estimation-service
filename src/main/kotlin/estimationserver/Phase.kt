package estimationserver

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import estimationserver.party.BasePartyListener
import estimationserver.party.Party
import estimationserver.party.Player

open class Phase (

    val party: Party

) : BasePartyListener() {

    override fun playerConnected (player: Player) {
        broadcastConnectedPlayers()
    }

    override fun playerDisconnected (player: Player) {
        broadcastConnectedPlayers()
    }

    private fun broadcastConnectedPlayers () {
        val objectMapper = jacksonObjectMapper()
        party.broadcastMessage(objectMapper.writeValueAsString(ConnectedPlayersResponse(party.getPlayers())))
    }

}

package estimationserver

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import estimationserver.party.Party
import estimationserver.party.Player
import talham7391.estimation.Estimation

class Game (

    val estimation: Estimation,

    party: Party

) : Phase(party) {

    init {
        party.addPartyListener(this)
        party.getPlayers().forEach { sendPlayerCurrentState(it) }
    }

    override fun playerConnected (player: Player) {
        super.playerConnected(player)
        sendPlayerCurrentState(player)
    }

    private fun sendPlayerCurrentState (player: Player) {
        sendGameState(player) {
            applyIsGameDone()
            applyMyCards(player)
        }
    }

    private fun sendGameState (player: Player, config: GameStateResponse.() -> Unit) {
        val gameState = GameStateResponse()
        gameState.apply(config)

        val objectMapper = jacksonObjectMapper()
        party.sendMessageToPlayer(player, objectMapper.writeValueAsString(gameState))
    }

    private fun GameStateResponse.applyIsGameDone () {
        done = if (estimation.getPastTricks().isEmpty()) {
            false
        } else {
            estimation.getPastTricks().size % 13 == 0
        }
    }

    private fun GameStateResponse.applyMyCards (player: Player) {
        val p = estimation.playerGroup.players.find { (it as? EstimationPlayer)?.data == player }
        if (p != null) {
            myCards = p.getCardsInHand().map { SerializedCard(it.suit.name, it.rank.name) }.toSet()
        }
    }
}
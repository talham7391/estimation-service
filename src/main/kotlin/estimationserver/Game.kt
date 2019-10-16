package estimationserver

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import estimationserver.party.Party
import estimationserver.party.Player
import talham7391.estimation.Estimation

class Game (

    private val estimation: Estimation,

    party: Party

) : Phase(party) {

    private val gamePhaseTracker = GamePhaseTracker()

    init {
        estimation.addTurnListener(gamePhaseTracker)
        party.addPartyListener(this)
        party.getPlayers().forEach { sendPlayerCurrentState(it) }
    }

    override fun playerConnected (player: Player) {
        super.playerConnected(player)
        sendPlayerCurrentState(player)
    }

    override fun receivedMessageFromPlayer (player: Player, message: String) {
        val objectMapper = jacksonObjectMapper()
        try {

            val request = objectMapper.readValue<BaseGameRequest>(message)

            when (request.type) {

                "INITIAL_BID" -> {
                    val r = objectMapper.readValue<InitialBidRequest>(message)
                    handleInitialBidRequest(player, r)
                }

                "PASS" -> {
                    val r = objectMapper.readValue<PassRequest>(message)
                    handlePassRequest(player, r)
                }

            }

        } catch (e: Exception) {}
    }

    private fun handleInitialBidRequest (player: Player, request: InitialBidRequest) {
        try {
            val p = estimation.getPlayerWithTurn() as? EstimationPlayer
            if (p?.data == player) {
                p.bid(request.bid)
            }
        } catch (e: Exception) {}
        sendPlayersIncrementalUpdate()
    }

    private fun handlePassRequest (player: Player, request: PassRequest) {
        try {
            val p = estimation.getPlayerWithTurn() as? EstimationPlayer
            if (p?.data == player) {
                p.pass()
            }
        } catch (e: Exception) {}
        sendPlayersIncrementalUpdate()
    }

    private fun sendPlayerCurrentState (player: Player) {
        sendGameState(player) {
            applyIsGameDone()
            applyMyCards(player)
            applyTurnOrder()
            applyTurnOf()
            applyPhase()
            applyInitialBids()
        }
    }

    private fun sendPlayersIncrementalUpdate () {
        party.getPlayers().forEach {
            sendGameState(it) {
                applyTurnOf()
                applyPhase()

                if (gamePhaseTracker.getPhase() == "INITIAL_BIDDING") {
                    applyInitialBids()
                }
            }
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

    private fun GameStateResponse.applyTurnOf () {
        val p = estimation.getPlayerWithTurn() as? EstimationPlayer
        turnOf = p?.data
    }

    private fun GameStateResponse.applyTurnOrder () {
        val players = Array<EstimationPlayer?>(4) { null }

        estimation.playerGroup.players.map {
            val p = it as? EstimationPlayer
            if (p != null) {
                players[it.getTurnIndex()] = p
            }
        }

        turnOrder = players.filterNotNull().map { it.data }
    }

    private fun GameStateResponse.applyPhase () {
        phase = gamePhaseTracker.getPhase()
    }

    private fun GameStateResponse.applyInitialBids () {
        val latestBids = mutableMapOf<Player, String>()
        for (b in estimation.initialBiddingHistory()) {
            val p = b.player as? EstimationPlayer
            if (p != null) {
                latestBids[p.data] = if (b.passed) "pass" else "${b.bid}"
            }
        }
        initialBids = latestBids.map { (k, v) -> PlayerBidData(k, v) }.toSet()
    }

    private fun cleanup () {
        estimation.removeTurnListener(gamePhaseTracker)
    }
}
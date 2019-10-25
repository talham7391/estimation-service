package estimationserver

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import estimationserver.party.Party
import estimationserver.party.Player
import estimationserver.party.PlayerDisconnected
import talham7391.estimation.*
import talham7391.estimation.gamedata.getWinner

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

        val request = objectMapper.readValue<BaseGameRequest>(message)

        when (request.type) {

            "BID" -> {
                val r = objectMapper.readValue<BidRequest>(message)
                handleBidRequest(player, r)
            }

            "PASS" -> {
                val r = objectMapper.readValue<PassRequest>(message)
                handlePassRequest(player, r)
            }

            "DECLARE_TRUMP" -> {
                val r = objectMapper.readValue<DeclareTrumpRequest>(message)
                handleDeclareTrumpRequest(player, r)
            }

            "PLAY_CARD" -> {
                val r = objectMapper.readValue<PlayCardRequest>(message)
                handlePlayCardRequest(player, r)
            }

        }
    }

    private fun handleBidRequest (player: Player, request: BidRequest) {
        try {
            val p = estimation.getPlayerWithTurn() as? EstimationPlayer
            if (p?.data == player) {
                p.bid(request.bid)

                val objectMapper = jacksonObjectMapper()
                party.broadcastMessage(objectMapper.writeValueAsString(BidEvent(request.bid.toString(), player)))
            }
        } catch (e: Exception) {}
        sendPlayersIncrementalUpdate()
    }

    private fun handlePassRequest (player: Player, request: PassRequest) {
        try {
            val p = estimation.getPlayerWithTurn() as? EstimationPlayer
            if (p?.data == player) {
                p.pass()

                val objectMapper = jacksonObjectMapper()
                party.broadcastMessage(objectMapper.writeValueAsString(BidEvent("pass", player)))
            }
        } catch (e: Exception) {}
        sendPlayersIncrementalUpdate()
    }

    private fun handleDeclareTrumpRequest (player: Player, request: DeclareTrumpRequest) {
        try {
            val p = estimation.getPlayerWithTurn() as? EstimationPlayer
            if (p?.data == player) {
                p.declareTrump(Suit.valueOf(request.suit))
            }
        } catch (e: Exception) {}
        sendPlayersIncrementalUpdate()
    }

    private fun handlePlayCardRequest (player: Player, request: PlayCardRequest) {
        try {
            val p = estimation.getPlayerWithTurn() as? EstimationPlayer
            if (p?.data == player) {
                p.playCard(Card(Suit.valueOf(request.suit), Rank.valueOf(request.rank)))
                if (isGameDone()) {
                    sendFinalUpdate()
                    gotoPreGameLobby()
                } else {
                    sendPlayersIncrementalUpdate()
                }
            }
        } catch (e: Exception) {
            println(e)
        }
    }

    private fun sendPlayerCurrentState (player: Player) {
        sendGameState(player) {
            applyMyCards(player)
            applyTurnOrder()
            applyTurnOf()
            applyPhase()
            applyInitialBids()
            applyTrumpSuit()
            applyFinalBids()
            applyPreviousTrick()
            applyCurrentTrick()
            applyPlayerTricks()
        }
    }

    private fun sendPlayersIncrementalUpdate () {
        party.getPlayers().forEach {
            sendGameState(it) {
                applyTurnOf()
                applyPhase()

                when (gamePhaseTracker.getPhase()) {
                    "INITIAL_BIDDING", "DECLARING_TRUMP" -> {
                        applyInitialBids()
                    }

                    "FINAL_BIDDING" -> {
                        applyTrumpSuit()
                        applyFinalBids()
                    }

                    else -> {
                        applyFinalBids()
                        applyMyCards(it)
                        applyPreviousTrick()
                        applyCurrentTrick()
                        applyPlayerTricks()
                    }
                }
            }
        }
    }

    private fun sendFinalUpdate () {
        party.getPlayers().forEach {
            sendGameState(it) {
                myCards = emptySet()

                val lastTrick = estimation.getPastTricks().last()
                currentTrick = lastTrick.plays.mapNotNull { play ->
                    val p = play.player as? EstimationPlayer
                    if (p != null) {
                        PlayData(
                            p.data,
                            SerializedCard(play.card.suit.name, play.card.rank.name)
                        )
                    } else {
                        null
                    }
                }.toSet()

                val numTricksInLatestGame = 13

                val tricks = estimation.getPastTricks().takeLast(numTricksInLatestGame)
                val numWon = mutableMapOf<Player, Int>()
                party.getPlayers().forEach { numWon[it] = 0 }
                tricks.forEach {
                    val p = it.getWinner() as? EstimationPlayer
                    if (p != null) {
                        numWon[p.data] = (numWon[p.data] ?: 0) + 1
                    }
                }
                playerTricks = numWon.map { (k, v) -> PlayerTrickData(k, v) }.toSet()
            }
        }
    }

    private fun sendGameState (player: Player, config: GameStateResponse.() -> Unit) {
        val gameState = GameStateResponse()
        gameState.apply(config)

        val objectMapper = jacksonObjectMapper()
        try {
            party.sendMessageToPlayer(player, objectMapper.writeValueAsString(gameState))
        } catch (e: PlayerDisconnected) {}
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

    private fun GameStateResponse.applyTrumpSuit () {
        try {
            trumpSuit = estimation.getTrumpSuit().name
        } catch (e: Exception) {}
    }

    private fun GameStateResponse.applyFinalBids () {
        finalBids = estimation.getPlayerBids().mapNotNull { bid ->
            val p = bid.player as? EstimationPlayer
            if (p != null) {
                PlayerBidData(p.data, "${bid.bid}")
            } else {
                null
            }
        }.toSet()
    }

    private fun GameStateResponse.applyPreviousTrick () {
        val numTricksInGame = estimation.getPastTricks().size % 13
        if (numTricksInGame == 0) {
            previousTrick = emptySet()
        } else {
            previousTrick = estimation.getPastTricks().last().plays.mapNotNull {
                val p = it.player as? EstimationPlayer
                if (p != null) {
                    PlayData(
                        p.data,
                        SerializedCard(it.card.suit.name, it.card.rank.name)
                    )
                } else {
                    null
                }
            }.toSet()
        }
    }

    private fun GameStateResponse.applyCurrentTrick () {
        currentTrick = estimation.getCurrentTrick()?.mapNotNull {
            val p = it.player as? EstimationPlayer
            if (p != null) {
                PlayData(
                    p.data,
                    SerializedCard(it.card.suit.name, it.card.rank.name)
                )
            } else {
                null
            }
        }?.toSet()
    }

    private fun GameStateResponse.applyPlayerTricks () {
        val numTricksInLatestGame = estimation.getPastTricks().size % 13

        val tricks = estimation.getPastTricks().takeLast(numTricksInLatestGame)
        val numWon = mutableMapOf<Player, Int>()
        party.getPlayers().forEach { numWon[it] = 0 }
        tricks.forEach {
            val p = it.getWinner() as? EstimationPlayer
            if (p != null) {
                numWon[p.data] = (numWon[p.data] ?: 0) + 1
            }
        }
        playerTricks = numWon.map { (k, v) -> PlayerTrickData(k, v) }.toSet()
    }

    private fun gotoPreGameLobby () {
        estimation.removeTurnListener(gamePhaseTracker)
        party.removePartyListener(this)
        PreGameLobby(estimation, party)
    }

    private fun isGameDone () = estimation.getPastTricks().size % 13 == 0 && estimation.getCurrentTrick() == null
}
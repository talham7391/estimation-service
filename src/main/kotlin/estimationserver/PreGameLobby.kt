package estimationserver

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import estimationserver.party.Party
import estimationserver.party.Player
import talham7391.estimation.Estimation

class PreGameLobby (

    val estimation: Estimation,

    party: Party

) : Phase(party) {

    private val playersReady = mutableSetOf<Player>()

    init {
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

            val request = objectMapper.readValue<BasePreGameLobbyRequest>(message)

            when (request.type) {

                "PLAYER_READY" -> {
                    val r = objectMapper.readValue<PlayerReadyRequest>(message)
                    handlePlayerReadyRequest(player, r)
                }

                "START_GAME" -> {
                    val r = objectMapper.readValue<StartGameRequest>(message)
                    handleStartGameRequest(player, r)
                }

            }

        } catch (e: Exception) {}
    }

    private fun handlePlayerReadyRequest (player: Player, request: PlayerReadyRequest) {
        if (request.ready) {
            playersReady.add(player)

            broadcastPreGameLobbyState {
                applyPlayersReadyData()
            }
        }
    }

    private fun handleStartGameRequest (player: Player, request: StartGameRequest) {
        val allPlayersReady = playersReady.size == party.numPlayers
        if (allPlayersReady && request.start) {
            party.removePartyListener(this)
            Game(estimation, party)
        } else {
            val objectMapper = jacksonObjectMapper()
            val error = ErrorResponse("Not all players are ready.")
            val res = objectMapper.writeValueAsString(error)
            party.sendMessageToPlayer(player, res)
        }
    }

    private fun sendPlayerCurrentState (player: Player) {
        sendPreGameLobbyState(player) {
            applyPlayersReadyData()
            applyPlayerScoresData()
        }
    }

    private fun sendPreGameLobbyState (player: Player, config: PreGameLobbyState.() -> Unit) {
        val preGameLobbyState = PreGameLobbyState()
        preGameLobbyState.apply(config)

        val objectMapper = jacksonObjectMapper()
        party.sendMessageToPlayer(player, objectMapper.writeValueAsString(preGameLobbyState))
    }

    private fun broadcastPreGameLobbyState (config: PreGameLobbyState.() -> Unit) {
        val preGameLobbyState = PreGameLobbyState()
        preGameLobbyState.apply { config() }

        val objectMapper = jacksonObjectMapper()
        party.broadcastMessage(objectMapper.writeValueAsString(preGameLobbyState))
    }

    private fun PreGameLobbyState.applyPlayersReadyData () {
        val m = mutableMapOf<Player, Boolean>()
        party.getPlayers().forEach { m[it] = false }

        playersReady.forEach { m[it] = true }

        val set = mutableSetOf<PlayerReadyData>()
        for ((k, v) in m) { set.add(PlayerReadyData(k, v)) }

        readyStatus = set
    }

    private fun PreGameLobbyState.applyPlayerScoresData () {
        val s = mutableSetOf<PlayerScoreData>()
        estimation.playerGroup.players.forEach {
            val es = it as? EstimationPlayer
            if (es != null) {
                s.add(PlayerScoreData(es.data, es.getScore()))
            }
        }

        playerScores = s
    }

}

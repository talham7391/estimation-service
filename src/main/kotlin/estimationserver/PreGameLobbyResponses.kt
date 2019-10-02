package estimationserver

import estimationserver.party.Player

data class PreGameLobbyState (

    var readyStatus: Set<PlayerReadyData>? = null,

    var playerScores: Set<PlayerScoreData>? = null

)

data class PlayerReadyData (

    val player: Player,
    val ready: Boolean

)

data class PlayerScoreData (

    val player: Player,
    val score: Int

)
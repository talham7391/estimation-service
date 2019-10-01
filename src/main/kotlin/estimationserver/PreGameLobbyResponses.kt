package estimationserver

import estimationserver.party.Player

data class PreGameLobbyState (

    var readyStatus: Set<PlayerReadyData>? = null

)

data class PlayerReadyData (

    val player: Player,
    val ready: Boolean

)
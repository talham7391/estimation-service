package estimationserver

import estimationserver.party.Player

data class GameStateResponse (

    var done: Boolean? = null,

    var myCards: Set<SerializedCard>? = null,

    var playerCards: Set<PlayerCardsData>? = null,      // TODO

    var initialBids: Set<PlayerBidData>? = null,        // TODO | next

    var turnOf: Player? = null,

    var phase: String? = null,                          // TODO | next

    var finalBids: Set<PlayerBidData>? = null,          // TODO

    var trumpSuit: String? = null,                      // TODO

    var playerTricks: Set<PlayerTrickData>? = null,     // TODO

    var turnOrder: List<Player>? = null,

    var currentTrick: Set<PlayData>? = null,            // TODO

    val type: String = "GAME_STATE"

)

data class SerializedCard (
    val suit: String,
    val rank: String
)

data class PlayerCardsData (
    val player: Player,
    val numCards: Int
)

data class PlayerBidData (
    val player: Player,
    val bid: Int
)

data class PlayerTrickData (
    val player: Player,
    val numWon: Int
)

data class PlayData (
    val player: Player,
    val card: SerializedCard
)
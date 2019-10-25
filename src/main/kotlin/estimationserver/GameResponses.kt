package estimationserver

import estimationserver.party.Player

data class GameStateResponse (

    var myCards: Set<SerializedCard>? = null,

    var playerCards: Set<PlayerCardsData>? = null,      // TODO

    var initialBids: Set<PlayerBidData>? = null,

    var turnOf: Player? = null,

    var phase: String? = null,

    var finalBids: Set<PlayerBidData>? = null,

    var trumpSuit: String? = null,

    var playerTricks: Set<PlayerTrickData>? = null,

    var turnOrder: List<Player>? = null,

    var previousTrick: Set<PlayData>? = null,

    var currentTrick: Set<PlayData>? = null,

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
    val bid: String
)

data class PlayerTrickData (
    val player: Player,
    val numWon: Int
)

data class PlayData (
    val player: Player,
    val card: SerializedCard
)

data class BidEvent (
    val bid: String,
    val player: Player,
    val type: String = "EVENT",
    val eventType: String = "BID"
)
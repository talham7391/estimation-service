package estimationserver.party

interface PartyListener {

    fun playerConnected (player: Player)

    fun playerDisconnected (player: Player)

    fun receivedMessageFromPlayer (player: Player, message: String)

}

abstract class BasePartyListener : PartyListener {

    override fun playerConnected(player: Player) {}

    override fun playerDisconnected(player: Player) {}

    override fun receivedMessageFromPlayer(player: Player, message: String) {}

}
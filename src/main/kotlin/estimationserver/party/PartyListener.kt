package estimationserver.party

interface PartyListener {

    fun playerConnected (player: Player)
    fun playerDisconnected (player: Player)

}
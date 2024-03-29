package estimationserver

import estimationserver.party.Player

data class ConnectedPlayersResponse (val players: Set<Player>, val type: String = "CONNECTED_PLAYERS")

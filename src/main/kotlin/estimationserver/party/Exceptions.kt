package estimationserver.party

class PartyFull (numPlayers: Int) : Exception("This party doesn't accept more than $numPlayers players.")

class DuplicatePlayer (player: Player) : Exception("The player $player already exists in the party.")

class PlayerNotInParty (player: Player) : Exception("Player $player is not in the party.")

class PlayerDisconnected (player: Player) : Exception("Player $player is disconnected.")

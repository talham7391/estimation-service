package estimationserver.party

class PartyFull (numPlayers: Int) : Exception("This party doesn't accept more than $numPlayers players.")

class DuplicatePlayer (player: Player) : Exception("The player $player already exists in the party.")

package estimationserver

import talham7391.estimation.Player
import talham7391.estimation.TurnListener
import talham7391.estimation.gamedata.Play

class GamePhaseTracker : TurnListener {

    private var phase = "TRICK_TAKING"

    override fun onPlayersTurnToDeclareTrump(player: Player) {
        phase = "DECLARING_TRUMP"
    }

    override fun onPlayersTurnToFinalBid(player: Player) {
        phase = "FINAL_BIDDING"
    }

    override fun onPlayersTurnToInitialBid(player: Player) {
        phase = "INITIAL_BIDDING"
    }

    override fun onPlayersTurnToPlayCard(player: Player, trickSoFar: List<Play>) {
        phase = "TRICK_TAKING"
    }

    fun getPhase () = phase
}
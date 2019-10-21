package estimationserver

import estimationserver.party.BasePartyListener
import estimationserver.party.Party
import estimationserver.party.Player
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

class PartyDeletionJob (

    private val partyId: String,
    private val party: Party,
    private val mutex: Mutex,
    private val deletionDelay: Long

) : BasePartyListener() {

    private val listenersMutex = Mutex()
    private val listeners = mutableSetOf<PartyDeletionJobListener>()

    private var timer : Job = startTimer()

    override fun playerConnected(player: Player) {
        cancelCurrentTimer()
    }

    override fun playerDisconnected(player: Player) {
        cancelCurrentTimer()
        timer = startTimer()
    }

    suspend fun addListener (l: PartyDeletionJobListener) {
        listenersMutex.lock()
        listeners.add(l)
        listenersMutex.unlock()
    }

    suspend fun removeListener (l: PartyDeletionJobListener) {
        listenersMutex.lock()
        listeners.remove(l)
        listenersMutex.unlock()
    }

    private suspend fun notifyListeners () {
        listenersMutex.lock()
        listeners.forEach { it.shouldDeleteParty(partyId) }
        listenersMutex.unlock()
    }

    private fun startTimer () = GlobalScope.launch {
        mutex.lock()
        var noPlayers = party.getConnectedPlayers().isEmpty()
        mutex.unlock()

        if (!noPlayers) {
            return@launch
        }

        delay(deletionDelay)

        mutex.lock()
        noPlayers = party.getConnectedPlayers().isEmpty()
        mutex.unlock()

        if (noPlayers) {
            notifyListeners()
        }
    }

    private fun cancelCurrentTimer () {
        if (timer.isActive) {
            timer.cancel()
        }
    }
}

interface PartyDeletionJobListener {

    fun shouldDeleteParty(partyId: String)

}

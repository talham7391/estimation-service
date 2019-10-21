package estimationserver

import estimationserver.party.Party
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import org.slf4j.LoggerFactory
import kotlin.random.Random

object PartyManager {
    private val actor = GlobalScope.partyManagerActor()
    private val logger = LoggerFactory.getLogger(PartyManager.javaClass)

    suspend fun createParty() : String {
        val res = CompletableDeferred<String>()
        actor.send(CreateParty(res))
        return res.await()
    }

    suspend fun getPartyIds () : Set<String> {
        val res = CompletableDeferred<Set<String>>()
        actor.send(GetPartyIds(res))
        return res.await()
    }

    suspend fun getParty (partyId: String) : PartyWrapper {
        val res = CompletableDeferred<PartyWrapper>()
        actor.send(GetParty(partyId, res))
        return res.await()
    }

    suspend fun deleteParty (partyId: String) {
        actor.send(DeleteParty(partyId))
    }

    fun CoroutineScope.partyManagerActor () : Channel<PartyManagerMessage> {
        val channel = Channel<PartyManagerMessage>()
        launch {

            val parties = mutableMapOf<String, PartyWrapper>()

            for (message in channel) {
                when (message) {

                    is CreateParty -> {
                        var partyId: String? = null
                        for (i in 0 until 10) {
                            val potentialId = Random.nextInt(9999).toString().padStart(4, '0')
                            if (potentialId !in parties) {
                                partyId = potentialId
                                break
                            }
                        }
                        if (partyId == null) {
                            message.result.completeExceptionally(UnableToGeneratePartyId())
                            logger.warn("Was not able to generate Party Id.")
                        } else {
                            val party = Party(4)
                            val mutex = Mutex()
                            val partyWrapper = PartyWrapper(party, mutex)

                            val deletionJob = PartyDeletionJob(
                                partyId,
                                party,
                                mutex,
                                10 * 1000
                            )
                            deletionJob.addListener(PartyDeletor())

                            party.addPartyListener(deletionJob)

                            parties[partyId] = partyWrapper
                            message.result.complete(partyId)
                            logger.info("PartyId[$partyId] created.")
                        }
                    }

                    is DeleteParty -> {
                        val partyId = message.id
                        parties.remove(partyId)
                        logger.info("PartyId[$partyId] deleted.")
                    }

                    is GetPartyIds -> {
                        message.result.complete(parties.keys)
                    }

                    is GetParty -> {
                        if (message.id in parties) {
                            message.result.complete(parties[message.id]!!)
                        } else {
                            message.result.completeExceptionally(PartyDoesNotExist(message.id))
                        }
                    }

                }
            }
        }
        return channel
    }
}


data class PartyWrapper (

    val party: Party,
    val mutex: Mutex = Mutex()

)


class PartyDeletor : PartyDeletionJobListener {

    override fun shouldDeleteParty(partyId: String) {
        GlobalScope.launch { PartyManager.deleteParty(partyId) }
    }

}


sealed class PartyManagerMessage

data class CreateParty (val result: CompletableDeferred<String>) : PartyManagerMessage()

data class GetPartyIds (val result: CompletableDeferred<Set<String>>) : PartyManagerMessage()

data class GetParty (val id: String, val result: CompletableDeferred<PartyWrapper>) : PartyManagerMessage()

data class DeleteParty (val id: String) : PartyManagerMessage()


class UnableToGeneratePartyId : Exception("Unable to generate party id.")

class PartyDoesNotExist (id: String) : Exception("Party with id $id does not exist.")

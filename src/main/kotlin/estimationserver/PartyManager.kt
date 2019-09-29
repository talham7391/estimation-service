package estimationserver

import estimationserver.party.Party
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.random.Random

object PartyManager {
    private val actor = GlobalScope.partyManagerActor()

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

    suspend fun getParty (partyId: String) : Party {
        val res = CompletableDeferred<Party>()
        actor.send(GetParty(partyId, res))
        return res.await()
    }

    fun CoroutineScope.partyManagerActor () : Channel<PartyManagerMessage> {
        val channel = Channel<PartyManagerMessage>()
        launch {

            val parties = mutableMapOf<String, Party>()

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
                        } else {
                            parties[partyId] = Party(4)
                            message.result.complete(partyId)
                        }
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

sealed class PartyManagerMessage

data class CreateParty (val result: CompletableDeferred<String>) : PartyManagerMessage()

data class GetPartyIds (val result: CompletableDeferred<Set<String>>) : PartyManagerMessage()

data class GetParty (val id: String, val result: CompletableDeferred<Party>) : PartyManagerMessage()


class UnableToGeneratePartyId : Exception("Unable to generate party id.")

class PartyDoesNotExist (id: String) : Exception("Party with id $id does not exist.")
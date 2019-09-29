package estimationserver

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class TestPartyManager {

    @Test fun testPartyManager () = runBlocking<Unit> {
        assertEquals(PartyManager.getPartyIds(), emptySet())

        val parties = mutableSetOf<String>()

        repeat(3) {
            val partyId = PartyManager.createParty()
            parties.add(partyId)
        }

        parties.forEach {
            PartyManager.getParty(it)
        }

        assertEquals(PartyManager.getPartyIds(), parties)
    }

}
package estimationserver

import estimationserver.party.MockMessenger
import estimationserver.party.Party
import estimationserver.party.Player
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlin.test.Test
import kotlin.test.assertEquals

class TestPartyDeletionJob {

    @Test fun testPartyDeletedWithoutConnectedPlayers () = runBlocking<Unit> {
        val party = Party(4)
        val partyDeletionJob = PartyDeletionJob("0", party, Mutex(), 250)
        party.addPartyListener(partyDeletionJob)

        val helper = TestPartyDeletionHelper()
        partyDeletionJob.addListener(helper)

        assertEquals(null, helper.shouldDelete)

        delay(500)

        assertEquals("0", helper.shouldDelete)
    }

    @Test fun testPartyDeletedAfterPlayerDisconnects () = runBlocking<Unit> {
        val party = Party(4)
        val p1 = Player("0")
        party.connectPlayer(p1, MockMessenger())

        val partyDeletionJob = PartyDeletionJob("0", party, Mutex(), 250)
        party.addPartyListener(partyDeletionJob)

        val helper = TestPartyDeletionHelper()
        partyDeletionJob.addListener(helper)

        assertEquals(null, helper.shouldDelete)

        delay(500)

        assertEquals(null, helper.shouldDelete)

        party.disconnectPlayer(p1)

        assertEquals(null, helper.shouldDelete)

        delay(500)

        assertEquals("0", helper.shouldDelete)
    }

    @Test fun testPartyNotDeletedWhenPlayerConnected () = runBlocking<Unit> {
        val party = Party(4)
        val p1 = Player("0")
        party.connectPlayer(p1, MockMessenger())

        val partyDeletionJob = PartyDeletionJob("0", party, Mutex(), 250)
        party.addPartyListener(partyDeletionJob)

        val helper = TestPartyDeletionHelper()
        partyDeletionJob.addListener(helper)

        assertEquals(null, helper.shouldDelete)

        delay(500)

        assertEquals(null, helper.shouldDelete)

        delay(500)

        assertEquals(null, helper.shouldDelete)
    }

    @Test fun testPartyNotDeletedIfPlayerReconnectsInTime () = runBlocking<Unit> {
        val party = Party(4)
        val p1 = Player("0")
        party.connectPlayer(p1, MockMessenger())

        val partyDeletionJob = PartyDeletionJob("0", party, Mutex(), 250)
        party.addPartyListener(partyDeletionJob)

        val helper = TestPartyDeletionHelper()
        partyDeletionJob.addListener(helper)

        party.disconnectPlayer(p1)

        delay(100)

        assertEquals(null, helper.shouldDelete)

        party.connectPlayer(p1, MockMessenger())

        assertEquals(null, helper.shouldDelete)

        delay(500)

        assertEquals(null, helper.shouldDelete)
    }
}

class TestPartyDeletionHelper : PartyDeletionJobListener {

    var shouldDelete: String? = null

    override fun shouldDeleteParty(partyId: String) {
        shouldDelete = partyId
    }
}
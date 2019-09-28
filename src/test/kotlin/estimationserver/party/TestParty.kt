package estimationserver.party

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TestParty {

    @Test fun testPlayersCanConnectAndDisconnect () = runBlocking<Unit> {
        val player = Player("bob")
        val party = Party(4)

        assert(party.getPlayers().isEmpty())

        party.connectPlayer(player, MockMessenger())
        assertEquals(party.getPlayers(), setOf(player))

        party.disconnectPlayer(player)
        assert(party.getPlayers().isEmpty())
    }

    @Test fun testPartyDoesNotAcceptMoreThanAllowedPlayers () = runBlocking<Unit> {
        // testing with parties of size 2 and 7
        for (numPlayers in listOf(2, 7)) {
            val party = Party(numPlayers)
            repeat(numPlayers) {
                party.connectPlayer(Player("$it"), MockMessenger())
            }
            assertFailsWith(PartyFull::class) {
                repeat(3) {
                    party.connectPlayer(Player("${numPlayers + it}"), MockMessenger())
                }
            }
        }
    }

    @Test fun testPartyRemembersDisconnectedPlayers () = runBlocking<Unit> {
        val party = Party(3)
        party.setRememberDisconnectedPlayers(true)

        val p1 = Player("Bob")
        val p2 = Player("John")
        val p3 = Player("Joe")

        party.connectPlayer(p1, MockMessenger())
        party.connectPlayer(p2, MockMessenger())
        party.connectPlayer(p3, MockMessenger())

        party.disconnectPlayer(p1)
        party.disconnectPlayer(p2)
        party.disconnectPlayer(p3)

        assertEquals(party.getPlayers(), setOf(p1, p2, p3))

        party.connectPlayer(p1, MockMessenger())
        party.connectPlayer(p2, MockMessenger())
        party.connectPlayer(p3, MockMessenger())

        assertEquals(party.getPlayers(), setOf(p1, p2, p3))
    }

    @Test fun testPartyForgetsDisconnectedPlayers () = runBlocking<Unit> {
        val party = Party(4)
        party.setRememberDisconnectedPlayers(true)

        val p1 = Player("Bob")
        val p2 = Player("John")
        val p3 = Player("Joe")
        val p4 = Player("Jerry")

        party.connectPlayer(p1, MockMessenger())
        party.connectPlayer(p2, MockMessenger())
        party.connectPlayer(p3, MockMessenger())
        party.connectPlayer(p4, MockMessenger())

        party.disconnectPlayer(p2)
        party.disconnectPlayer(p3)

        assertEquals(party.getPlayers(), setOf(p1, p2, p3, p4))

        party.setRememberDisconnectedPlayers(false)

        assertEquals(party.getPlayers(), setOf(p1, p4))

        party.connectPlayer(p2, MockMessenger())

        assertEquals(party.getPlayers(), setOf(p1, p2, p4))
    }

    @Test fun testSameConnectionIdCannotBeUsedTwice () = runBlocking<Unit> {
        val party = Party(4)

        val p1 = Player("Bob")
        val p2 = Player("John")

        party.connectPlayer(p1, MockMessenger())
        party.connectPlayer(p2, MockMessenger())

        assertFailsWith(DuplicatePlayer::class) {
            party.connectPlayer(p1, MockMessenger())
            party.connectPlayer(p2, MockMessenger())
        }
    }
}

class MockMessenger : Messenger {

    override fun send(message: String) : Boolean {
        return true
    }

}
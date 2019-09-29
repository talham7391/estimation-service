package estimationserver.party

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

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
            val playersInParty = mutableSetOf<Player>()
            repeat(numPlayers) {
                val p = Player("$it")
                party.connectPlayer(p, MockMessenger())
                playersInParty.add(p)
            }
            assertFailsWith(PartyFull::class) {
                repeat(3) {
                    party.connectPlayer(Player("${numPlayers + it}"), MockMessenger())
                }
            }
            assertEquals(party.getPlayers(), playersInParty)
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

    @Test fun testCanSendMessagesToPlayer () = runBlocking<Unit> {
        val p1 = Player("bob")
        val m1 = LastMessageSentMessenger()

        val p2 = Player("random")
        val m2 = LastMessageSentMessenger()

        val party = Party(2)
        party.connectPlayer(p1, m1)
        party.connectPlayer(p2, m2)

        assertNull(m1.lastMessageSent)
        assertNull(m2.lastMessageSent)

        party.sendMessageToPlayer(p1, "last")

        assertEquals(m1.lastMessageSent, "last")
        assertNull(m2.lastMessageSent)

        party.sendMessageToPlayer(p2, "first")

        assertEquals(m1.lastMessageSent, "last")
        assertEquals(m2.lastMessageSent, "first")
    }

    @Test fun testCannotSendMessagesToPlayerNotInParty () = runBlocking<Unit> {
        val p1 = Player("bob")
        val p2 = Player("joe")
        val p3 = Player("jerry")
        val party = Party(3)

        party.connectPlayer(p1, MockMessenger())
        party.connectPlayer(p2, MockMessenger())

        assertFailsWith(PlayerNotInParty::class) {
            party.sendMessageToPlayer(p3, "test")
        }
    }

    @Test fun testCanBroadcastMessage () = runBlocking<Unit> {
        val p1 = Player("bob")
        val m1 = LastMessageSentMessenger()
        val p2 = Player("joe")
        val m2 = LastMessageSentMessenger()
        val p3 = Player("jerry")
        val m3 = LastMessageSentMessenger()
        val party = Party(3)

        party.connectPlayer(p1, m1)
        party.connectPlayer(p2, m2)
        party.connectPlayer(p3, m3)

        party.broadcastMessage("broadcast")

        assertEquals(m1.lastMessageSent, "broadcast")
        assertEquals(m2.lastMessageSent, "broadcast")
        assertEquals(m3.lastMessageSent, "broadcast")
    }

    @Test fun testCanReceiveMessagesFromPlayer () = runBlocking<Unit> {
        val p1 = Player("bob")
        val p2 = Player("joe")
        val listener = LastMessageReceivedListener()
        val party = Party(3).also { it.addPartyListener(listener) }

        party.connectPlayer(p1, MockMessenger())
        party.connectPlayer(p2, MockMessenger())

        party.receiveMessageFromPlayer(p1, "hello")
        assertEquals(listener.lastMessageReceived?.first, p1)
        assertEquals(listener.lastMessageReceived?.second, "hello")

        party.receiveMessageFromPlayer(p2, "bye")
        assertEquals(listener.lastMessageReceived?.first, p2)
        assertEquals(listener.lastMessageReceived?.second, "bye")
    }

    @Test fun testCannotReceiveMessagesFromPlayerNotInParty () = runBlocking<Unit> {
        val p1 = Player("bob")
        val p2 = Player("joe")
        val listener = LastMessageReceivedListener()
        val party = Party(3).also { it.addPartyListener(listener) }

        party.connectPlayer(p1, MockMessenger())

        assertFailsWith(PlayerNotInParty::class) {
            party.receiveMessageFromPlayer(p2, "bye")
        }
        assertNull(listener.lastMessageReceived)
    }

    @Test fun testCannotReceiveMessagesFromDisconnectedPlayer () = runBlocking<Unit> {
        val p1 = Player("bob")
        val listener = LastMessageReceivedListener()
        val party = Party(1).also {
            it.setRememberDisconnectedPlayers(true)
            it.addPartyListener(listener)
        }

        party.connectPlayer(p1, MockMessenger())
        party.disconnectPlayer(p1)

        assertFailsWith(PlayerDisconnected::class) {
            party.receiveMessageFromPlayer(p1, "hello")
        }
        assertNull(listener.lastMessageReceived)
    }

    @Test fun testCannotSendMessagesToDisconnectedPlayer () = runBlocking<Unit> {
        val p1 = Player("bob")
        val m1 = LastMessageSentMessenger()

        val party = Party(1).apply { setRememberDisconnectedPlayers(true) }
        party.connectPlayer(p1, m1)

        party.disconnectPlayer(p1)

        assertFailsWith(PlayerDisconnected::class) {
            party.sendMessageToPlayer(p1, "last")
        }
        assertNull(m1.lastMessageSent)

        party.connectPlayer(p1, m1)

        party.sendMessageToPlayer(p1, "last")
        assertEquals(m1.lastMessageSent, "last")
    }
}

class MockMessenger : Messenger {

    override fun send(message: String) : Boolean {
        return true
    }

}

class LastMessageSentMessenger : Messenger {

    var lastMessageSent: String? = null

    override fun send(message: String): Boolean {
        lastMessageSent = message
        return true
    }

}

class LastMessageReceivedListener : BasePartyListener() {

    var lastMessageReceived: Pair<Player, String>? = null

    override fun receivedMessageFromPlayer(player: Player, message: String) {
        lastMessageReceived = Pair(player, message)
    }
}
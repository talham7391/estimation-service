package estimationserver.party

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class TestParty {

    @Test fun testPlayersCanConnectAndDisconnect () {
        val player = Player("bob")
        val party = Party(4)

        assert(party.getPlayers().isEmpty())

        party.connectPlayer(player, MockMessenger())
        assertEquals(party.getPlayers(), setOf(player))

        party.disconnectPlayer(player)
        assert(party.getPlayers().isEmpty())
    }

    @Test fun testPartyDoesNotAcceptMoreThanAllowedPlayers () {
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

    @Test fun testPartyRemembersDisconnectedPlayers () {
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

    @Test fun testPartyForgetsDisconnectedPlayers () {
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

    @Test fun testSameConnectionIdCannotBeUsedTwice () {
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

    @Test fun testCanSendMessagesToPlayer () {
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

    @Test fun testCannotSendMessagesToPlayerNotInParty () {
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

    @Test fun testCanBroadcastMessage () {
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

    @Test fun testCanReceiveMessagesFromPlayer () {
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

    @Test fun testCannotReceiveMessagesFromPlayerNotInParty () {
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

    @Test fun testCannotReceiveMessagesFromDisconnectedPlayer () {
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

    @Test fun testCannotSendMessagesToDisconnectedPlayer () {
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

    @Test fun testGetConnectedPlayers () {
        val party = Party(3)

        val p1 = Player("Bob")
        val p2 = Player("John")
        val p3 = Player("Joe")

        assertEquals(0, party.getConnectedPlayers().size)

        party.connectPlayer(p1, MockMessenger())
        party.connectPlayer(p2, MockMessenger())

        assertEquals(setOf(p1, p2), party.getConnectedPlayers())

        party.disconnectPlayer(p1)

        assertEquals(setOf(p2), party.getConnectedPlayers())

        party.setRememberDisconnectedPlayers(true)

        party.connectPlayer(p1, MockMessenger())
        party.connectPlayer(p3, MockMessenger())

        assertEquals(setOf(p1, p2, p3), party.getConnectedPlayers())

        party.disconnectPlayer(p2)
        party.disconnectPlayer(p3)

        assertEquals(setOf(p1), party.getConnectedPlayers())
    }
}

class LastMessageReceivedListener : BasePartyListener() {

    var lastMessageReceived: Pair<Player, String>? = null

    override fun receivedMessageFromPlayer (player: Player, message: String) {
        lastMessageReceived = Pair(player, message)
    }
}
package estimationserver

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import estimationserver.party.LastMessageSentMessenger
import estimationserver.party.MockMessenger
import estimationserver.party.Party
import estimationserver.party.Player
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TestLobby {

    @Test fun testLobbyRelaysConnectionsAndDisconnections () {
        val party = Party(3)
        val lobby = Lobby(party)

        party.addPartyListener(lobby)

        val p1 = Player("Bob")
        val m1 = LastMessageSentMessenger()
        val p2 = Player("Joe")
        val m2 = LastMessageSentMessenger()

        assertNull(m1.lastMessageSent)
        assertNull(m2.lastMessageSent)

        party.connectPlayer(p1, m1)

        val objectMapper = jacksonObjectMapper()

        assertNotNull(m1.lastMessageSent)

        var res = objectMapper.readValue<ConnectedPlayersResponse>(m1.lastMessageSent!!)
        assertEquals(res.players, setOf(p1))
        assertNull(m2.lastMessageSent)

        m1.lastMessageSent = null

        party.connectPlayer(p2, m2)

        assertNotNull(m1.lastMessageSent)
        assertNotNull(m2.lastMessageSent)

        res = objectMapper.readValue(m1.lastMessageSent!!)
        assertEquals(res.players, setOf(p1, p2))

        res = objectMapper.readValue(m2.lastMessageSent!!)
        assertEquals(res.players, setOf(p1, p2))

        m1.lastMessageSent = null
        m2.lastMessageSent = null

        party.disconnectPlayer(p1)

        assertNotNull(m2.lastMessageSent)
        res = objectMapper.readValue(m2.lastMessageSent!!)
        assertEquals(res.players, setOf(p2))
        assertNull(m1.lastMessageSent)
    }

    @Test fun testPlayerCannotSendReadyIfLobbyIsNotFull () {
        val party = Party(3)
        val lobby = Lobby(party)

        party.addPartyListener(lobby)

        val p1 = Player("Bob")
        val m1 = LastMessageSentMessenger()
        val p2 = Player("Joe")

        party.connectPlayer(p1, m1)

        val objectMapper = jacksonObjectMapper()
        val req = PlayersReadyRequest(true)

        party.receiveMessageFromPlayer(p1, objectMapper.writeValueAsString(req))

        assertNotNull(m1.lastMessageSent)
        objectMapper.readValue<ErrorResponse>(m1.lastMessageSent!!)

        m1.lastMessageSent = null

        party.connectPlayer(p2, MockMessenger())

        party.receiveMessageFromPlayer(p1, objectMapper.writeValueAsString(req))

        // test incomplete - should recieve input about next stage
    }

}

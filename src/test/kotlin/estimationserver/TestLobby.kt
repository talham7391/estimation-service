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

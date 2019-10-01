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

class TestPreGameLobby {

    @Test fun testPreGameLobbyRelaysConnectionsAndDisconnections () = runBlocking<Unit> {

    }

    @Test fun testPlayerCanSendReady () = runBlocking<Unit> {

    }

    @Test fun testPlayerReceivesInfoOnConnection () = runBlocking<Unit> {

    }

    @Test fun testPlayerCannotSendReadyIfLobbyIsNotFull () = runBlocking<Unit> {

    }

}

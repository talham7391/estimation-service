package estimationserver

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import estimationserver.party.LastMessageSentMessenger
import estimationserver.party.Party
import estimationserver.party.Player
import kotlinx.coroutines.runBlocking
import talham7391.estimation.Estimation
import kotlin.test.Test
import kotlin.test.assertEquals

class TestPreGameLobby {

    val objectMapper = jacksonObjectMapper()

    inline fun setupPreGameLobby (func: (
        Party,
        Player,
        LastMessageSentMessenger,
        Player,
        LastMessageSentMessenger,
        Player,
        LastMessageSentMessenger,
        Player,
        LastMessageSentMessenger
    ) -> Unit) {
        val party = Party(4)
        party.setRememberDisconnectedPlayers(true)

        val p1 = Player("Bob")
        val m1 = LastMessageSentMessenger()
        val p2 = Player("Joe")
        val m2 = LastMessageSentMessenger()
        val p3 = Player("Mary")
        val m3 = LastMessageSentMessenger()
        val p4 = Player("Jane")
        val m4 = LastMessageSentMessenger()

        val estimation = Estimation(
            EstimationPlayer(p1),
            EstimationPlayer(p2),
            EstimationPlayer(p3),
            EstimationPlayer(p4)
        )

        val preGameLobby = PreGameLobby(estimation, party)
        party.addPartyListener(preGameLobby)

        party.connectPlayer(p1, m1)
        party.connectPlayer(p2, m2)
        party.connectPlayer(p3, m3)
        party.connectPlayer(p4, m4)

        func(party, p1, m1, p2, m2, p3, m3, p4, m4)
    }

    @Test fun testPlayerCanSendReady () = setupPreGameLobby { party, p1, m1, p2, m2, p3, _, p4, _ ->
        val r = objectMapper.writeValueAsString(PlayerReadyRequest("PLAYER_READY", true))
        party.receiveMessageFromPlayer(p1, r)

        var expected = PreGameLobbyState()
        expected.readyStatus = setOf(
            PlayerReadyData(p1, true),
            PlayerReadyData(p2, false),
            PlayerReadyData(p3, false),
            PlayerReadyData(p4, false)
        )
        var actual = objectMapper.readValue<PreGameLobbyState>(m1.lastMessageSent!!)
        assertEquals(expected, actual)

        party.receiveMessageFromPlayer(p3, r)

        expected = PreGameLobbyState()
        expected.readyStatus = setOf(
            PlayerReadyData(p1, true),
            PlayerReadyData(p2, false),
            PlayerReadyData(p3, true),
            PlayerReadyData(p4, false)
        )
        actual = objectMapper.readValue(m2.lastMessageSent!!)
        assertEquals(expected, actual)
    }

    @Test fun testPlayerReceivesAllInfoOnConnection () = setupPreGameLobby { party, p1, m1, p2, _, p3, _, p4, _ ->
        party.disconnectPlayer(p1)
        party.connectPlayer(p1, m1)

        val expected = PreGameLobbyState()
        expected.readyStatus = setOf(
            PlayerReadyData(p1, false),
            PlayerReadyData(p2, false),
            PlayerReadyData(p3, false),
            PlayerReadyData(p4, false)
        )
        expected.playerScores = setOf(
            PlayerScoreData(p1, 0),
            PlayerScoreData(p2, 0),
            PlayerScoreData(p3, 0),
            PlayerScoreData(p4, 0)
        )
        val actual = objectMapper.readValue<PreGameLobbyState>(m1.lastMessageSent!!)
        assertEquals(expected, actual)
    }

    @Test fun testPlayerCannotSendStartIfLobbyIsNotAllReady () = setupPreGameLobby { party, p1, m1, p2, m2, p3, _, p4, _ ->
        val r = objectMapper.writeValueAsString(PlayerReadyRequest("PLAYER_READY", true))
        party.receiveMessageFromPlayer(p1, r)
        party.receiveMessageFromPlayer(p2, r)

        val s = objectMapper.writeValueAsString(StartGameRequest("START_GAME", true))
        party.receiveMessageFromPlayer(p1, s)

        objectMapper.readValue<ErrorResponse>(m1.lastMessageSent!!)
        objectMapper.readValue<PreGameLobbyState>(m2.lastMessageSent!!)

        party.receiveMessageFromPlayer(p3, r)
        party.receiveMessageFromPlayer(p4, r)

        party.receiveMessageFromPlayer(p2, s)

        // test incomplete - needs info about next stage
    }

}

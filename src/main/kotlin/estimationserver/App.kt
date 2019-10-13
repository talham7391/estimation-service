package estimationserver

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import estimationserver.party.Player
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.cio.websocket.readBytes
import io.ktor.http.cio.websocket.send
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onReceiveOrNull
import kotlinx.coroutines.selects.select

fun Application.main () {
    install(WebSockets)
    install(ContentNegotiation) {
        jackson {  }
    }

    routing {
        get {
            call.respond(HttpStatusCode.OK)
        }

        route("/games") {
            get {
                val partyIds = PartyManager.getPartyIds()
                call.respond(PartyIdsResponse(partyIds))
            }

            post {
                val partyId = PartyManager.createParty()
                val partyWrapper = PartyManager.getParty(partyId)

                Lobby(partyWrapper.party)

                call.respond(PartyIdResponse(partyId))
            }
        }

        route("/connect/{partyId}") {
            webSocket {
                val partyId = call.parameters["partyId"]
                partyId ?: return@webSocket

                val objectMapper = jacksonObjectMapper()
                val connectionId = objectMapper.readValue<ConnectionIdRequest>(incoming.receive().readBytes())

                val player = Player(connectionId.connectionId)
                val messenger = PlayerMessenger(outgoing)

                val partyWrapper = PartyManager.getParty(partyId)
                try {
                    partyWrapper.mutex.lock()
                    partyWrapper.party.connectPlayer(player, messenger)
                } catch (e: Exception) {
                    throw e
                } finally {
                    partyWrapper.mutex.unlock()
                }

                for (message in incoming) {
                    val m = String(message.readBytes())
                    try {
                        partyWrapper.mutex.lock()
                        partyWrapper.party.receiveMessageFromPlayer(player, m)
                    } catch (e: Exception) {
                        throw e
                    } finally {
                        partyWrapper.mutex.unlock()
                    }
                }

                try {
                    partyWrapper.mutex.lock()
                    partyWrapper.party.disconnectPlayer(player)
                } catch (e: Exception) {
                    throw e
                } finally {
                    partyWrapper.mutex.unlock()
                }
            }
        }
    }
}

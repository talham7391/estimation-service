package estimationserver

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import estimationserver.party.Player
import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.callId
import io.ktor.http.HttpStatusCode
import io.ktor.http.cio.websocket.readBytes
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import kotlin.random.Random

fun Application.main () {
    val logger = LoggerFactory.getLogger("App")

    install(CallLogging) {
        level = Level.INFO
    }
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

                val callId = Random.nextInt(10_000_000)

                logger.info("CallId[$callId] PartyId[$partyId] new connection.")

                val objectMapper = jacksonObjectMapper()
                val connectionId = objectMapper.readValue<ConnectionIdRequest>(incoming.receive().readBytes())

                logger.info("CallId[$callId] PartyId[$partyId] ConnectionId[$connectionId]")

                val player = Player(connectionId.connectionId)
                val messenger = PlayerMessenger(outgoing)

                val partyWrapper = PartyManager.getParty(partyId)

                try {
                    try {
                        partyWrapper.mutex.lock()
                        partyWrapper.party.connectPlayer(player, messenger)
                        logger.info("CallId[$callId] PartyId[$partyId] ConnectionId[$connectionId] connected.")
                    } catch (e: Exception) {
                        logger.error("CallId[$callId] PartyId[$partyId] ConnectionId[$connectionId] error.")
                        e.printStackTrace()
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
                            logger.error("CallId[$callId] PartyId[$partyId] ConnectionId[$connectionId] error.")
                            e.printStackTrace()
                            throw e
                        } finally {
                            partyWrapper.mutex.unlock()
                        }
                    }
                } catch (e: Exception) {
                    throw e
                } finally {
                    try {
                        partyWrapper.mutex.lock()
                        partyWrapper.party.disconnectPlayer(player)
                        logger.info("CallId[$callId] PartyId[$partyId] ConnectionId[$connectionId] disconnected.")
                    } catch (e: Exception) {
                        logger.error("CallId[$callId] PartyId[$partyId] ConnectionId[$connectionId] error.")
                        e.printStackTrace()
                        throw e
                    } finally {
                        partyWrapper.mutex.unlock()
                    }
                }
            }
        }
    }
}

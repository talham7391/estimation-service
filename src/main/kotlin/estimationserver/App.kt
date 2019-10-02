package estimationserver

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket

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
                val party = PartyManager.getParty(partyId)

                Lobby(party)

                call.respond(PartyIdResponse(partyId))
            }
        }

//        route("/connect/{partyId}") {
//            webSocket {
//                val partyId = call.parameters["partyId"]
//                partyId ?: return@webSocket
//
//                val connectionId = incoming.receive().readBytes().toString(Charsets.UTF_8)
//                val player = Player(connectionId)
//
//                val party = PartyManager.getParty(partyId)
//            }
//        }
    }
}

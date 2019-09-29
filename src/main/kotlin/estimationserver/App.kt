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

fun Application.main () {
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
                call.respond(PartyIdResponse(partyId))
            }
        }
    }
}

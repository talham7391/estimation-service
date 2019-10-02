package estimationserver

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class BasePreGameLobbyRequest (val type: String)

data class PlayerReadyRequest (val type: String, val ready: Boolean)

data class StartGameRequest (val type: String, val start: Boolean)

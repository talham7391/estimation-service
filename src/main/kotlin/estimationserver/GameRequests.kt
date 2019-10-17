package estimationserver

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class BaseGameRequest (val type: String)

data class BidRequest (val type: String, val bid: Int)

data class PassRequest (val type: String)

data class DeclareTrumpRequest (val type: String, val suit: String)

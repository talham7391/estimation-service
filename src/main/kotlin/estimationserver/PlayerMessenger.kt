package estimationserver

import estimationserver.party.Messenger
import io.ktor.http.cio.websocket.Frame
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch

class PlayerMessenger (

    private val sendChannel: SendChannel<Frame>

) : Messenger {

    override fun send(message: String): Boolean {
        GlobalScope.launch {
            try {
                sendChannel.send(Frame.Text(message))
            } catch (e: ClosedSendChannelException) {}
        }
        return true
    }

}
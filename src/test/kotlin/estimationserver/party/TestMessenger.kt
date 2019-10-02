package estimationserver.party

class MockMessenger : Messenger {

    override fun send (message: String) : Boolean {
        return true
    }

}

class LastMessageSentMessenger : Messenger {

    var lastMessageSent: String? = null

    override fun send (message: String): Boolean {
        lastMessageSent = message
        return true
    }

}

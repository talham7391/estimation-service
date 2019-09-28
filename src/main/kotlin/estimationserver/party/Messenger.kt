package estimationserver.party

interface Messenger {

    fun send (message: String) : Boolean

}
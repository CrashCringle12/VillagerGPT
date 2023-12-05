package tj.horner.villagergpt.events

import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import tj.horner.villagergpt.conversation.NPCConversation

class NPCConversationStartEvent(val conversation: NPCConversation) : Event() {
    companion object {
        private val handlers = HandlerList()

        @Suppress("unused")
        @JvmStatic
        fun getHandlerList() = handlers
    }

    override fun getHandlers(): HandlerList {
        return NPCConversationStartEvent.handlers
    }
}
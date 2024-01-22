package tj.horner.villagergpt.events

import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import tj.horner.villagergpt.conversation.NPCConversation
import tj.horner.villagergpt.conversation.NPCGlobalConversation

class NPCGlobalConversationStartEvent (val conversation: NPCGlobalConversation) : Event() {
    companion object {
        private val handlers = HandlerList()

        @Suppress("unused")
        @JvmStatic
        fun getHandlerList() = handlers
    }

    override fun getHandlers(): HandlerList {
        return NPCGlobalConversationStartEvent .handlers
    }
}
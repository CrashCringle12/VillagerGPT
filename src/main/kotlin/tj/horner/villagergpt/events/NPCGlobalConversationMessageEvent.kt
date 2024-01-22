package tj.horner.villagergpt.events

import tj.horner.villagergpt.conversation.NPCGlobalConversation
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatMessage
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
class NPCGlobalConversationMessageEvent (val conversation: NPCGlobalConversation, val message: ChatMessage) : Event(true) {
    companion object {
        private val handlers = HandlerList()

        @Suppress("unused")
        @JvmStatic
        fun getHandlerList() = handlers
    }

    override fun getHandlers(): HandlerList {
        return NPCGlobalConversationMessageEvent.handlers
    }
}
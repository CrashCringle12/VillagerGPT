package tj.horner.villagergpt.events

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatMessage
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import tj.horner.villagergpt.conversation.NPCConversation

@OptIn(BetaOpenAI::class)
class NPCConversationMessageEvent(val conversation: NPCConversation, val message: ChatMessage) : Event(true) {
    companion object {
        private val handlers = HandlerList()

        @Suppress("unused")
        @JvmStatic
        fun getHandlerList() = handlers
    }

    override fun getHandlers(): HandlerList {
        return NPCConversationMessageEvent.handlers
    }
}
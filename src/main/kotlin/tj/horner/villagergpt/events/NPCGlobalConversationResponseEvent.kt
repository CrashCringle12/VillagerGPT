package tj.horner.villagergpt.events

import net.citizensnpcs.api.npc.NPC
import net.kyori.adventure.text.Component
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import tj.horner.villagergpt.conversation.NPCGlobalConversation

class NPCGlobalConversationResponseEvent(val npc: NPC, val conversation: NPCGlobalConversation, val message: Component) : Event(true) {
    companion object {
        private val handlers = HandlerList()

        @Suppress("unused")
        @JvmStatic
        fun getHandlerList() = handlers
    }

    override fun getHandlers(): HandlerList {
        return NPCGlobalConversationResponseEvent.handlers
    }
}
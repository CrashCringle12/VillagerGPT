package tj.horner.villagergpt.events

import net.citizensnpcs.api.npc.NPC
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import tj.horner.villagergpt.conversation.NPCGlobalConversation

class NPCGlobalConversationEndEvent(val conversation: NPCGlobalConversation) : Event() {
    companion object {
        private val handlers = HandlerList()

        @Suppress("unused")
        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

    override fun getHandlers(): HandlerList {
        return NPCGlobalConversationEndEvent.handlers
    }
}
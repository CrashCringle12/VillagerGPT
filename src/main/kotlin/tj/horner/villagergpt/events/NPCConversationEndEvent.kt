package tj.horner.villagergpt.events

import net.citizensnpcs.api.npc.NPC
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class NPCConversationEndEvent(val player: Player, val npc: NPC) : Event() {
    companion object {
        private val handlers = HandlerList()

        @Suppress("unused")
        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

    override fun getHandlers(): HandlerList {
        return NPCConversationEndEvent.handlers
    }
}
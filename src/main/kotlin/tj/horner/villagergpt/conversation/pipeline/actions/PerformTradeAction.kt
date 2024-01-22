package tj.horner.villagergpt.conversation.pipeline.actions

import net.citizensnpcs.api.npc.NPC
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import tj.horner.villagergpt.conversation.pipeline.ConversationMessageAction

class PerformTradeAction(private val npc : NPC, private val message: String) : ConversationMessageAction {

    override fun run() {
        when (message) {
            "accept" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "barter admin-accept ${npc.name}")
            "decline" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "barter admin-decline ${npc.name}")
            "cancel" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "barter admin-cancel ${npc.name}")

            else -> {
            }

        }

    }
}
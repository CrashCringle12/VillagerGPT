package tj.horner.villagergpt.conversation.pipeline.actions

import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import net.citizensnpcs.api.npc.NPC
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import tj.horner.villagergpt.conversation.NPCGlobalConversation
import tj.horner.villagergpt.conversation.pipeline.ConversationMessageAction

class PerformTradeAction(private val npc : NPC, private val conversation : NPCGlobalConversation, private val message: String) : ConversationMessageAction {

    override fun run() {
        when (message) {
            "accept" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "barter admin-accept ${npc.name}")
            "decline" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "barter admin-deny ${npc.name}")
            "cancel" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "barter admin-cancel ${npc.name}")
            else -> {
            }

        }
        // If there are only two npcs get the other npc
        if (conversation.npcs.size == 2) {
            val otherNpc = conversation.npcs.filter { it != npc }.first()
            if (message == "accept") {
                val npcPlayer = (npc.entity as Player)
                var npcInventory = ""
                for (item in npcPlayer.inventory.contents) {
                    if (item != null) {
                        npcInventory += item.amount.toString() + " "  +item.type.name + "\n"
                    }
                }
                conversation.npcMessages[npc.uniqueId]?.add(
                        ChatMessage(
                                role = ChatRole.System,
                                content = "You have accepted the trade. Current Inventory:\n$npcInventory"
                        )
                )
                val otherNpcPlayer = (otherNpc.entity as Player)
                var otherNpcInventory = ""
                for (item in otherNpcPlayer.inventory.contents) {
                    if (item != null) {
                        otherNpcInventory += item.amount.toString() + " "  +item.type.name + "\n"
                    }
                }
                conversation.npcMessages[otherNpc.uniqueId]?.add(
                        ChatMessage(
                                role = ChatRole.System,
                                content = "The trade has been accepted. Current Inventory:\n$otherNpcInventory"
                        )
                )
            } else if (message == "decline") {
                conversation.npcMessages[npc.uniqueId]?.add(
                        ChatMessage(
                                role = ChatRole.System,
                                content = "You have declined the trade."
                        )
                )
                conversation.npcMessages[otherNpc.uniqueId]?.add(
                        ChatMessage(
                                role = ChatRole.System,
                                content = npc.name + " has declined the trade."
                        )
                )
            } else if (message == "cancel") {
                conversation.npcMessages[npc.uniqueId]?.add(
                        ChatMessage(
                                role = ChatRole.System,
                                content = "You have rescinded the trade."
                        )
                )
                conversation.npcMessages[otherNpc.uniqueId]?.add(
                        ChatMessage(
                                role = ChatRole.System,
                                content = npc.name + " has rescinded the trade."
                        )
                )
            }
        }
    }
}
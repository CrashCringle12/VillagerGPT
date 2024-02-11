package tj.horner.villagergpt.conversation.pipeline.actions

import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.npc.NPC
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import tj.horner.villagergpt.VillagerGPT.Companion.ALTNAME
import tj.horner.villagergpt.VillagerGPT.Companion.PLNAME
import tj.horner.villagergpt.conversation.NPCGlobalConversation
import tj.horner.villagergpt.conversation.pipeline.ConversationMessageAction

class PerformTradeAction(private val conversation : NPCGlobalConversation, private val message: String) : ConversationMessageAction {

    override fun run() {
        val orchestrator = Bukkit.getPlayer("CrashCringle12")
        if (orchestrator == null) {
            Bukkit.getLogger().info("Orchestrator not found")
            return
        }
        when (message) {
            "accept" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "barter admin-accept ${conversation.npc.name}")
            "decline" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "barter admin-deny ${conversation.npc.name}")
            "cancel" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "barter admin-cancel ${conversation.npc.name}")
            else -> {
            }

        }
        // If there are only two npcs get the other npc
            val npc = conversation.npc
            var otherNpc: NPC = npc
            // Get the other npc named "Bobby"
            for (npc1 in CitizensAPI.getNPCRegistry()) {
                if (npc1.name.uppercase() == ALTNAME) {
                    otherNpc = npc1
                }
            }
            if (message == "accept") {
                val npcPlayer = (npc.entity as Player)
                var npcInventory = ""
                for (item in npcPlayer.inventory.contents) {
                    if (item != null) {
                        npcInventory += item.amount.toString() + " "  +item.type.name + " | "
                    }
                }
                orchestrator.chat("[SYSTEM][TARGET-$PLNAME] barterSplit $npcInventory")

                val otherNpcPlayer = (otherNpc.entity as Player)
                var otherNpcInventory = ""
                for (item in otherNpcPlayer.inventory.contents) {
                    if (item != null) {
                        otherNpcInventory += item.amount.toString() + " "  +item.type.name + " | "
                    }
                }
               // orchestrator.chat("[SYSTEM][$ALTNAME] The trade has been accepted. Current Inventory:\n$otherNpcInventory")
                orchestrator.chat("[SYSTEM][TARGET-$ALTNAME] barterSplit $otherNpcInventory")

            } else if (message == "decline") {
                orchestrator.chat("[SYSTEM][$PLNAME] You have declined the trade.")
                orchestrator.chat("[SYSTEM][$ALTNAME] $PLNAME has declined the trade.")
            } else if (message == "cancel") {
                orchestrator.chat("[SYSTEM][$PLNAME] You have rescinded the trade.")
                orchestrator.chat("[SYSTEM][$ALTNAME] $PLNAME has rescinded the trade.")
            }
        }
    }
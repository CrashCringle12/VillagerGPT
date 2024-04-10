package tj.horner.villagergpt.conversation.pipeline.actions

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import tj.horner.villagergpt.VillagerGPT
import tj.horner.villagergpt.conversation.VillagerConversation
import tj.horner.villagergpt.conversation.pipeline.ConversationMessageAction
import tj.horner.villagergpt.events.VillagerConversationEndEvent
import tj.horner.villagergpt.events.VillagerConversationSummarizeEvent

class EndConversationAction (private val villagerconversation: VillagerConversation, private val plugin: VillagerGPT) : ConversationMessageAction {
    override fun run() {
//        plugin.logger.info("Summarizing conversation")
//        val summarizeEvent = VillagerConversationSummarizeEvent(villagerconversation.player, villagerconversation.villager)
//        plugin.server.pluginManager.callEvent(summarizeEvent)
        villagerconversation.ended = true
        val endEvent = VillagerConversationEndEvent(villagerconversation.player, villagerconversation.villager)
        plugin.server.pluginManager.callEvent(endEvent)
    }
}
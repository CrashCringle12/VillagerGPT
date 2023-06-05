package tj.horner.villagergpt.conversation.pipeline.actions

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import tj.horner.villagergpt.VillagerGPT
import tj.horner.villagergpt.conversation.VillagerConversation
import tj.horner.villagergpt.conversation.pipeline.ConversationMessageAction
import tj.horner.villagergpt.events.VillagerConversationEndEvent

class EndConversationAction (private val villagerconversation: VillagerConversation, private val plugin: VillagerGPT) : ConversationMessageAction {
    override fun run() {
        villagerconversation.ended = true
        val endEvent = VillagerConversationEndEvent(villagerconversation.player, villagerconversation.villager)
        plugin.server.pluginManager.callEvent(endEvent)
    }
}
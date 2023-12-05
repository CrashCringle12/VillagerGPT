package tj.horner.villagergpt.conversation.pipeline.actions

import tj.horner.villagergpt.VillagerGPT
import tj.horner.villagergpt.conversation.NPCConversation
import tj.horner.villagergpt.conversation.pipeline.ConversationMessageAction
import tj.horner.villagergpt.events.NPCConversationEndEvent

class EndConversationAction (private val villagerconversation: NPCConversation, private val plugin: VillagerGPT) : ConversationMessageAction {
    override fun run() {
        villagerconversation.ended = true
        val endEvent = NPCConversationEndEvent(villagerconversation.player, villagerconversation.npc)
        plugin.server.pluginManager.callEvent(endEvent)
    }
}
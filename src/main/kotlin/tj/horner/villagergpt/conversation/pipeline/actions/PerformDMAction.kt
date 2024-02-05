package tj.horner.villagergpt.conversation.pipeline.actions

import net.citizensnpcs.api.npc.NPC
import net.kyori.adventure.text.Component
import tj.horner.villagergpt.VillagerGPT
import tj.horner.villagergpt.conversation.NPCGlobalConversation
import tj.horner.villagergpt.conversation.pipeline.ConversationMessageAction

class PerformDMAction(private val npc: NPC,  private val message: Component, private val globalConversation : NPCGlobalConversation, private val plugin : VillagerGPT) : ConversationMessageAction {

    override fun run() {
        globalConversation.sendResponseToNPCs(message, npc)
    }
}
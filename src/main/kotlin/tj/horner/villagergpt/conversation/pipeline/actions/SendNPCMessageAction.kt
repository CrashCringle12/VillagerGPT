package tj.horner.villagergpt.conversation.pipeline.actions

import net.citizensnpcs.api.npc.NPC
import net.kyori.adventure.text.Component
import tj.horner.villagergpt.conversation.NPCGlobalConversation
import tj.horner.villagergpt.conversation.pipeline.ConversationMessageAction

class SendNPCMessageAction(private val npc: NPC, private val globalConversation : NPCGlobalConversation, private val message: Component) : ConversationMessageAction {

    override fun run() {
        globalConversation.sendResponseToNPCs(message, npc)
    }
}
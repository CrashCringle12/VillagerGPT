package tj.horner.villagergpt.conversation.pipeline

import net.citizensnpcs.api.npc.NPC
import tj.horner.villagergpt.conversation.NPCConversation
import tj.horner.villagergpt.conversation.NPCGlobalConversation

interface ConversationMessageProcessor {
    fun processMessage(message: String, conversation: NPCConversation): Collection<ConversationMessageAction>?
    fun processMessage(message: String, conversation: NPCGlobalConversation, npc : NPC): Collection<ConversationMessageAction>?

}
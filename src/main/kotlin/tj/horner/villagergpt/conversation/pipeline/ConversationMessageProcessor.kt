package tj.horner.villagergpt.conversation.pipeline

import tj.horner.villagergpt.conversation.NPCConversation
import tj.horner.villagergpt.conversation.NPCGlobalConversation

interface ConversationMessageProcessor {
    fun processMessage(message: String, conversation: NPCConversation): Collection<ConversationMessageAction>?
    fun processMessage(message: String, conversation: NPCGlobalConversation): Collection<ConversationMessageAction>?

}
package tj.horner.villagergpt.conversation.pipeline

import tj.horner.villagergpt.conversation.NPCConversation

interface ConversationMessageProcessor {
    fun processMessage(message: String, conversation: NPCConversation): Collection<ConversationMessageAction>?
}
package tj.horner.villagergpt.conversation.pipeline

import tj.horner.villagergpt.conversation.NPCConversation

interface ConversationMessageProducer {
    suspend fun produceNextMessage(conversation: NPCConversation): String
}
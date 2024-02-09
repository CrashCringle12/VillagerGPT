package tj.horner.villagergpt.conversation.pipeline

import tj.horner.villagergpt.conversation.NPCConversation
import tj.horner.villagergpt.conversation.NPCGlobalConversation

interface ConversationMessageProducer {
    suspend fun produceNextMessage(conversation: NPCConversation): String
    suspend fun produceNextMessage(conversation: NPCGlobalConversation): String

}
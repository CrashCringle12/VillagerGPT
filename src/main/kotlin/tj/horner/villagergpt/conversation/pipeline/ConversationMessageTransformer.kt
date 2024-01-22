package tj.horner.villagergpt.conversation.pipeline

import tj.horner.villagergpt.conversation.NPCConversation
import tj.horner.villagergpt.conversation.NPCGlobalConversation

interface ConversationMessageTransformer {
    fun transformMessage(message: String, conversation: NPCConversation): String
    fun transformMessage(message: String, conversation: NPCGlobalConversation): String

}
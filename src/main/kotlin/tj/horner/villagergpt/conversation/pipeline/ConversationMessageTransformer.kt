package tj.horner.villagergpt.conversation.pipeline

import tj.horner.villagergpt.conversation.NPCConversation

interface ConversationMessageTransformer {
    fun transformMessage(message: String, conversation: NPCConversation): String
}
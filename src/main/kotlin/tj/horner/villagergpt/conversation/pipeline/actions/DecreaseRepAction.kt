package tj.horner.villagergpt.conversation.pipeline.actions

import tj.horner.villagergpt.conversation.VillagerConversation
import tj.horner.villagergpt.conversation.pipeline.ConversationMessageAction

class DecreaseRepAction (private val conversation: VillagerConversation, private val type: String, private val num: Int) : ConversationMessageAction {
    override fun run() {
        conversation.decreaseReputation(type, num)
    }
}
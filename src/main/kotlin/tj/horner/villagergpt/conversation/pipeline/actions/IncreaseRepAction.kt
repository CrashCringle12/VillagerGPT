package tj.horner.villagergpt.conversation.pipeline.actions

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import tj.horner.villagergpt.conversation.VillagerConversation
import tj.horner.villagergpt.conversation.pipeline.ConversationMessageAction

class IncreaseRepAction (private val conversation: VillagerConversation, private val type: String, private val num: Int) :  ConversationMessageAction {
    override fun run() {
        conversation.increaseReputation(type, num)
    }
}
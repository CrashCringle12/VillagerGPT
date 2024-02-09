package tj.horner.villagergpt.conversation.pipeline.actions;
import net.kyori.adventure.text.Component
import tj.horner.villagergpt.conversation.NPCGlobalConversation
import tj.horner.villagergpt.conversation.pipeline.ConversationMessageAction

class SendGlobalMessageAction(private val globalConversation : NPCGlobalConversation, private val message: Component) : ConversationMessageAction {

    override fun run() {
        globalConversation.npc.
    }
}s
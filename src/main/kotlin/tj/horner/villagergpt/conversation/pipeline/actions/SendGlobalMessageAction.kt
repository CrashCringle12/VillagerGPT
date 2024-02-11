package tj.horner.villagergpt.conversation.pipeline.actions;
import net.kyori.adventure.text.Component
import tj.horner.villagergpt.conversation.NPCGlobalConversation
import tj.horner.villagergpt.conversation.pipeline.ConversationMessageAction
import java.util.logging.Logger

class SendGlobalMessageAction(private val globalConversation : NPCGlobalConversation, private val message: String, private val logger: Logger) : ConversationMessageAction {

    override fun run() {

        // Get a random item out the npcs inventory to trade
        val npcPlayer = globalConversation.npc.entity as org.bukkit.entity.Player
        var localmessage = message.replace("\n", "").replace("\r", "").strip();
        logger.info("NPC Player: $npcPlayer")
        logger.info("Message: $localmessage")
        npcPlayer.chat(localmessage)
    }
}
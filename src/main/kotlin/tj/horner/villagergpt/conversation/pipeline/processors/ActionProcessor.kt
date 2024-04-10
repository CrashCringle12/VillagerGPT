package tj.horner.villagergpt.conversation.pipeline.processors

import com.destroystokyo.paper.entity.villager.ReputationType
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import tj.horner.villagergpt.VillagerGPT
import tj.horner.villagergpt.conversation.VillagerConversation
import tj.horner.villagergpt.conversation.pipeline.ConversationMessageAction
import tj.horner.villagergpt.conversation.pipeline.ConversationMessageProcessor
import tj.horner.villagergpt.conversation.pipeline.ConversationMessageTransformer
import tj.horner.villagergpt.conversation.pipeline.actions.*
import java.util.logging.Logger

class ActionProcessor(private val plugin: VillagerGPT)  : ConversationMessageProcessor, ConversationMessageTransformer {
    private val actionRegex = Regex("ACTION:([A-Z_]+)")
    private val repRegex = Regex("ACTION:((INCREASE|DECREASE)_REP:([^:]+):([^:]+))")

    override fun processMessage(
        message: String,
        conversation: VillagerConversation
    ): Collection<ConversationMessageAction> {
        val parsedActions = getActions(message)
        val parsedRepActions = getRepActions(message)
        // Combine parsedActions.mapNotNull { textToAction(it, conversation) } and parsedRepActions.mapNotNull { textToAction(it, conversation) }
        val actions = parsedActions.mapNotNull { textToAction(it, conversation) } + parsedRepActions.mapNotNull { textToRepAction(it, conversation) }
        return actions
    }

    override fun transformMessage(message: String, conversation: VillagerConversation): String {
        return message.replace(actionRegex, "").trim()
    }

    private fun getActions(message: String): Set<String> {
        val matches = actionRegex.findAll(message)
        return matches.map { it.groupValues[1] }.toSet()
    }

    private fun getRepActions(message: String): Set<String> {
        val matches = repRegex.findAll(message)
        return matches.map { it.groupValues[1] }.toSet()
    }

    private fun textToAction(text: String, conversation: VillagerConversation): ConversationMessageAction? {
        return when (text) {
            "SHAKE_HEAD" -> ShakeHeadAction(conversation.villager)
            "SOUND_YES" -> PlaySoundAction(conversation.player, conversation.villager, villagerSound("entity.villager.yes"))
            "SOUND_NO" -> PlaySoundAction(conversation.player, conversation.villager, villagerSound("entity.villager.no"))
            "SOUND_AMBIENT" -> PlaySoundAction(conversation.player, conversation.villager, villagerSound("entity.villager.ambient"))
            "END_CONVO" -> EndConversationAction(conversation, plugin)
            "CALL_GUARDS" -> CallGuardsAction(conversation, plugin)
            else -> null
        }
    }
    private fun textToRepAction(actionString: String, conversation: VillagerConversation): ConversationMessageAction? {
        val actionDetails = actionString.split(":")
        if (actionDetails.size == 3) {
            val actionType = actionDetails[0] // "INCREASE_REP" or "DECREASE_REP"
            val type = actionDetails[1]
            val amount = actionDetails[2].toIntOrNull() ?: return null // Safely convert to Int, return null if not possible

            return when (actionType) {
                "INCREASE_REP" -> IncreaseRepAction(conversation, type, amount)
                "DECREASE_REP" -> DecreaseRepAction(conversation, type, amount)
                else -> null // This case should never happen given the current setup
            }
        }
        return null // Return null if the actionString does not conform to the expected format
    }

    private fun villagerSound(key: String): Sound {
        return Sound.sound(Key.key(key), Sound.Source.NEUTRAL, 1f, 1f)
    }
}
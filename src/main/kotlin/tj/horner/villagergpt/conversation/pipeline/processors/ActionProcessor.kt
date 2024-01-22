package tj.horner.villagergpt.conversation.pipeline.processors

import net.citizensnpcs.api.npc.NPC
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import tj.horner.villagergpt.VillagerGPT
import tj.horner.villagergpt.conversation.NPCConversation
import tj.horner.villagergpt.conversation.NPCGlobalConversation
import tj.horner.villagergpt.conversation.pipeline.ConversationMessageAction
import tj.horner.villagergpt.conversation.pipeline.ConversationMessageProcessor
import tj.horner.villagergpt.conversation.pipeline.ConversationMessageTransformer
import tj.horner.villagergpt.conversation.pipeline.actions.*

class ActionProcessor(private val plugin: VillagerGPT)  : ConversationMessageProcessor, ConversationMessageTransformer {
    private val actionRegex = Regex("ACTION:([A-Z_]+)")

    override fun processMessage(
        message: String,
        conversation: NPCConversation
    ): Collection<ConversationMessageAction> {
        val parsedActions = getActions(message)
        return parsedActions.mapNotNull { textToAction(it, conversation) }
    }

    override fun processMessage(
            message: String,
            conversation: NPCGlobalConversation, npc : NPC
    ): Collection<ConversationMessageAction> {
        val parsedActions = getActions(message)
        return parsedActions.mapNotNull { textToAction(it, conversation, npc) }
    }

    override fun transformMessage(message: String, conversation: NPCConversation): String {
        return message.replace(actionRegex, "").trim()
    }

    override fun transformMessage(message: String, conversation: NPCGlobalConversation): String {
        return message.replace(actionRegex, "").trim()
    }

    private fun getActions(message: String): Set<String> {
        val matches = actionRegex.findAll(message)
        return matches.map { it.groupValues[1] }.toSet()
    }

    private fun textToAction(text: String, conversation: NPCConversation): ConversationMessageAction? {
        return when (text) {
//            "SHAKE_HEAD" -> ShakeHeadAction(conversation.npc.entity)
            "SOUND_YES" -> PlaySoundAction(conversation.player, conversation.npc.entity, villagerSound("entity.villager.yes"))
            "SOUND_NO" -> PlaySoundAction(conversation.player, conversation.npc.entity, villagerSound("entity.villager.no"))
            "SOUND_AMBIENT" -> PlaySoundAction(conversation.player, conversation.npc.entity, villagerSound("entity.villager.ambient"))
            "END_CONVO" -> EndConversationAction(conversation, plugin)
            "CALL_GUARDS" -> CallGuardsAction(conversation, plugin)
            else -> null
        }
    }

    private fun textToAction(text: String, conversation: NPCGlobalConversation, npc : NPC): ConversationMessageAction? {
        return when (text) {
            "DECLINE" -> PerformTradeAction(npc, "decline")
            "ACCEPT" -> PerformTradeAction(npc, "accept")
            "CANCEL" -> PerformTradeAction(npc, "cancel")
            "DM" -> Bukkit.getPlayer("CrashCringle12")?.let { SendPlayerMessageAction(it, Component.text(text)) }
            else -> null
        }
    }

    private fun villagerSound(key: String): Sound {
        return Sound.sound(Key.key(key), Sound.Source.NEUTRAL, 1f, 1f)
    }
}
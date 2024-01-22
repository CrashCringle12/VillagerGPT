package tj.horner.villagergpt.conversation.pipeline

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import net.citizensnpcs.api.npc.NPC
import tj.horner.villagergpt.conversation.NPCConversation
import tj.horner.villagergpt.conversation.NPCGlobalConversation

/**
 * A pipeline for producing and processing messages for a VillagerConversation.
 *
 * Given a player's message and the existing conversation, the message producer
 * will provide the next message in the sequence, which is then passed to the
 * message processors in the order they were defined. The message is processed
 * then transformed for each processor in the pipeline.
 *
 * The output is a set of actions to be performed based on what the processors
 * provided.
 */
class MessageProcessorPipeline(
    private val producer: ConversationMessageProducer,
    private val processors: List<ConversationMessageProcessor>
) {
    @OptIn(BetaOpenAI::class)
    suspend fun run(playerMessage: String, conversation: NPCConversation): Iterable<ConversationMessageAction> {
        var convo = conversation

        convo.addMessage(ChatMessage(
            role = ChatRole.User,
            content = playerMessage
        ))

        val nextMessage: String
        try {
            nextMessage = producer.produceNextMessage(convo)
        } catch(e: Exception) {
            convo.removeLastMessage()
            throw(e)
        }

        val actions = mutableListOf<ConversationMessageAction>()
        var transformedMessage = nextMessage
        processors.forEach {
            val resultActions = it.processMessage(transformedMessage, convo)
            if (resultActions != null) actions.addAll(resultActions)

            if (it is ConversationMessageTransformer) {
                val transformer = it as ConversationMessageTransformer
                transformedMessage = transformer.transformMessage(transformedMessage, convo)
            }
        }

        convo.addMessage(ChatMessage(
            role = ChatRole.Assistant,
            content = nextMessage
        ))
        return actions
    }

    @OptIn(BetaOpenAI::class)
    suspend fun run(playerMessage: String, conversation: NPCGlobalConversation, npc : NPC): Iterable<ConversationMessageAction> {
        var convo = conversation

        convo.addMessage(ChatMessage(
                role = ChatRole.User,
                content = playerMessage
        ), npc)
        val nextMessage: String
        try {
            nextMessage = producer.produceNextMessage(convo, npc)
        } catch(e: Exception) {
            convo.removeLastMessage(npc.uniqueId)
            throw(e)
        }

        val actions = mutableListOf<ConversationMessageAction>()
        var transformedMessage = nextMessage
        processors.forEach {
            val resultActions = it.processMessage(transformedMessage, convo, npc)
            if (resultActions != null) actions.addAll(resultActions)

            if (it is ConversationMessageTransformer) {
                val transformer = it as ConversationMessageTransformer
                transformedMessage = transformer.transformMessage(transformedMessage, convo)
            }
        }

        convo.addMessage(ChatMessage(
                role = ChatRole.Assistant,
                content = nextMessage
        ), npc)
        return actions
    }
}
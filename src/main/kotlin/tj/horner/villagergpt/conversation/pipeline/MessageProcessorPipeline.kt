package tj.horner.villagergpt.conversation.pipeline

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import tj.horner.villagergpt.VillagerGPT
import tj.horner.villagergpt.conversation.VillagerConversation
import java.io.File

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
    suspend fun run(playerMessage: String, conversation: VillagerConversation): Iterable<ConversationMessageAction> {
        conversation.addMessage(ChatMessage(
            role = ChatRole.User,
            content = playerMessage
        ))

        val nextMessage: String
        try {
            nextMessage = producer.produceNextMessage(conversation)
        } catch(e: Exception) {
            conversation.removeLastMessage()
            throw(e)
        }

        val actions = mutableListOf<ConversationMessageAction>()
        var transformedMessage = nextMessage
        processors.forEach {
            val resultActions = it.processMessage(transformedMessage, conversation)
            if (resultActions != null) actions.addAll(resultActions)

            if (it is ConversationMessageTransformer) {
                val transformer = it as ConversationMessageTransformer
                transformedMessage = transformer.transformMessage(transformedMessage, conversation)
            }
        }

        conversation.addMessage(ChatMessage(
            role = ChatRole.Assistant,
            content = nextMessage
        ))

        return actions
    }

    @OptIn(BetaOpenAI::class)
    suspend fun run(playerMessage: String, conversation: VillagerConversation, plugin: VillagerGPT): Iterable<ConversationMessageAction> {
        conversation.addSummaryMessage(ChatMessage(
                role = ChatRole.System,
                content = playerMessage
        ))

        val nextMessage: String
        try {
            nextMessage = producer.produceNextMessage(conversation)
        } catch(e: Exception) {
            conversation.removeLastMessage()
            throw(e)
        }
        // Write this message to a file called "conversation.txt" in the plugin's data folder
        // This is a simple way to store the conversation for later use
        try {
            val conversationFile = File(plugin.dataFolder, "${conversation.player.name}-${conversation.villager.name}-conversation.txt")
            plugin.logger.info("Writing conversation to file: ${conversationFile.absolutePath}")
            conversationFile.writeText(conversation.messages.joinToString("\n"))
            // Write summary file to the same directory
            val summaryFile = File(plugin.dataFolder, "${conversation.player.name}-${conversation.villager.name}-summary.txt")
            plugin.logger.info("Writing conversation summary to file: ${summaryFile.absolutePath}")
            plugin.logger.info("Conversation summary: $nextMessage")
            summaryFile.writeText(nextMessage)
        } catch (e: Exception) {
            plugin.logger.warning("Failed to write conversation to file: ${e.message}")
        }

        val actions = mutableListOf<ConversationMessageAction>()
        var transformedMessage = nextMessage
        processors.forEach {
            val resultActions = it.processMessage(transformedMessage, conversation)
            if (resultActions != null) actions.addAll(resultActions)

            if (it is ConversationMessageTransformer) {
                val transformer = it as ConversationMessageTransformer
                transformedMessage = transformer.transformMessage(transformedMessage, conversation)
            }
        }

        conversation.addSummaryMessage(ChatMessage(
                role = ChatRole.Assistant,
                content = nextMessage
        ))

        return actions
    }
}
package tj.horner.villagergpt.conversation.pipeline.producers

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import net.citizensnpcs.api.npc.NPC
import org.bukkit.configuration.Configuration
import tj.horner.villagergpt.conversation.NPCConversation
import tj.horner.villagergpt.conversation.NPCGlobalConversation
import tj.horner.villagergpt.conversation.pipeline.ConversationMessageProducer

class OpenAIMessageProducer(config: Configuration) : ConversationMessageProducer {
    private val openAI = OpenAI(
        OpenAIConfig(
            config.getString("openai-key")!!,
            LoggingConfig(
                LogLevel.None
            )
        )
    )

    private val model = ModelId("gpt-3.5-turbo-0125")

    @OptIn(BetaOpenAI::class)
    override suspend fun produceNextMessage(conversation: NPCConversation): String {
        val request = ChatCompletionRequest(
            model = model,
            messages = conversation.messages,
            temperature = 0.5,
            user = conversation.player.uniqueId.toString()
        )

        val completion = openAI.chatCompletion(request)
        return completion.choices[0].message.content!!
    }

    @OptIn(BetaOpenAI::class)
    override suspend fun produceNextMessage(conversation: NPCGlobalConversation, npc : NPC): String {
        val request = ChatCompletionRequest(
                model = model,
                messages = conversation.npcMessages[npc.uniqueId]!!,
                temperature = 0.1,
                user = npc.uniqueId.toString()
        )


        val completion = openAI.chatCompletion(request)
        return completion.choices[0].message.content!!
    }
}
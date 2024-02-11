package tj.horner.villagergpt

import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.github.shynixn.mccoroutine.bukkit.setSuspendingExecutor
import crashcringle.malmoserverplugin.MalmoServerPlugin
import tj.horner.npcgpt.conversation.NPCConversationManager
import tj.horner.villagergpt.commands.ClearCommand
import tj.horner.villagergpt.commands.EndCommand
import tj.horner.villagergpt.commands.TalkCommand
import tj.horner.villagergpt.conversation.pipeline.MessageProcessorPipeline
import tj.horner.villagergpt.conversation.pipeline.processors.ActionProcessor
import tj.horner.villagergpt.conversation.pipeline.processors.TradeOfferProcessor
import tj.horner.villagergpt.conversation.pipeline.producers.OpenAIMessageProducer
import tj.horner.villagergpt.handlers.ConversationEventsHandler
import tj.horner.villagergpt.tasks.EndStaleConversationsTask
import java.util.logging.Level

class VillagerGPT : SuspendingJavaPlugin() {
    val conversationManager = NPCConversationManager(this)
    val messagePipeline = MessageProcessorPipeline(
        OpenAIMessageProducer(config),
        listOf(
            ActionProcessor(this),
            TradeOfferProcessor(logger)
        )
    )

    override suspend fun onEnableAsync() {
        saveDefaultConfig()

        if (!validateConfig()) {
            logger.log(Level.WARNING, "VillagerGPT has not been configured correctly! Please set the `openai-key` in config.yml.")
            return
        }
        setCommandExecutors()
        registerEvents()
        scheduleTasks()
    }

    override fun onDisable() {
        logger.info("Ending all conversations")
        conversationManager.endAllConversations()
        conversationManager.endGlobalConversation()
    }

    private fun setCommandExecutors() {
        getCommand("ttv-"+PLNAME.lowercase())!!.setSuspendingExecutor(TalkCommand(this))
        getCommand("ttvclear-"+PLNAME.lowercase())!!.setSuspendingExecutor(ClearCommand(this))
        getCommand("ttvend-"+PLNAME.lowercase())!!.setSuspendingExecutor(EndCommand(this))
    }

    private fun registerEvents() {
        server.pluginManager.registerSuspendingEvents(ConversationEventsHandler(this), this)
    }

    private fun scheduleTasks() {
        EndStaleConversationsTask(this).runTaskTimer(this, 0L, 200L)
    }

    private fun validateConfig(): Boolean {
        val openAiKey = config.getString("openai-key") ?: return false
        return openAiKey.trim() != ""
    }

    companion object {
        const val PLNAME = "BOBBY"
        const val ALTNAME = "JOHN"
        const val PROFESSION_ = "Blacksmith"
    }
}

package tj.horner.villagergpt.handlers

import com.aallam.openai.api.BetaOpenAI
import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import io.papermc.paper.event.player.AsyncChatEvent
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Villager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.persistence.PersistentDataType
import tj.horner.villagergpt.MetadataKey
import tj.horner.villagergpt.VillagerGPT
import tj.horner.villagergpt.chat.ChatMessageTemplate
import tj.horner.villagergpt.conversation.formatting.MessageFormatter
import tj.horner.villagergpt.events.VillagerConversationEndEvent
import tj.horner.villagergpt.events.VillagerConversationMessageEvent
import tj.horner.villagergpt.events.VillagerConversationStartEvent
import tj.horner.villagergpt.events.VillagerConversationSummarizeEvent

class ConversationEventsHandler(private val plugin: VillagerGPT) : Listener {
    @EventHandler
    fun onConversationStart(evt: VillagerConversationStartEvent) {
        val message = Component.text("You are now in a conversation with ")
            .append(evt.conversation.villager.name().color(NamedTextColor.AQUA))
            .append(Component.text(". Send a chat message to get started and use /ttvend to end it"))
            .decorate(TextDecoration.ITALIC)

        evt.conversation.player.sendMessage(ChatMessageTemplate.withPluginNamePrefix(message))

        evt.conversation.villager.isAware = false
        evt.conversation.villager.lookAt(evt.conversation.player)

        plugin.logger.info("Conversation started between ${evt.conversation.player.name} and ${evt.conversation.villager.name}")
    }

    @EventHandler
    fun onConversationEnd(evt: VillagerConversationEndEvent) {
        val message = Component.text("Your conversation with ")
            .append(evt.villager.name().color(NamedTextColor.AQUA))
            .append(Component.text(" has ended"))
            .decorate(TextDecoration.ITALIC)

        evt.player.sendMessage(ChatMessageTemplate.withPluginNamePrefix(message))

        evt.villager.resetOffers()
        evt.villager.isAware = true

        plugin.logger.info("Conversation ended between ${evt.player.name} and ${evt.villager.name}")

    }

    @EventHandler
    fun onVillagerInteracted(evt: PlayerInteractEntityEvent) {
        if (evt.rightClicked !is Villager) return
        
        var metaplugin = Bukkit.getPluginManager().getPlugin("EliteMobs")
        if (metaplugin == null) {
            return
        }
        try {
            if (evt.player.location.world.name.contains("em_")) {
                return
            }
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
        val villager = evt.rightClicked as Villager
        if (!villager.persistentDataContainer.has(NamespacedKey(metaplugin, "VILLAGER_GPT"), PersistentDataType.STRING)) {
            if (evt.player.hasMetadata(MetadataKey.SelectingVillager)) {
                evt.player.sendMessage("This villager does not possess the knowledge of Old")
                return
            } else {
                return
            }
        }
        // Villager is in a conversation with another player
        val existingConversation = plugin.conversationManager.getConversation(villager)
        if (existingConversation != null && existingConversation.player.uniqueId != evt.player.uniqueId) {
            val message = Component.text("This villager is in a conversation with ")
                .append(existingConversation.player.displayName())
                .decorate(TextDecoration.ITALIC)
            evt.player.sendMessage(ChatMessageTemplate.withPluginNamePrefix(message))
            evt.isCancelled = true
            return
        }

        if (!evt.player.hasMetadata(MetadataKey.SelectingVillager)) {
            evt.player.sendMessage("You must run /ttv to talk to " + villager.name)
            return
        }


        // Player is selecting a villager for conversation
        evt.isCancelled = true

        if (villager.profession == Villager.Profession.NONE) {
            val message = Component.text("You can only speak to villagers with a profession")
                .decorate(TextDecoration.ITALIC)

            evt.player.sendMessage(ChatMessageTemplate.withPluginNamePrefix(message))
            return
        }

        plugin.conversationManager.startConversation(evt.player, villager)
        evt.player.removeMetadata(MetadataKey.SelectingVillager, plugin)
    }

    @EventHandler(priority = EventPriority.NORMAL)
    suspend fun onSendMessage(evt: AsyncChatEvent) {
        val conversation = plugin.conversationManager.getConversation(evt.player) ?: return
        evt.isCancelled = true
        evt.viewers().clear()
        


        if (conversation.pendingResponse) {
            val message = Component.text("Please wait for ")
                .append(conversation.villager.name().color(NamedTextColor.AQUA))
                .append(Component.text(" to respond"))
                .decorate(TextDecoration.ITALIC)

            evt.player.sendMessage(ChatMessageTemplate.withPluginNamePrefix(message))
            return
        }

        conversation.pendingResponse = true
        val villager = conversation.villager

        try {
            val pipeline = plugin.messagePipeline

            val playerMessage = PlainTextComponentSerializer.plainText().serialize(evt.originalMessage())
            val formattedPlayerMessage = MessageFormatter.formatMessageFromPlayer(Component.text(playerMessage), villager)

            evt.player.sendMessage(formattedPlayerMessage)

            val actions = pipeline.run(playerMessage, conversation)
            if (!conversation.ended) {
                withContext(plugin.minecraftDispatcher) {
                    actions.forEach { it.run() }
                }
            }
        } catch(e: Exception) {
            val message = Component.text("Something went wrong while getting ")
                .append(villager.name().color(NamedTextColor.AQUA))
                .append(Component.text("'s response. Please try again"))
                .decorate(TextDecoration.ITALIC)

            evt.player.sendMessage(ChatMessageTemplate.withPluginNamePrefix(message))
            throw(e)
        } finally {
            conversation.pendingResponse = false
        }
    }

    @OptIn(BetaOpenAI::class)
    @EventHandler
    fun onConversationMessage(evt: VillagerConversationMessageEvent) {
        if (!plugin.config.getBoolean("log-conversations")) return
        plugin.logger.info("Message between ${evt.conversation.player.name} and ${evt.conversation.villager.name}: ${evt.message}")
    }

    @EventHandler(priority = EventPriority.NORMAL)
    suspend fun onSummarize(evt: VillagerConversationSummarizeEvent) {
        val conversation = plugin.conversationManager.getConversation(evt.player) ?: return
        val villager = conversation.villager
        try {
            val pipeline = plugin.messagePipeline
            val summarizePrompt = "Summarize the conversation between you and ${evt.player} as concisely as possible. This will be sent to you if you initiate a conversation with them again."
            val actions = pipeline.run(summarizePrompt, conversation, plugin)
            if (!conversation.ended) {
                withContext(plugin.minecraftDispatcher) {
                    actions.forEach { it.run() }
                }
            }
        } catch(e: Exception) {
            val message = Component.text("Something went wrong while getting ")
                    .append(villager.name().color(NamedTextColor.AQUA))
                    .append(Component.text("'s response. Please try again"))
                    .decorate(TextDecoration.ITALIC)

            evt.player.sendMessage(ChatMessageTemplate.withPluginNamePrefix(message))
            throw(e)
        } finally {
            conversation.pendingResponse = false
        }
    }

    @EventHandler
    fun onVillagerDied(evt: EntityDeathEvent) {
        if (evt.entity !is Villager) return
        val villager = evt.entity as Villager

        val conversation = plugin.conversationManager.getConversation(villager)
        if (conversation != null) {
            plugin.conversationManager.endConversation(conversation)
        }
    }

}
package tj.horner.villagergpt.handlers

import com.aallam.openai.api.BetaOpenAI
import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import io.papermc.paper.event.player.AsyncChatEvent
import kotlinx.coroutines.withContext
import net.citizensnpcs.api.event.NPCRightClickEvent
import net.citizensnpcs.api.npc.NPC
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerJoinEvent
import tj.horner.villagergpt.MetadataKey
import tj.horner.villagergpt.VillagerGPT
import tj.horner.villagergpt.chat.ChatMessageTemplate
import tj.horner.villagergpt.conversation.formatting.MessageFormatter
import tj.horner.villagergpt.events.*

class ConversationEventsHandler(private val plugin: VillagerGPT) : Listener {
    @EventHandler
    fun onConversationStart(evt: NPCConversationStartEvent) {
        val message = Component.text("You are now in a conversation with ")
            .append(evt.conversation.npc.entity.name().color(NamedTextColor.AQUA))
            .append(Component.text(". Send a chat message to get started and use /ttvend to end it"))
            .decorate(TextDecoration.ITALIC)

        evt.conversation.player.sendMessage(ChatMessageTemplate.withPluginNamePrefix(message))

       // evt.conversation.npc.entity.isAware = false

        plugin.logger.info("Conversation started between ${evt.conversation.player.name} and ${evt.conversation.npc.name}")
    }

    @EventHandler
    fun onJoin(evt : PlayerJoinEvent) {
        val player = evt.player
        // Check if player is in the global conversation
        if (plugin.conversationManager.getGlobalConversation() == null) return
        val globalConversation = plugin.conversationManager.getGlobalConversation()
        val players = globalConversation!!.players
        if (!players.contains(player)) {
            val message = Component.text("You are now in a conversation with ")
                    .append(Component.text("Global").color(NamedTextColor.DARK_GREEN))
                    .append(Component.text(". Send a chat message to get started and use /ttvend to end it"))
                    .decorate(TextDecoration.ITALIC)
            player.sendMessage(ChatMessageTemplate.withPluginNamePrefix(message))
        }
    }


    @EventHandler
    fun onConversationEnd(evt: NPCConversationEndEvent) {
        val message = Component.text("Your conversation with ")
            .append(evt.npc.entity.name().color(NamedTextColor.AQUA))
            .append(Component.text(" has ended"))
            .decorate(TextDecoration.ITALIC)

        evt.player.sendMessage(ChatMessageTemplate.withPluginNamePrefix(message))

       // evt.npc.resetOffers()
        //evt.npc.isAware = true

        plugin.logger.info("Conversation ended between ${evt.player.name} and ${evt.npc.name}")
    }

    @EventHandler
    fun onVillagerInteracted(evt: NPCRightClickEvent) {
        System.out.println("Villager Interacted");
        System.out.print("We love to party.")
        val npc = evt.npc as NPC
        plugin.conversationManager.startGlobalConversation()
        plugin.logger.info("Global conversation started")
        // Npc is in a conversation with another player
        val existingConversation = plugin.conversationManager.getConversation(npc)
        if (existingConversation != null && existingConversation.player.uniqueId != evt.clicker.uniqueId) {
            val message = Component.text("This npc is in a conversation with ")
                .append(existingConversation.player.displayName())
                .decorate(TextDecoration.ITALIC)

            evt.clicker.sendMessage(ChatMessageTemplate.withPluginNamePrefix(message))
            evt.isCancelled = true
            return
        }

        if (!evt.clicker.hasMetadata(MetadataKey.SelectingNpc)) return

        // Player is selecting a villager for conversation
        evt.isCancelled = true

        plugin.conversationManager.startConversation(evt.clicker, npc)
        evt.clicker.removeMetadata(MetadataKey.SelectingNpc, plugin)
    }

    @EventHandler
    suspend fun onSendMessage(evt: AsyncChatEvent) {

        val conversation = plugin.conversationManager.getConversation(evt.player)
        if (conversation != null) {
            evt.isCancelled = true
            if (conversation.pendingResponse) {
                val message = Component.text("Please wait for ")
                        .append(conversation.npc.entity.name().color(NamedTextColor.AQUA))
                        .append(Component.text(" to respond"))
                        .decorate(TextDecoration.ITALIC)

                evt.player.sendMessage(ChatMessageTemplate.withPluginNamePrefix(message))
                return
            }

            conversation.pendingResponse = true
            val npc = conversation.npc

            try {
                val pipeline = plugin.messagePipeline

                val playerMessage = PlainTextComponentSerializer.plainText().serialize(evt.originalMessage())
                val formattedPlayerMessage =
                        MessageFormatter.formatMessageFromPlayer(Component.text(playerMessage), npc)

                evt.player.sendMessage(formattedPlayerMessage)

                val actions = pipeline.run(playerMessage, conversation)
                if (!conversation.ended) {
                    withContext(plugin.minecraftDispatcher) {
                        actions.forEach { it.run() }
                    }
                }
            } catch (e: Exception) {
                val message = Component.text("Something went wrong while getting ")
                        .append(npc.entity.name().color(NamedTextColor.AQUA))
                        .append(Component.text("'s response. Please try again"))
                        .decorate(TextDecoration.ITALIC)

                evt.player.sendMessage(ChatMessageTemplate.withPluginNamePrefix(message))
                throw (e)
            } finally {
                conversation.pendingResponse = false
            }
        } else {
            val globalConversation = plugin.conversationManager.getGlobalConversation()
            if (globalConversation != null) {
                if (evt.player.name == globalConversation.npc.name) {
                    plugin.logger.info("Player ${evt.player.name} is equal to ${globalConversation.npc.name} ")
                    return;
                } else {
                    plugin.logger.info("Player ${evt.player.name} is not equal to ${globalConversation.npc.name} ")
                }
                evt.isCancelled = true
                if (globalConversation.pendingResponse) {
                    val message = Component.text("Please wait for ")
                            .append(globalConversation.npc.entity.name().color(NamedTextColor.AQUA))
                            .append(Component.text(" to respond"))
                            .decorate(TextDecoration.ITALIC)
                    evt.player.sendMessage(ChatMessageTemplate.withPluginNamePrefix(message))
                    return
                }
                globalConversation.pendingResponse = true
                val npc = globalConversation.npc
                try {

                    val pipeline = plugin.messagePipeline

                    val playerMessage = PlainTextComponentSerializer.plainText().serialize(evt.originalMessage())
                    val formattedPlayerMessage = MessageFormatter.formatMessageFromGlobal(Component.text(playerMessage), evt.player.name)

                   // evt.player.sendMessage(formattedPlayerMessage)
                    evt.viewers().forEach { it.sendMessage(formattedPlayerMessage) }

                    val actions = pipeline.run(playerMessage, globalConversation, npc)
                    if (!globalConversation.ended) {
                        withContext(plugin.minecraftDispatcher) {
                            actions.forEach { it.run() }
                        }
                    }


                } catch (e: Exception) {
                    val message = Component.text("Something went wrong while getting ")
                            .append(Component.text("Global").color(NamedTextColor.DARK_GREEN))
                            .append(Component.text("'s response. Please try again"))
                            .decorate(TextDecoration.ITALIC)

                    Bukkit.broadcast(ChatMessageTemplate.withPluginNamePrefix(message))
                    throw (e)
                }
            }
        }

    }

    @OptIn(BetaOpenAI::class)
    @EventHandler
    fun onConversationMessage(evt: NPCConversationMessageEvent) {
        if (!plugin.config.getBoolean("log-conversations")) return
        plugin.logger.info("Message between ${evt.conversation.player.name} and ${evt.conversation.npc.name}: ${evt.message}")
    }

    @OptIn(BetaOpenAI::class)
    @EventHandler
    fun onGlobalConversationMessage(evt: NPCGlobalConversationMessageEvent) {
        if (!plugin.config.getBoolean("log-conversations")) return
        plugin.logger.info("Global Message ${evt.message.name} : ${evt.message}")
    }

    @EventHandler
    suspend fun onGlobalConversationResponse(evt: NPCGlobalConversationResponseEvent) {
        if (!plugin.config.getBoolean("log-conversations")) return
      //  plugin.logger.info("Global Message ${evt.npc.name} : ${PlainTextComponentSerializer.plainText().serialize(evt.message)}")
        val globalConversation = plugin.conversationManager.getGlobalConversation()
        if (globalConversation != null) {
            try {
                val pipeline = plugin.messagePipeline
                val playerMessage = PlainTextComponentSerializer.plainText().serialize(evt.message)
                val npcEntity = evt.npc.entity as Player
                if (playerMessage.toString() == "" || playerMessage.toString() == " ") {
                    return
                }
                if (playerMessage.toString().contains("ACTION:PASS") || playerMessage.contains("ACTION: PASS")) {
                    return
                }
                val chat = playerMessage
//                        """{
//                "message": $playerMessage,
//                "playerInfo": {
//                    "name": ${evt.npc.name},
//                    "itemInHand": ${npcEntity.itemInHand.type.name},
//                    }"
//                 }"""
                withContext(plugin.minecraftDispatcher) {
                    globalConversation.npcs.forEach {
                        if (it.uniqueId != evt.npc.uniqueId || evt.npc.name != it.name) {

                            val actions = pipeline.run(chat, globalConversation, it)
                            if (!globalConversation.npcEnded[it.uniqueId]!!) {
                                withContext(plugin.minecraftDispatcher) {
                                    actions.forEach { it.run() }
                                }
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                val message = Component.text("Something went wrong while getting ")
                        .append(Component.text("Global").color(NamedTextColor.DARK_GREEN))
                        .append(Component.text("'s response. Please try again"))
                        .decorate(TextDecoration.ITALIC)

                Bukkit.broadcast(ChatMessageTemplate.withPluginNamePrefix(message))
                throw (e)
            }
        }
    }

    @EventHandler
    fun onVillagerDied(evt: EntityDeathEvent) {
        if (evt.entity !is NPC) return
        val npc = evt.entity as NPC
        val conversation = plugin.conversationManager.getConversation(npc)
        if (conversation != null) {
            plugin.conversationManager.endConversation(conversation)
        }
    }
}
package tj.horner.villagergpt.conversation.pipeline.processors

import com.google.gson.Gson
import crashcringle.malmoserverplugin.api.TradeRequestEvent
import crashcringle.malmoserverplugin.barterkings.BarterKings
import crashcringle.malmoserverplugin.barterkings.trades.Trade
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.MerchantRecipe
import tj.horner.villagergpt.conversation.NPCConversation
import tj.horner.villagergpt.conversation.formatting.MessageFormatter
import tj.horner.villagergpt.conversation.pipeline.ConversationMessageAction
import tj.horner.villagergpt.conversation.pipeline.ConversationMessageProcessor
import tj.horner.villagergpt.conversation.pipeline.actions.SendPlayerMessageAction
import tj.horner.villagergpt.conversation.pipeline.actions.SetTradesAction
import java.util.logging.Level
import java.util.logging.Logger
import crashcringle.malmoserverplugin.barterkings.trades.TradeController;
import crashcringle.malmoserverplugin.barterkings.trades.TradeRequest
import net.citizensnpcs.api.npc.NPC
import org.bukkit.entity.Player
import org.hibernate.internal.util.collections.CollectionHelper.listOf
import tj.horner.villagergpt.conversation.NPCGlobalConversation
import tj.horner.villagergpt.conversation.pipeline.actions.SendGlobalMessageAction
import tj.horner.villagergpt.conversation.pipeline.actions.SendNPCMessageAction

class TradeOfferProcessor(private val logger: Logger) : ConversationMessageProcessor {
    private val gson = Gson()
    private val itemFactory = Bukkit.getServer().itemFactory

    override fun processMessage(message: String, conversation: NPCConversation): Collection<ConversationMessageAction> {
        val tradeExpressionRegex = Regex("TRADE(\\[.+?\\])ENDTRADE")
        val splitMessage = splitWithMatches(message, tradeExpressionRegex)

        val trades = mutableListOf<Trade>()

        val messageComponent = Component.text().content("")

        splitMessage.forEach {
            if (it.trim().startsWith("TRADE")) {
                val response = it.trim().replace(Regex("(^TRADE)|(ENDTRADE$)"), "")

                try {
                    val trade = parseNPCTradeResponse(response, conversation.npc)
                    trades.add(trade)

                    val tradeMessage = chatFormattedRecipe(trade)
                    messageComponent.append(tradeMessage)
                } catch(e: Exception) {
                    logger.log(Level.WARNING, "Chat response contained invalid trade: $response", e)
                    messageComponent.append(invalidTradeComponent(response))
                }
            } else {
                messageComponent.append(Component.text(it).color(NamedTextColor.WHITE))
            }
        }

        val formattedMessage = MessageFormatter.formatMessageFromNPC(messageComponent.build(), conversation.npc)
        return listOf(
            SetTradesAction(conversation.npc, trades),
            SendPlayerMessageAction(conversation.player, formattedMessage)
        )
    }

    override fun processMessage(message: String, conversation: NPCGlobalConversation, npc : NPC): Collection<ConversationMessageAction> {
        val tradeExpressionRegex = Regex("TRADE(\\[.+?\\])ENDTRADE")
        val splitMessage = splitWithMatches(message, tradeExpressionRegex)

        val trades = mutableListOf<Trade>()

        val messageComponent = Component.text().content("")

        splitMessage.forEach {
            if (it.trim().startsWith("TRADE")) {
                val response = it.trim().replace(Regex("(^TRADE)|(ENDTRADE$)"), "")

                try {
                    val trade = parseNPCTradeResponse(response, npc)
                    trades.add(trade)

                    val tradeMessage = chatFormattedRecipe(trade)
                    messageComponent.append(tradeMessage)
                } catch(e: Exception) {
                    logger.log(Level.WARNING, "Chat response contained invalid trade: $response", e)
                    messageComponent.append(invalidTradeComponent(response))
                }
            } else {
                messageComponent.append(Component.text(it).color(NamedTextColor.WHITE))
            }
        }

        val formattedMessage = MessageFormatter.formatMessageFromGlobal(messageComponent.build(), npc.name)
        return listOf(
                SetTradesAction(npc, trades),
                SendGlobalMessageAction(formattedMessage),
                SendNPCMessageAction(npc, conversation, formattedMessage)
        )
    }

    private fun parseVillagerTradeResponse(text: String): MerchantRecipe {
        val tradeList = gson.fromJson(text, arrayListOf(arrayListOf<String>()).javaClass)

        val ingredients = tradeList[0].map { parseItemStack(it) }
        val results = tradeList[1].map { parseItemStack(it) }
        val recipe = MerchantRecipe(results[0], 1)
        recipe.ingredients = ingredients

        return recipe
    }

    private fun parseNPCTradeResponse(text: String, npc: NPC): Trade {
        // Log the text
        logger.info("Trade response: $text")
        val tradeList = gson.fromJson(text, arrayListOf(arrayListOf<String>()).javaClass)
        val player = tradeList[0][0]
        val ingredients = tradeList[1].map { parseItemStack(it) }
        val results = tradeList[2].map { parseItemStack(it) }
        var trade = Trade(ingredients[0], results[0]);
        // Cast npc.entity to Player to get the player's UUID
        if (npc.entity is Player) {
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "barter admin-trade  ${npc.name} ${trade.offeredItems[0].type} ${trade.offeredItems[0].amount} ${trade.requestedItems[0].type} ${trade.requestedItems[0].amount} ${player}")
            logger.info("Command: barter admin-trade ${npc.name} ${trade.offeredItems[0].type} ${trade.offeredItems[0].amount} ${trade.requestedItems[0].type} ${trade.requestedItems[0].amount} ${player}")
            return trade;
        } else {
            throw Exception("NPC is not a player");
        }
    }


    private fun parseItemStack(text: String): ItemStack {
        val matches = Regex("([0-9]+) (.+)").find(text) ?: return ItemStack(Material.AIR)

        val (numItems, materialString) = matches.destructured
        val stack = itemFactory.createItemStack(materialString)
        stack.amount = numItems.toInt().coerceAtMost(64)

        return stack
    }

    private fun parsePlayer(text: String): String? {
        val matches = Regex("""TRADE\[\["([^"]+)"]""").find(text)?: return "CrashCringle12"
        val playerName = matches.groups[1]?.value
        return playerName
    }


    private fun chatFormattedRecipe(trade: Trade): TextComponent {
        val component = Component.text().content("is offering ")

        trade.offeredItems.forEachIndexed { index, it ->
            component.append(Component.text("${it.amount} ").color(NamedTextColor.LIGHT_PURPLE))
            component.append(it.displayName())

            if (index + 1 < trade.offeredItems.count())
                component.append(Component.text(" + "))
            else
                component.append(Component.text(" "))
        }

        component.append(Component.text("in exchange for "))
        trade.requestedItems.forEachIndexed { index, it ->
            component.append(Component.text("${it.amount} ").color(NamedTextColor.LIGHT_PURPLE))
            component.append(it.displayName())

            if (index + 1 < trade.requestedItems.count())
                component.append(Component.text(" + "))
            else
                component.append(Component.text(" "))
        }
        component.append(Component.text(". Use an ACTION to accept or decline."))
        component.color(NamedTextColor.DARK_GREEN)

        return component.build()
    }

    private fun invalidTradeComponent(rawTrade: String): Component {
        return Component.text()
            .content("[Invalid Trade]")
            .hoverEvent(HoverEvent.showText(Component.text("The response contained a recipe for an invalid trade. Here is the attempted recipe:\n\n$rawTrade")))
            .color(NamedTextColor.RED)
            .build()
    }

    private fun splitWithMatches(input: String, regex: Regex): List<String> {
        val result = mutableListOf<String>()
        var lastIndex = 0

        regex.findAll(input).forEach { match ->
            result.add(input.subSequence(lastIndex, match.range.first).toString())
            result.add(match.value)
            lastIndex = match.range.last + 1
        }

        if (lastIndex < input.length) {
            result.add(input.subSequence(lastIndex, input.length).toString())
        }

        return result
    }
}
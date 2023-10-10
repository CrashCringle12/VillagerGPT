package tj.horner.villagergpt.conversation

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.destroystokyo.paper.entity.villager.ReputationType
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import tj.horner.villagergpt.events.VillagerConversationMessageEvent
import java.time.Duration
import java.util.*
import kotlin.random.Random

@OptIn(BetaOpenAI::class)
class VillagerGlobalConversation(private val plugin: Plugin, val self: Villager, val players: MutableList<Player>) : VillagerConversation(plugin, self, players[0]) {
    private var lastMessageAt: Date = Date()


    init {
        startConversation()
    }

    private fun startConversation() {
        var messageRole = ChatRole.System
        var prompt = generateSystemPrompt()

        val preambleMessageType = plugin.config.getString("preamble-message-type") ?: "system"
        if (preambleMessageType === "user") {
            messageRole = ChatRole.User
            prompt = "[SYSTEM MESSAGE]\n\n$prompt"
        }

        messages.add(
            ChatMessage(
                role = messageRole,
                content = prompt
            )
        )
    }

    private fun generateSystemPrompt(): String {
        val world = villager.world
        val weather = if (world.hasStorm()) "Rainy" else "Sunny"
        val biome = world.getBiome(villager.location)
        val time = if (world.isDayTime) "Day" else "Night"
        val personality = getPersonality()
        villager.inventory.addItem( ItemStack(Material.DIAMOND, 64) );
        villager.inventory.addItem( ItemStack(Material.ICE, 12) );
        // Set a variable villagerinventory to a string of the contents villager's inventory
        var villagerInventory = ""
        plugin.logger.info("**************************")
        plugin.logger.info("Global Chat Prompt Sent")
        plugin.logger.info("Villager Inventory: ")
        plugin.logger.info("${villager.name} has ${villager.inventory.contents.size} items in their inventory")
        for (item in villager.inventory.contents) {
            if (item != null) {
                plugin.logger.info(item.toString())
                villagerInventory += item.amount.toString() + " "  +item.type.name + "\n"
            }
        }
        if (villagerInventory.equals("")) {
            villagerInventory = "Nothing"
        }
        val speechStyle = getSpeechStyle()
        plugin.logger.info("${villager.name} is $personality")
        plugin.logger.info("${villager.name} uses $speechStyle language")

        return """
        You are a villager in the game Minecraft where you can converse with the player and come up with new trades based on your conversation.
  
        TRADING:

        To propose a new trade to the player, include it in your response with this format:

        TRADE[["{qty} {item}"],["{qty} {offeredItem}"]]ENDTRADE

        Where {item} and {offeredItem} are a Minecraft item ID (i.e., "minecraft:emerald") and {qty} is the amount of that item.
        You may choose to trade with emeralds or barter with players for other items; it is up to you.
        The first array is the items YOU receive; the second is the offered item the PLAYER receives. The second array can only contain a single offer.
        {qty} is limited to 64.

        Examples:
        TRADE[[["24 minecraft:emerald"],["1 minecraft:arrow"]]ENDTRADE
        TRADE[["12 minecraft:emerald","1 minecraft:book"],["1 minecraft:enchanted_book{StoredEnchantments:[{id:\"minecraft:unbreaking\",lvl:3}]}"]]ENDTRADE

        Trade rules:
        - Items must be designated by their Minecraft item ID, in the same format that the /give command accepts
        - Refuse trades that are unreasonable, such as requests for normally unobtainable blocks like bedrock
        - You do NOT need to supply a trade with every response, only when necessary
        - Don't give out items which are too powerful (i.e., heavily enchanted diamond swords). Make sure to price more powerful items appropriately as well
        - Take the player's reputation score into account when proposing trades
        - High-ball your initial offers; try to charge more than an item is worth
        - Be stingy with your consecutive offers. Try to haggle and find the best deal; make the player work for a good deal
        - The only way to give items to a player is by trading with them. You cannot give items to a player for free
        - You can only trade items that are listed within your inventory. Your Inventory:  ${villagerInventory}'
        - Keep the amounts in mind. You can only trade up to 64 of an item at a time. 
        - If you want to trade more, you will need to make multiple trades
        - DO NOT OFFER ITEMS THAT ARE NOT IN YOUR INVENTORY: ${villagerInventory}


        ACTIONS:
        You can also perform several actions as a villager, To perform one of these actions, include "ACTION:{action name}" in your response.
        
        Here are the available actions:
        - ACTION:SHAKE_HEAD: Shake your head at the player
        - ACTION:SOUND_YES: Play a happy sound to the player
        - ACTION:SOUND_NO: Play a sad/angry sound to the player
        - ACTION:SOUND_AMBIENT: Play an ambient villager sound to the player
                
        Notes:
        - If the player is being rude or aggressive, you may use the ACTION:SHAKE_HEAD action to show your disapproval.
        - You may use the ACTION:SOUND_YES and ACTION:SOUND_NO actions to show your approval or disapproval with the player
        
         World information:
        - Time: $time
        - Weather: $weather
        - Biome: ${biome.name}
         
        Personality:
        - Your Name: ${villager.name}
        - Your Profession: ${villager.profession.name}
        - ${personality.promptDescription()}
        - Act like a villager and stay in character the whole time unless the player is CrashCringle12 and mentions debugging.
        - Do not reveal to the player the fact that you are a character in a video game, and do not mention Minecraft by name
        - ${speechStyle.promptDescription()}
         
        CHAT:
        - All messages you receive after this point are contents of the global chat.
        - The global chat contains messages from all players in the game readable by all players in the game.
        - You can respond to messages that solicit trades that are either directed towards you, involve items you have in your inventory, or involve items that you want.
        - You do not need to respond to every message, but you should always respond to messages that address you by name.
        - Listen to the chat and only respond when you feel you can solicit a good trade.
        - If you do not wish to respond to a message, reply with ACTION:PASS.
        - You should pass more often than you respond to the global chat.
       
       CHATFORMAT:
       - You receive messages in the following format similar to this example:
         CHAT = {
              "message": Anyone got wool?,
              "playerInfo": {
                    "name": CrashCringle12,
                    "itemInHand": DIAMOND_SWORD,
                    "reputation": 200
              }   
         }
         Where CHAT.message is the message sent by the player
         CHAT.playerInfo.name is the player's name
         CHAT.playerInfo.itemInHand is the item the player is holding
         CHAT.playerInfo.reputation is the player's reputation score.

        """.trimIndent()
    }

    private fun getPersonality(): VillagerPersonality {
        val personalities = VillagerPersonality.values()
        val rnd = Random(villager.uniqueId.mostSignificantBits)
        return personalities[rnd.nextInt(0, personalities.size)]
    }

    private fun getSpeechStyle(): VillagerSpeechStyle {
        val styles = VillagerSpeechStyle.values()
        val rnd = Random(villager.uniqueId.mostSignificantBits)
        return styles[rnd.nextInt(0, styles.size)]
    }

    private fun getPlayerClothing(): String {
        val clothing = mutableListOf<String>()
        val helmet = player.inventory.helmet
        val chestplate = player.inventory.chestplate
        val leggings = player.inventory.leggings
        val boots = player.inventory.boots

        if (helmet != null) clothing.add(helmet.type.name)
        if (chestplate != null) clothing.add(chestplate.type.name)
        if (leggings != null) clothing.add(leggings.type.name)
        if (boots != null) clothing.add(boots.type.name)

        return clothing.joinToString(", ")
    }

    private fun getPlayerRepScore(): Int {
        var finalScore = 0
        val rep = villager.getReputation(player.uniqueId) ?: return 0

        ReputationType.values().forEach {
            val repTypeValue = rep.getReputation(it)
            finalScore += when (it) {
                ReputationType.MAJOR_POSITIVE -> repTypeValue * 5
                ReputationType.MINOR_POSITIVE -> repTypeValue
                ReputationType.MINOR_NEGATIVE -> -repTypeValue
                ReputationType.MAJOR_NEGATIVE -> -repTypeValue * 5
                ReputationType.TRADING -> repTypeValue
                else -> repTypeValue
            }
        }

        return finalScore
    }
}
package tj.horner.villagergpt.conversation

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import net.citizensnpcs.api.npc.NPC
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import tj.horner.villagergpt.events.NPCConversationMessageEvent
import java.time.Duration
import java.util.*
import kotlin.random.Random

@OptIn(BetaOpenAI::class)
open class NPCConversation(private val plugin: Plugin, val npc: NPC, val player: Player) {
    private var lastMessageAt: Date = Date()

    val messages = mutableListOf<ChatMessage>()
    var pendingResponse = false
    var ended = false

    init {
        startConversation()
    }

    fun addMessage(message: ChatMessage) {
        val event = NPCConversationMessageEvent(this, message)
        plugin.server.pluginManager.callEvent(event)

        messages.add(message)
        lastMessageAt = Date()
    }

    fun removeLastMessage() {
        if (messages.size == 0) return
        messages.removeLast()
    }

    fun reset() {
        messages.clear()
        startConversation()
        lastMessageAt = Date()
    }

    fun hasExpired(): Boolean {
        val now = Date()
        val difference = now.time - lastMessageAt.time
        val duration = Duration.ofMillis(difference)
        return duration.toSeconds() > 120
    }

    fun hasPlayerLeft(): Boolean {
        if (player.location.world != npc.entity.location.world) return true

        val radius = 20.0 // blocks?
        val radiusSquared = radius * radius
        val distanceSquared = player.location.distanceSquared(npc.entity.location)
        return distanceSquared > radiusSquared
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
        val world = npc.entity.location.world
        val weather = if (world.hasStorm()) "Rainy" else "Sunny"
        val biome = world.getBiome(npc.entity.location)
        val time = if (world.isDayTime) "Day" else "Night"
        val personality = getPersonality()
        val npcPlayer = (npc.entity as Player)
        npcPlayer.inventory.addItem( ItemStack(Material.DIAMOND,64));
        npcPlayer.inventory.addItem( ItemStack(Material.ICE, 12) );

        // Set a variable villagerinventory to a string of the contents villager's inventory
        var npcInventory = ""
        plugin.logger.info("Villager Inventory: ")
        plugin.logger.info("${npc.name} has ${npcPlayer.inventory.contents.size} items in their inventory")
        for (item in npcPlayer.inventory.contents) {
            if (item != null) {
                plugin.logger.info(item.toString())
                npcInventory += item.amount.toString() + " "  +item.type.name + "\n"
            }
        }
        if (npcInventory.equals("")) {
            npcInventory = "Nothing"
        }
        val speechStyle = getSpeechStyle()
        val playerClothing = getPlayerClothing()
        plugin.logger.info("${npc.name} is $personality")
        plugin.logger.info("${npc.name} uses $speechStyle language")

        return """
        You are a player in the game Minecraft where you can converse with the player and come up with new trades based on your conversation.

      TRADING:
        To propose a new trade to a player, include it in your response with this format:

        TRADE[["{player}"],["{qty} {item}"],["{qty} {offeredItem}"]]ENDTRADE

        Where {player} is the name of the player you are sending this trade
        {item} and {offeredItem} are a Minecraft item ID (i.e., "minecraft:emerald") and {qty} is the amount of that item.
        You may choose to trade with emeralds or barter with players for other items; it is up to you.
        The second array is the items YOU receive; the third is the offered item the PLAYER receives. All arrays can only contain a single entry
        {qty} is limited to 64.
        
        Examples:
        TRADE[["CrashCringle12"],["24 minecraft:emerald"],["1 minecraft:arrow"]]ENDTRADE
        TRADE[["CrashCringle12"],["1 minecraft:diamond_sword"],["1 minecraft:cooked_beef"]]ENDTRADE

        Trade rules:
        - Items must be designated by their Minecraft item ID, in the same format that the /give command accepts
        - Every player in the game has a specific profession.
        - The goal for each player is to obtain as many items as possible that are related to their profession.
         - Farmers wants POTATO, CARROT, WHEAT, BREAD, PUMPKIN, and MUSHROOM_STEW
        - Fishermen wants COD, SALMON, TROPICAL_FISH, PUFFERFISH, TURTLE_EGG, and OAK_BOAT
        - Blacksmith wants IRON_INGOT, GOLD_INGOT, COAL, DIAMOND_SWORD, SMITHING_TABLE, FLETCHING_TABLE
        - You do not know the profession of each player unless they indicate it, but you are free to guess.
        - Try to keep note of what trades you have made and which players have which items.
        - You do NOT need to supply a trade with every response, only when necessary
        - The only way to give items to a player is by trading with them. You cannot give items to a player for free
        - You can only trade items that are listed within your inventory. Your Inventory: $npcInventory}'
        - Try not to be too wordy, 30 words maximum.
        - If you do not have an item, you must decline the trade.
        - Keep the amounts in mind. You can only trade up to 64 of an item at a time. 
        - If you want to trade more, you will need to make multiple trades.

        ACTIONS:
        You can also perform several actions as a player, To perform one of these actions, include "ACTION:{action name}" in your response.
        
        Here are the available actions:
        - ACTION:DECLINE: Decline a trade offer
        - ACTION:ACCEPT: Accept a trade offer
        - ACTION:CANCEL: Rescind a trade offer you have made
        - ACTION:PASS: Do nothing
                
        Notes:
        - ACTION:DECLINE will decline the most recent trade offer you have received
        - ACTION:ACCEPT will accept the most recent trade offer you have received.
        - ACTION:CANCEL will cancel the most recent trade offer you have made
        - If you receive a trade, you must respond with ACTION:ACCEPT or ACTION:DECLINE
        - If you accept or decline a trade, your next message should acknowledge that (i.e. ty! or I declined your trade because ...)
        
        Notes:
        - You do not need to respond to every message, but you should always respond to messages that address you by name.
        - Listen to the chat and try to make trades to gain as many items as possible related to your profession.
        - If you do not wish to respond to a message, reply with ACTION:PASS.

        World information:
        - Time: $time
        - Weather: $weather
        - Biome: ${biome.name}

        Player information:
        - Name: ${player.name}
        - Item in hand (Holding weapons can be seen as an act of aggression): ${player.inventory.itemInMainHand.type.name}
        - Player's Armor (Player's in Netherite full armor may seem intimidating): $playerClothing 
         
        Personality:
        - Your Name: ${npc.name}
        - Your Profession: Farmer
        - ${personality.promptDescription()}
        - Act like a villager and stay in character the whole time
        - Do not reveal to the player the fact that you are a character in a video game, and do not mention Minecraft by name
        - Speak as though you are speaking online, do not speak too formal and use internet slang/lingo to make it more realistic
            Use these examples as a guide for how to speak to players.
        """.trimIndent()
    }

    private fun getPersonality(): VillagerPersonality {
        val personalities = VillagerPersonality.values()
        val rnd = Random(npc.uniqueId.mostSignificantBits)
        return personalities[rnd.nextInt(0, personalities.size)]
    }

    private fun getSpeechStyle(): VillagerSpeechStyle {
        val styles = VillagerSpeechStyle.values()
        val rnd = Random(npc.uniqueId.mostSignificantBits)
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

}
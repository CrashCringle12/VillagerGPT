package tj.horner.villagergpt.conversation

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import net.citizensnpcs.api.npc.NPC
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import tj.horner.villagergpt.events.NPCGlobalConversationDMEvent
import tj.horner.villagergpt.events.NPCGlobalConversationMessageEvent
import tj.horner.villagergpt.events.NPCGlobalConversationResponseEvent
import java.time.Duration
import java.util.*

@OptIn(BetaOpenAI::class)
class NPCGlobalConversation(private val plugin: Plugin, val npc: NPC, val players: MutableList<Player>) {
    private var lastMessageAt: Date = Date()
    // Create a mapping of NPC to a list of messages
    val messages = mutableListOf<ChatMessage>()
    var pendingResponse = false;
    var ended = false

    init {
        startGlobalConversation()
    }
    fun addMessage(message: ChatMessage, npc : NPC) {
        val event = NPCGlobalConversationMessageEvent(this, message)
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
        startGlobalConversation()
        lastMessageAt = Date()
    }

    fun hasExpired(): Boolean {
        val now = Date()
        val difference = now.time - lastMessageAt.time
        val duration = Duration.ofMillis(difference)
        return duration.toSeconds() > 120
    }


    fun sendResponseToNPCs(message: Component, npc: NPC) {
        // Check if message is an empty string
        if (message.toString() == "" || message.toString() == " ") {
            return
        }

        if (message.toString().contains("ACTION:PASS") || message.toString().contains("ACTION: PASS")) {
            return
        }
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                val event = NPCGlobalConversationResponseEvent(npc, this, message)
                plugin.server.pluginManager.callEvent(event)
            });
        }, (20+ Math.random() * 150).toLong())
    }

    fun sendResponseToNPC(message: Component, npc: NPC, target: NPC) {
        // Check if message is an empty string
        if (message.toString() == "" || message.toString() == " ") {
            return
        }

        if (message.toString().contains("ACTION:PASS") || message.toString().contains("ACTION: PASS")) {
            return
        }
            Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                val event = NPCGlobalConversationDMEvent(npc, target, this, message)
                plugin.server.pluginManager.callEvent(event)
            });
    }

//    fun addMessageAll(message: ChatMessage) {
//        val event = NPCGlobalConversationMessageEvent(this, message)
//        plugin.server.pluginManager.callEvent(event)
//        for (npc in npcs) {
//            npcMessages[npc.uniqueId]!!.add(message)
//            npcLastMessageAt[npc.uniqueId] = Date()
//        }
//    }


//    fun hasPlayerLeft(): Boolean {
//        for (player in players) {
//            if (player.location.world != npcs[0].entity.location.world) return true
//            val radius = 20.0 // blocks?
//            val radiusSquared = radius * radius
//            val distanceSquared = player.location.distanceSquared(npcs[0].entity.location)
//            return distanceSquared > radiusSquared
//
//        }
//
//        return false;
//    }

    private fun startGlobalConversation() {
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
        val npcPlayer = (npc.entity as Player)
        npcPlayer.inventory.addItem( ItemStack(Material.DIAMOND,64));
        npcPlayer.inventory.addItem( ItemStack(Material.ICE, 12) );

        // Set a variable villagerinventory to a string of the contents villager's inventory
        var npcInventory = ""
        plugin.logger.info("**************************")
        plugin.logger.info("Global Chat Prompt Sent")
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
        //plugin.logger.info("${npc.name} is $personality")
        //plugin.logger.info("${npc.name} uses $speechStyle language")

        return """
        You are a player in the game Minecraft where you can converse with other players and come up with new trades based on your conversation.
  
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
        - You can only trade items that are listed within your inventory. Your Inventory: ${npcInventory}'
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
         World information:
        - Time: $time
        - Weather: $weather
        - Biome: ${biome.name}
         
        CHAT:
        - All messages you receive after this point are contents of the global chat.
        - The global chat contains messages from all players in the game readable by all players in the game.
        - You can respond to messages that solicit trades that are either directed towards you, involve items you have in your inventory, or involve items that you want.\
        - There will likely be many messages in the chat that do not concern you. Use ACTION:PASS to ignore these messages.
        - You do not need to respond to every message, but you should always respond to messages that address you by name.
        - Listen to the chat and try to make trades to gain as many items as possible related to your profession.
        - If you do not wish to respond to a message, reply with ACTION:PASS.
     
       
        Personality:
        - Your Name: ${npc.name}
        - Your Profession: Farmer
        Speaking verbose is suspicious and will make the player think you are a bot. 
        """.trimIndent()
    }
}
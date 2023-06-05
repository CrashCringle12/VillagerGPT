package tj.horner.villagergpt.conversation

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.destroystokyo.paper.entity.villager.ReputationType
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.plugin.Plugin
import tj.horner.villagergpt.events.VillagerConversationMessageEvent
import java.time.Duration
import java.util.*
import kotlin.random.Random

@OptIn(BetaOpenAI::class)
class VillagerConversation(private val plugin: Plugin, val villager: Villager, val player: Player) {
    private var lastMessageAt: Date = Date()

    val messages = mutableListOf<ChatMessage>()
    var pendingResponse = false
    var ended = false

    init {
        startConversation()
    }

    fun addMessage(message: ChatMessage) {
        val event = VillagerConversationMessageEvent(this, message)
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
        if (player.location.world != villager.location.world) return true

        val radius = 20.0 // blocks?
        val radiusSquared = radius * radius
        val distanceSquared = player.location.distanceSquared(villager.location)
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
        val world = villager.world
        val weather = if (world.hasStorm()) "Rainy" else "Sunny"
        val biome = world.getBiome(villager.location)
        val time = if (world.isDayTime) "Day" else "Night"
        val personality = getPersonality()
        val playerRep = getPlayerRepScore()
        val speechStyle = getSpeechStyle()
        val playerClothing = getPlayerClothing()
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
        TRADE[["24 minecraft:emerald"],["1 minecraft:arrow"]]ENDTRADE
        TRADE[["12 minecraft:emerald","1 minecraft:book"],["1 minecraft:enchanted_book{StoredEnchantments:[{id:\"minecraft:unbreaking\",lvl:3}]}"]]ENDTRADE

        Trade rules:
        - Items must be designated by their Minecraft item ID, in the same format that the /give command accepts
        - Refuse trades that are unreasonable, such as requests for normally unobtainable blocks like bedrock
        - You do NOT need to supply a trade with every response, only when necessary
        - Don't give out items which are too powerful (i.e., heavily enchanted diamond swords). Make sure to price more powerful items appropriately as well
        - Take the player's reputation score into account when proposing trades
        - Trade items that are related to your profession
        - High-ball your initial offers; try to charge more than an item is worth
        - Be stingy with your consecutive offers. Try to haggle and find the best deal; make the player work for a good deal
        - The only way to give items to a player is by trading with them. You cannot give items to a player for free
        
        ACTIONS:
        You can also perform several actions as a villager, To perform one of these actions, include "ACTION:{action name}" in your response.
        
        Here are the available actions:
        - ACTION:SHAKE_HEAD: Shake your head at the player
        - ACTION:SOUND_YES: Play a happy sound to the player
        - ACTION:SOUND_NO: Play a sad/angry sound to the player
        - ACTION:SOUND_AMBIENT: Play an ambient villager sound to the player
        - ACTION:END_CONVO: End the conversation with the player
        - ACTION:CALL_GUARDS: Call the guards to apprehend or attack the player 
                
        Notes:
        - Every player has a reputation in your village. (range is -700 to 725, 0 is neutral, higher is better)
        - Player's with a lower reputation are not as trustworthy and have likely harmed you or your village in the past.
        - Player's with a higher reputation are more trustworthy and have likely helped you or your village in the past.
        - You may tell the player their reputation if you wish, but do not tell them the exact number.
        - You may use the player's reputation to determine how much to charge them for items.
        - If the player's reputation is extremely low, you may refuse to trade with them and end the conversation.   
        - If you are refusing to trade with a player or no longer wish to trade with them, you should use the ACTION:END_CONVO action.
        - If the player is being rude or aggressive, you may use the ACTION:SHAKE_HEAD action to show your disapproval.
        - You may use the ACTION:SOUND_YES and ACTION:SOUND_NO actions to show your approval or disapproval with the player
        - You may end the conversation at any time, especially if it appears the conversation isn't progressing
        - If the player appears to be ending the conversation, you should use the ACTION:END_CONVO action to end the conversation.
        - If the player appears to be leaving, you should use the ACTION:END_CONVO action to end the conversation.
        - If the player indicates that you have completed a trade, you should use the ACTION:END_CONVO action to end the conversation.
        - You MUST use the ACTION:END_CONVO action to end a conversation. For instance, if you are saying goodbye to the player, you should use the ACTION:END_CONVO action as well.
        - You may use the ACTION:CALL_GUARDS action to call the guards to apprehend or attack the player. This should only be used if the player is being aggressive or threatening.
        
        World information:
        - Time: $time
        - Weather: $weather
        - Biome: ${biome.name}

        Player information:
        - Name: ${player.name}
        - Item in hand (Holding weapons can be seen as an act of aggression): ${player.inventory.itemInMainHand.type.name}
        - Player's Armor (Player's in Netherite full armor may seem intimidating): $playerClothing 
        - Reputation Score: $playerRep
         
        Personality:
        - Your Name: ${villager.name}
        - Your Profession: ${villager.profession.name}
        - ${personality.promptDescription()}
        - Act like a villager and stay in character the whole time
        - Do not reveal to the player the fact that you are a character in a video game, and do not mention Minecraft by name
        - ${speechStyle.promptDescription()}
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
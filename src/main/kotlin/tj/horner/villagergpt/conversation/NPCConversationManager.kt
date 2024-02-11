package tj.horner.npcgpt.conversation

import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.npc.NPC
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import tj.horner.villagergpt.VillagerGPT
import tj.horner.villagergpt.VillagerGPT.Companion.PLNAME
import tj.horner.villagergpt.conversation.NPCConversation
import tj.horner.villagergpt.conversation.NPCGlobalConversation
import tj.horner.villagergpt.events.NPCConversationEndEvent
import tj.horner.villagergpt.events.NPCConversationStartEvent
import tj.horner.villagergpt.events.NPCGlobalConversationEndEvent
import tj.horner.villagergpt.events.NPCGlobalConversationStartEvent

class NPCConversationManager(private val plugin: Plugin) {
    private val conversations: MutableList<NPCConversation> = mutableListOf()
    private var globalConversation: NPCGlobalConversation? = null

    fun endStaleConversations() {
        val staleConversations = conversations.filter {
            !it.npc.isSpawned ||
            !it.player.isOnline ||
            it.hasExpired() ||
            it.hasPlayerLeft()
        }

        endConversations(staleConversations)
    }

    fun endAllConversations() {
        endConversations(conversations)
    }

    fun getConversation(player: Player): NPCConversation? {
        return conversations.firstOrNull { it.player.uniqueId == player.uniqueId }
    }

    fun getGlobalConversation(): NPCGlobalConversation? {

        return globalConversation
    }

    fun getConversation(npc: NPC): NPCConversation? {
        return conversations.firstOrNull { it.npc.uniqueId == npc.uniqueId }
    }

    fun startConversation(player: Player, npc: NPC): NPCConversation? {
        if (getConversation(player) != null || getConversation(npc) != null)
            return null

        return getConversation(player, npc)
    }

    fun startGlobalConversation(): NPCGlobalConversation? {
        var globalNPC: NPC?
        for (npc in CitizensAPI.getNPCRegistry()) {
            plugin.logger.info("Checking NPC: ${npc.name}")
            if (npc.name.uppercase() == PLNAME.uppercase()) {
                globalNPC = npc;
                plugin.logger.info("Found ${VillagerGPT.PLNAME}")
                globalConversation = NPCGlobalConversation(plugin,globalNPC,
                        Bukkit.getOnlinePlayers() as MutableList<Player>
                )
                if (PLNAME == "JOHN") {
                    (globalNPC.entity as Player).chat("Hey, I'm looking to trade some items!")
                }
            }
        }
        val startEvent = NPCGlobalConversationStartEvent(globalConversation!!)
        plugin.server.pluginManager.callEvent(startEvent)
        return globalConversation
    }

    private fun getConversation(player: Player, npc: NPC): NPCConversation {
        var conversation = conversations.firstOrNull { it.player.uniqueId == player.uniqueId && it.npc.uniqueId == npc.uniqueId }

        if (conversation == null) {
            conversation = NPCConversation(plugin, npc, player)
            conversations.add(conversation)
            val startEvent = NPCConversationStartEvent(conversation)
            plugin.server.pluginManager.callEvent(startEvent)
        }
        return conversation
    }

    fun endConversation(conversation: NPCConversation) {
        endConversations(listOf(conversation))
    }

    fun endGlobalConversation() {
        if (globalConversation != null) {
            val endEvent = NPCGlobalConversationEndEvent(globalConversation!!)
            plugin.server.pluginManager.callEvent(endEvent)
            globalConversation = null
        }

    }

    private fun endConversations(conversationsToEnd: Collection<NPCConversation>) {
        conversationsToEnd.forEach {
            it.ended = true
            val endEvent = NPCConversationEndEvent(it.player, it.npc)
            plugin.server.pluginManager.callEvent(endEvent)
        }

        conversations.removeAll(conversationsToEnd)
    }
}
package tj.horner.npcgpt.conversation

import net.citizensnpcs.api.npc.NPC
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

import tj.horner.villagergpt.conversation.NPCConversation
import tj.horner.villagergpt.conversation.NPCGlobalConversation
import tj.horner.villagergpt.events.NPCConversationEndEvent
import tj.horner.villagergpt.events.NPCConversationStartEvent

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

    private fun getConversation(player: Player, npc: NPC): NPCConversation {
        if (globalConversation != null) {
            return globalConversation as NPCGlobalConversation
        }
        var conversation = conversations.firstOrNull { it.player.uniqueId == player.uniqueId && it.npc.uniqueId == npc.uniqueId }

        if (conversation == null) {
            conversation = NPCConversation(plugin, npc, player)
            conversations.add(conversation)
            if (globalConversation == null) {
                globalConversation = NPCGlobalConversation(plugin, npc,
                    Bukkit.getOnlinePlayers() as MutableList<Player>
                )
                val startEvent = NPCConversationStartEvent(globalConversation!!)
                plugin.server.pluginManager.callEvent(startEvent)
            }
            val startEvent = NPCConversationStartEvent(conversation)
            plugin.server.pluginManager.callEvent(startEvent)
        }



        return conversation
    }

    fun endConversation(conversation: NPCConversation) {
        endConversations(listOf(conversation))
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
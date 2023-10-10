package tj.horner.villagergpt.conversation

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.plugin.Plugin
import tj.horner.villagergpt.events.VillagerConversationEndEvent
import tj.horner.villagergpt.events.VillagerConversationStartEvent

class VillagerConversationManager(private val plugin: Plugin) {
    private val conversations: MutableList<VillagerConversation> = mutableListOf()
    private var globalConversation: VillagerGlobalConversation? = null

    fun endStaleConversations() {
        val staleConversations = conversations.filter {
            it.villager.isDead ||
            !it.player.isOnline ||
            it.hasExpired() ||
            it.hasPlayerLeft()
        }

        endConversations(staleConversations)
    }

    fun endAllConversations() {
        endConversations(conversations)
    }

    fun getConversation(player: Player): VillagerConversation? {
        return conversations.firstOrNull { it.player.uniqueId == player.uniqueId }
    }

    fun getGlobalConversation(): VillagerGlobalConversation? {
        return globalConversation
    }

    fun getConversation(villager: Villager): VillagerConversation? {
        return conversations.firstOrNull { it.villager.uniqueId == villager.uniqueId }
    }

    fun startConversation(player: Player, villager: Villager): VillagerConversation? {
        if (getConversation(player) != null || getConversation(villager) != null)
            return null

        return getConversation(player, villager)
    }

    private fun getConversation(player: Player, villager: Villager): VillagerConversation {
        if (globalConversation != null) {
            return globalConversation as VillagerGlobalConversation
        }
        var conversation = conversations.firstOrNull { it.player.uniqueId == player.uniqueId && it.villager.uniqueId == villager.uniqueId }

        if (conversation == null) {
            conversation = VillagerConversation(plugin, villager, player)
            conversations.add(conversation)
            if (globalConversation == null) {
                globalConversation = VillagerGlobalConversation(plugin, villager,
                    Bukkit.getOnlinePlayers() as MutableList<Player>
                )
                val startEvent = VillagerConversationStartEvent(globalConversation!!)
                plugin.server.pluginManager.callEvent(startEvent)
            }
            val startEvent = VillagerConversationStartEvent(conversation)
            plugin.server.pluginManager.callEvent(startEvent)
        }



        return conversation
    }

    fun endConversation(conversation: VillagerConversation) {
        endConversations(listOf(conversation))
    }

    private fun endConversations(conversationsToEnd: Collection<VillagerConversation>) {
        conversationsToEnd.forEach {
            it.ended = true
            val endEvent = VillagerConversationEndEvent(it.player, it.villager)
            plugin.server.pluginManager.callEvent(endEvent)
        }

        conversations.removeAll(conversationsToEnd)
    }
}
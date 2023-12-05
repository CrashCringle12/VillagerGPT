package tj.horner.villagergpt.conversation.pipeline.actions

import com.destroystokyo.paper.entity.villager.ReputationType
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.IronGolem
import tj.horner.villagergpt.VillagerGPT
import tj.horner.villagergpt.conversation.NPCConversation
import tj.horner.villagergpt.conversation.pipeline.ConversationMessageAction
import tj.horner.villagergpt.events.NPCConversationEndEvent

class CallGuardsAction (private val villagerconversation: NPCConversation, private val plugin: VillagerGPT) : ConversationMessageAction {
    override fun run() {
        villagerconversation.ended = true
        val endEvent = NPCConversationEndEvent(villagerconversation.player, villagerconversation.npc)
        plugin.server.pluginManager.callEvent(endEvent)

        val loc = villagerconversation.npc.entity.location
        val world = loc.world
        val x = loc.x
        val y = loc.y
        val z = loc.z
        for (i in 1..4) {
            val golem = world.spawnEntity(Location(world, x + 1, y, z), EntityType.IRON_GOLEM) as IronGolem
            golem.target = villagerconversation.player
        }

        // Add ReputationType.MAJOR_NEGATIVE to the villager
        // Code:
//        val reputations = villagerconversation.npc.reputations.get(villagerconversation.player.uniqueId)
//        reputations?.setReputation(ReputationType.MAJOR_NEGATIVE, ((reputations.getReputation(ReputationType.MAJOR_NEGATIVE) + 1) * 1.5).toInt())



    }
}
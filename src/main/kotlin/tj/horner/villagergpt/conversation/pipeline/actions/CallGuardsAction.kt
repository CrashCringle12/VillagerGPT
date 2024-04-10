package tj.horner.villagergpt.conversation.pipeline.actions

import com.destroystokyo.paper.entity.villager.ReputationType
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.IronGolem
import tj.horner.villagergpt.VillagerGPT
import tj.horner.villagergpt.conversation.VillagerConversation
import tj.horner.villagergpt.conversation.pipeline.ConversationMessageAction
import tj.horner.villagergpt.events.VillagerConversationEndEvent

class CallGuardsAction (private val villagerconversation: VillagerConversation, private val plugin: VillagerGPT) : ConversationMessageAction {
    override fun run() {
        villagerconversation.ended = true
        val endEvent = VillagerConversationEndEvent(villagerconversation.player, villagerconversation.villager)
        plugin.server.pluginManager.callEvent(endEvent)

        val loc = villagerconversation.villager.location
        val world = loc.world
        val x = loc.x
        val y = loc.y
        val z = loc.z
        for (i in 1..4) {
            val golem = world.spawnEntity(Location(world, x + 1, y, z), EntityType.IRON_GOLEM) as IronGolem
            golem.target = villagerconversation.player
        }
        villagerconversation.player.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOW, 1200, 10))
        villagerconversation.player.sendMessage("You have been handcuffed!")
        // Add ReputationType.MAJOR_NEGATIVE to the villager
        // Code:
        val reputations = villagerconversation.villager.reputations.get(villagerconversation.player.uniqueId)
        reputations?.setReputation(ReputationType.MAJOR_NEGATIVE, 100)
        reputations?.setReputation(ReputationType.MINOR_NEGATIVE, 200)
        reputations?.setReputation(ReputationType.MAJOR_POSITIVE, 0)
    }
}
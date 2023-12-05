package tj.horner.villagergpt.conversation.pipeline.actions

import net.citizensnpcs.api.npc.NPC
import org.bukkit.inventory.MerchantRecipe
import tj.horner.villagergpt.conversation.pipeline.ConversationMessageAction

class SetTradesAction(private val villager: NPC, private val trades: List<MerchantRecipe>) : ConversationMessageAction {
    override fun run() {
//        villager.resetOffers()
//        villager.recipes = trades
    }
}
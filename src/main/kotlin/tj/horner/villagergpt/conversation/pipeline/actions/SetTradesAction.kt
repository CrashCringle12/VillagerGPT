package tj.horner.villagergpt.conversation.pipeline.actions

import crashcringle.malmoserverplugin.barterkings.trades.Trade
import crashcringle.malmoserverplugin.barterkings.trades.TradeRequest
import net.citizensnpcs.api.npc.NPC
import tj.horner.villagergpt.conversation.pipeline.ConversationMessageAction

class SetTradesAction(private val npc: NPC, private val trades: MutableList<Trade>) : ConversationMessageAction {
    override fun run() {

//        villager.resetOffers()
//        villager.recipes = trades
    }
}
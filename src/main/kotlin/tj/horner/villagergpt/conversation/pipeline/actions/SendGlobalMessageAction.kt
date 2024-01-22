package tj.horner.villagergpt.conversation.pipeline.actions;
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import tj.horner.villagergpt.conversation.pipeline.ConversationMessageAction

class SendGlobalMessageAction(private val message: Component) : ConversationMessageAction {

    override fun run() {
        for (player in Bukkit.getOnlinePlayers())
            player.sendMessage(message)
    }
}
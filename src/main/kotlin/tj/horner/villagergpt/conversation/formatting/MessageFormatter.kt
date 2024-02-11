package tj.horner.villagergpt.conversation.formatting

import net.citizensnpcs.api.npc.NPC
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.ChatColor

object MessageFormatter {
    fun formatMessageFromPlayer(message: Component, npc: NPC): Component {
        return formatMessage(message, playerComponent(), npcComponent(npc))
    }

    fun formatMessageFromGlobal(message: String, name : String ): String {
        return formatMessage(message, name)
    }

    fun formatMessageFromNPC(message: Component, npc: NPC): Component {
        return formatMessage(message, npcComponent(npc), playerComponent())
    }


    private fun formatMessage(message: Component, sender: Component, recipient: Component): Component {
        val formattedMessage = Component.text().content("")

        return formattedMessage
            .append(sender)
            .append(Component.text(" → ").color(NamedTextColor.WHITE))
            .append(recipient)
            .append(Component.text(": "))
            .append(message)
            .build()
    }

    private fun formatMessage(message: String, sender: String): String {
        val formattedMessage = "" + sender + ChatColor.WHITE + " → " + message
        return formattedMessage
    }


    private fun playerComponent(): Component {
        return Component.text("You").color(NamedTextColor.DARK_AQUA)
    }

    private fun playerGlobalComponent(name : String): Component {
        return Component.text(name).color(NamedTextColor.DARK_AQUA)
    }

    private fun npcComponent(npc: NPC): Component {
        return npc.entity.name().color(NamedTextColor.AQUA)
    }

    private fun globalComponent(): Component {
        return Component.text("").color(NamedTextColor.DARK_GREEN)
    }
}
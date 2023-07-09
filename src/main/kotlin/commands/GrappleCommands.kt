package commands

import items.ItemManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GrappleCommands : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Only players can use this command!")
            return true
        }

        val player = sender

        if (command.name.equals("givegrapple", ignoreCase = true)) {
            player.inventory.addItem(ItemManager.grappleHook)
        }

        return true
    }
}

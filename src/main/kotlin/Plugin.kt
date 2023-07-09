import commands.GrappleCommands
import events.GrappleEvents
import events.PlayerJoin
import items.ItemManager
import org.bukkit.plugin.java.JavaPlugin

@Suppress("UNUSED")
class Plugin : JavaPlugin(){
    override fun onEnable() {
        server.pluginManager.registerEvents(PlayerJoin(), this)
        server.pluginManager.registerEvents(GrappleEvents(this), this)

        ItemManager.init()

        getCommand("givegrapple")?.setExecutor(GrappleCommands())
    }
}
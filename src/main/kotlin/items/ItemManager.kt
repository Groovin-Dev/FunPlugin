package items

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object ItemManager {
    lateinit var grappleHook: ItemStack

    fun init() {
        createGrappleHook()
    }

    private fun createGrappleHook() {
        val item = ItemStack(Material.PAPER, 1)
        val meta = item.itemMeta

        // Set name and display name
        meta.displayName(Component.text("Grapple Hook"))
        meta.lore(listOf(Component.text("A hook that can be used to grapple to blocks.")))

        // Set item meta
        item.setItemMeta(meta)

        // Set item
        grappleHook = item
    }
}

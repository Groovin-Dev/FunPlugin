package events

import Plugin
import items.ItemManager
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector

class GrappleEvents(private val plugin: Plugin) : Listener {

    @EventHandler
    fun onRightClick(event: PlayerInteractEvent) {
        val player = event.player
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return
        if (!isPlayerHoldingGrappleHook(player)) return

        // If the player is already grappled, cancel the event
        // Use a scoreboard to track if the player is grappled
        if (player.scoreboardTags.contains("grappled")) {
            plugin.logger.info("Grapple hook used by ${player.name} but is already grappled")
            return
        }

        // Perform raycast
        val hitResult = player.rayTraceBlocks(50.0)
        val hitBlock = hitResult?.hitBlock

        // If the raycast didn't hit a block, cancel the event
        if (hitBlock == null) {
            plugin.logger.info("Grapple hook used by ${player.name} but didn't hit a block")
            return
        }

        // If there is not 2 blocks of air above the hit block, cancel the event
        if (hitBlock.getRelative(0, 1, 0).type.isSolid || hitBlock.getRelative(0, 2, 0).type.isSolid) {
            plugin.logger.info("Grapple hook used by ${player.name} but there is not 2 blocks of air above the hit block")

            // Tell the player you cant grapple without a ledge
            player.sendMessage("You can't grapple without a ledge!")

            return
        }

        plugin.logger.info("Grapple hook used by ${player.name} and hit block ${hitBlock.type}")

        // Set the player's scoreboard tag to grappled
        player.scoreboardTags.add("grappled")

        // compute the vector from the player to the hit block
        val ray = hitBlock.location.toVector().subtract(player.location.toVector())
        val direction = ray.clone().normalize()

        grappleTeleport(player, hitBlock.location, direction).runTaskTimer(plugin, 0, 1)
        grappleParticle(player, hitBlock.location).runTaskTimer(plugin, 0, 1)
    }

    private fun isPlayerHoldingGrappleHook(player: Player): Boolean {
        return player.inventory.itemInMainHand.isSimilar(ItemManager.grappleHook)
    }

    private fun isPlayerNearLocation(player: Player, location: Location, distance: Int): Boolean {
        return player.location.distance(location).toInt() <= distance
    }

    private fun cancelGrapple(player: Player) {
        if (player.scoreboardTags.contains("grappled")) {
            player.scoreboardTags.remove("grappled")
        }
    }

    fun checkGrapple(player: Player, grappleLocation: Location): Boolean {
        // If the player is no longer holding the grapple hook, cancel the grapple
        if (!isPlayerHoldingGrappleHook(player)) {
            plugin.logger.info("Grapple cancelled for ${player.name}. Reason: player no longer holding grapple hook")
            cancelGrapple(player)
            return false
        }

        // If the player is near the grapple location, cancel the grapple
        if (isPlayerNearLocation(player, grappleLocation, 2)) {
            plugin.logger.info("Grapple cancelled for ${player.name}. Reason: player is near grapple location")
            cancelGrapple(player)
            return false
        }

        // If the player is no longer grappled, cancel the grapple
        if (!player.scoreboardTags.contains("grappled")) {
            plugin.logger.info("Grapple cancelled for ${player.name}. Reason: player no longer grappled")
            cancelGrapple(player)
            return false
        }

        return true
    }

    private fun grappleTeleport(player: Player, targetLocation: Location, direction: Vector): BukkitRunnable {
        return object : BukkitRunnable() {
            override fun run() {
                // Check if the grapple should be cancelled
                if (!checkGrapple(player, targetLocation)) {
                    cancel()

                    // Create a new location at the top of the target location
                    val topOfTargetLocation = targetLocation.clone().add(0.0, 1.0, 0.0)

                    // Set the new locations direction to the players direction
                    topOfTargetLocation.direction = player.location.direction

                    // Teleport the player to the top of the target location
                    player.teleport(topOfTargetLocation)

                    return
                }

                player.teleport(player.location.add(direction))
                player.playSound(player.location, Sound.BLOCK_DISPENSER_FAIL, 1.0f, 1.0f)

                val currentLocationAsString = "${player.location.x}, ${player.location.y}, ${player.location.z}"
                val targetLocationAsString = "${targetLocation.x}, ${targetLocation.y}, ${targetLocation.z}"
                plugin.logger.info("GrappleTeleport for ${player.name}: $currentLocationAsString -> $targetLocationAsString (${player.location.distance(targetLocation)})")
            }
        }
    }

    private fun grappleParticle(player: Player, targetLocation: Location): BukkitRunnable {
        return object : BukkitRunnable() {
            private val initialLocation = player.location.clone() // Record the player's original location

            override fun run() {
                // Check if the grapple should be cancelled
                if (!checkGrapple(player, targetLocation)) {
                    cancel()
                    return
                }

                val ray = targetLocation.toVector().subtract(initialLocation.toVector())
                val particleCount = 50 // adjust as necessary
                for (i in 0..particleCount) {
                    val t = i.toDouble() / particleCount
                    val particleLocation = initialLocation.toVector().add(ray.clone().multiply(t)).toLocation(player.world)
                    player.world.spawnParticle(Particle.CRIT, particleLocation, 0)
                }
            }
        }
    }
}

package events

import Plugin
import items.ItemManager
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.scheduler.BukkitTask

class GrappleEvents(private val plugin: Plugin) : Listener {
    @EventHandler
    fun onRightClick(event: PlayerInteractEvent) {
        val player = event.player
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return

        // check if player is holding grapple hook
        if (player.inventory.itemInMainHand.isSimilar(ItemManager.grappleHook)) {
            plugin.logger.info("Grapple hook used by ${player.name}")

            // perform raycast
            val hitResult = player.rayTraceBlocks(50.0)
            val hitBlock = hitResult?.hitBlock

            // if raycast hit a block
            if (hitBlock != null) {
                plugin.logger.info("Grapple hook hit a block at ${hitBlock.location}")

                // compute the vector from the player to the hit block
                val ray = hitBlock.location.toVector().subtract(player.location.toVector())
                val direction = ray.clone().normalize()

                val initialLocation = player.location.clone() // Record the player's original location

                // task that moves the player towards the hit block and creates a line of particles
                var moveTask: BukkitTask? = null
                moveTask = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
                    // check if player is still holding grapple hook and hasn't reached the hit block
                    if (!player.inventory.itemInMainHand.isSimilar(ItemManager.grappleHook)) {
                        plugin.logger.info("Grapple hook task cancelled for ${player.name}")
                        moveTask?.cancel()
                    } else {
                        // move the player towards the hit block
                        player.teleport(player.location.add(direction))

                        // Play the dispenser fail sound
                        player.playSound(player.location, Sound.BLOCK_DISPENSER_FAIL, 1.0f, 1.0f)

                        // If the player is close enough to the hit block, teleport them to the top of it and cancel the task
                        val distanceToBlock = player.location.distance(hitBlock.location)
                        if (distanceToBlock < 1.0) {
                            // Get the player's current pitch and yaw
                            val pitch = player.location.pitch
                            val yaw = player.location.yaw

                            // Teleport the player to the top of the hit block
                            player.teleport(Location(hitBlock.world, hitBlock.x + 0.5, hitBlock.y + 1.0, hitBlock.z + 0.5, yaw, pitch))

                            moveTask?.cancel()
                        }

                        // create a line of particles from the player to the hit block
                        val particleCount = 50 // adjust as necessary
                        for (i in 0..particleCount) {
                            val t = i.toDouble() / particleCount
                            val particleLocation = initialLocation.toVector().add(ray.clone().multiply(t)).toLocation(player.world)
                            player.world.spawnParticle(Particle.CRIT, particleLocation, 0)
                        }
                    }
                }, 0, 1)
            }
        }
    }
}

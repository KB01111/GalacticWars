package galacticwars.clonewars.survival

import java.util.concurrent.atomic.AtomicBoolean
import net.minecraft.world.entity.animal.equine.AbstractHorse

/** Loader-neutral persistent cooldown access for brush-based mount fiber recovery. */
object MountFiberAttachmentRuntime {
    private val installed = AtomicBoolean()

    @Volatile
    private var platformAccess: MountFiberAttachmentAccess? = null

    @JvmStatic
    fun install(access: MountFiberAttachmentAccess) {
        require(installed.compareAndSet(false, true)) {
            "Mount fiber attachment runtime is already installed"
        }
        platformAccess = access
    }

    @JvmStatic
    fun lastBrushedDay(horse: AbstractHorse): Long? =
        checkNotNull(platformAccess) {
            "Mount fiber attachment runtime has not been installed"
        }.getLastBrushedDay(horse)

    /** Atomically checks and records the daily recovery window on the server thread. */
    @JvmStatic
    fun tryMarkBrushed(horse: AbstractHorse, day: Long): Boolean {
        require(day >= 0L) { "Mount brushed day cannot be negative" }
        val access = checkNotNull(platformAccess) {
            "Mount fiber attachment runtime has not been installed"
        }
        if (access.getLastBrushedDay(horse) == day) {
            return false
        }
        access.setLastBrushedDay(horse, day)
        return true
    }
}

interface MountFiberAttachmentAccess {
    fun getLastBrushedDay(horse: AbstractHorse): Long?

    fun setLastBrushedDay(horse: AbstractHorse, day: Long)
}

package galacticwars.clonewars.client;

import galacticwars.clonewars.network.ClassHudPayload;

/** Client-only interpolation of the owner-targeted class HUD projection. */
public final class ClassClientState {
    private static final long CLIENT_TICK_NANOS = 50_000_000L;
    private static final long STALE_AFTER_NANOS = 2_500_000_000L;
    private static ClassHudPayload snapshot = ClassHudPayload.unassigned();
    private static long receivedAtNanos;

    private ClassClientState() {
    }

    public static void update(ClassHudPayload payload) {
        snapshot = payload;
        receivedAtNanos = System.nanoTime();
    }

    public static ClassHudPayload snapshot() {
        ClassHudPayload authoritative = snapshot;
        long elapsedNanos = Math.max(0L, System.nanoTime() - receivedAtNanos);
        int elapsedTicks = (int) Math.min(Integer.MAX_VALUE, elapsedNanos / CLIENT_TICK_NANOS);
        return new ClassHudPayload(
                authoritative.classId(),
                authoritative.rank(),
                authoritative.experience(),
                authoritative.experienceForNextRank(),
                authoritative.nextMilestoneRank(),
                authoritative.resource(),
                authoritative.ability1Id(),
                remaining(authoritative.cooldown1(), elapsedTicks),
                authoritative.ability2Id(),
                remaining(authoritative.cooldown2(), elapsedTicks));
    }

    public static boolean visible() {
        return receivedAtNanos != 0L
                && !snapshot.classId().isBlank()
                && System.nanoTime() - receivedAtNanos < STALE_AFTER_NANOS;
    }

    private static int remaining(int cooldown, int elapsedTicks) {
        return Math.max(0, cooldown - elapsedTicks);
    }
}

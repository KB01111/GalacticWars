package galacticwars.clonewars.client;

import galacticwars.clonewars.network.ServerPolicyPayload;

/** Read-only server-policy projection; no client preference mutates these values. */
public final class ServerPolicyClientState {
    private static ServerPolicyPayload snapshot = new ServerPolicyPayload(false, true, false, true, true, true);

    private ServerPolicyClientState() {
    }

    public static void update(ServerPolicyPayload payload) { snapshot = payload; }
    public static ServerPolicyPayload snapshot() { return snapshot; }
}

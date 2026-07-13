package galacticwars.clonewars.network;

import java.nio.file.Files;
import java.nio.file.Path;

public final class NetworkThreadDispatchTest {
    private NetworkThreadDispatchTest() {
    }

    public static void main(String[] args) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/galacticwars/clonewars/network/GalacticNetwork.java"));
        int dispatches = source.split("context\\.enqueueWork", -1).length - 1;
        if (dispatches != 3) {
            throw new AssertionError("Every server payload handler must dispatch to the main thread");
        }
        System.out.println("NetworkThreadDispatchTest passed");
    }
}

package galacticwars.clonewars.force;

import java.util.Set;

public final class ForceEffectExecutorCoverageTest {
    public static void main(String[] args) {
        if (ForceEffectExecutorCatalog.supports(null)) {
            throw new AssertionError("Null Force effect ids must fail closed");
        }
        if (!ForceEffectExecutorCatalog.registeredIds().equals(
                Set.of("push", "pull", "leap", "dash", "choke"))) {
            throw new AssertionError("Every launch Force effect must have one server executor");
        }
        System.out.println("ForceEffectExecutorCoverageTest passed");
    }
}

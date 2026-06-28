package middleearth.lotr.warmod;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue LOG_STARTUP = BUILDER
            .comment("Whether to log foundation setup during common setup")
            .define("logStartup", true);

    public static final ModConfigSpec.BooleanValue ENABLE_CONTENT_SEED = BUILDER
            .comment("Whether the initial Middle-earth content seed should be considered enabled by gameplay systems")
            .define("enableContentSeed", true);

    static final ModConfigSpec SPEC = BUILDER.build();
}

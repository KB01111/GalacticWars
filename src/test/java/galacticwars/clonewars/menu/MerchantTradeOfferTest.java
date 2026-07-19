package galacticwars.clonewars.menu;

import galacticwars.clonewars.data.LaunchContentDefinitions;
import galacticwars.clonewars.data.LaunchContentRuntime;
import galacticwars.clonewars.economy.PhysicalTradeService;
import galacticwars.clonewars.progression.LaunchContentCatalog;
import io.netty.buffer.Unpooled;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;

public final class MerchantTradeOfferTest {
    public static void main(String[] args) throws Exception {
        var previous = LaunchContentRuntime.current();
        try {
            var override = new LaunchContentDefinitions.TradeDefinition(
                    "server_override", "republic", 73, "minecraft:diamond", 3,
                    "faction_intro", 2, "");
            LaunchContentRuntime.install(
                    new LaunchContentDefinitions(
                            Map.of(), Map.of(), Map.of(), Map.of(),
                            Map.of(override.id(), override), Map.of()),
                    List.of("galacticwars:republic"), Map.of());
            var loaded = LaunchContentCatalog.trades().get("server_override");
            var preview = new PhysicalTradeService.TradePreview(
                    loaded.id(), loaded.itemId(), loaded.itemCount(), loaded.price(),
                    true, "available");
            MerchantTradeOffer offer = MerchantTradeOffer.fromPreview(preview);
            assertEquals("server_override", offer.tradeId(), "server override trade id");
            assertEquals("minecraft:diamond", offer.itemId(), "server override item id");
            assertEquals(3, offer.itemCount(), "server override amount");
            assertEquals(73, offer.creditPrice(), "server override price");
            assertTrue(offer.eligible(), "server override eligibility");
            assertEquals("reason.galacticwars.trade.available", offer.reasonTranslationKey(),
                    "localized availability reason");

            MerchantTradeOffer locked = MerchantTradeOffer.fromPreview(
                    new PhysicalTradeService.TradePreview(
                            "locked_override", "minecraft:iron_ingot", 2, 19,
                            false, "trade_locked"));
            FriendlyByteBuf encoded = buffer();
            MerchantTradeOffer.writeOffers(encoded, List.of(offer, locked));
            List<MerchantTradeOffer> decoded = MerchantTradeOffer.readOffers(encoded);
            assertEquals(List.of(offer, locked), decoded, "offer wire round trip");
            assertEquals(0, encoded.readableBytes(), "offer decoder consumed the bounded payload");

            assertTrue(offer.quote().matches(preview), "server quote matches its preview");
            assertFalse(offer.quote().matches(new PhysicalTradeService.TradePreview(
                    loaded.id(), loaded.itemId(), loaded.itemCount(), loaded.price() + 1,
                    true, "available")), "stale price invalidates server quote");
            verifyBounds(offer);
            verifyClientUsesOnlyServerOffers();
            verifyLocalizedReasons();
        } finally {
            LaunchContentRuntime.install(
                    previous.definitions(), previous.factions(), previous.units());
        }
        System.out.println("MerchantTradeOfferTest passed");
    }

    private static void verifyBounds(MerchantTradeOffer offer) {
        assertThrows(() -> MerchantTradeOffer.writeOffers(
                        buffer(), Collections.nCopies(MerchantTradeOffer.MAX_OFFERS + 1, offer)),
                "oversized offer list rejected while encoding");
        FriendlyByteBuf oversized = buffer();
        oversized.writeVarInt(MerchantTradeOffer.MAX_OFFERS + 1);
        assertThrows(() -> MerchantTradeOffer.readOffers(oversized),
                "oversized offer list rejected while decoding");
        assertThrows(() -> new MerchantTradeOffer(
                        "x".repeat(MerchantTradeOffer.MAX_TEXT_BYTES + 1),
                        "minecraft:stone", 1, 1, true,
                        "reason.galacticwars.trade.available"),
                "oversized trade id rejected");
        assertThrows(() -> new MerchantTradeOffer(
                        "bounded", "minecraft:stone",
                        LaunchContentDefinitions.MAX_TRADE_ITEM_COUNT + 1,
                        1, true, "reason.galacticwars.trade.available"),
                "oversized item amount rejected");
        assertThrows(() -> new MerchantTradeOffer(
                        "bounded", "minecraft:stone", 1,
                        LaunchContentDefinitions.MAX_TRADE_CREDIT_PRICE + 1,
                        true, "reason.galacticwars.trade.available"),
                "oversized credit price rejected");
    }

    private static void verifyClientUsesOnlyServerOffers() throws Exception {
        String screen = Files.readString(Path.of(
                "src/main/java/galacticwars/clonewars/client/gui/MerchantTradeScreen.java"));
        assertFalse(screen.contains("LaunchContentCatalog"),
                "client must not reconstruct server datapack trades");
        assertTrue(screen.contains("menu.offers()"), "client consumes server-authored offers");
        assertTrue(screen.contains("button.active = offer.eligible()"),
                "ineligible offers render disabled");
        assertTrue(screen.contains("Tooltip.create"), "disabled offers expose a reason tooltip");
    }

    private static void verifyLocalizedReasons() throws Exception {
        String lang = Files.readString(Path.of(
                "src/main/resources/assets/galacticwars/lang/en_us.json"));
        for (String reason : List.of(
                "available", "merchant_unavailable", "hostile_merchant", "trade_embargoed",
                "trade_locked", "regional_control_required", "unknown_trade_item",
                "insufficient_credits", "offer_changed", "duplicate_event", "unavailable")) {
            assertTrue(lang.contains("\"reason.galacticwars.trade." + reason + "\""),
                    "localized trade reason " + reason);
        }
    }

    private static FriendlyByteBuf buffer() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }

    private static void assertThrows(Runnable action, String label) {
        try {
            action.run();
        } catch (RuntimeException expected) {
            return;
        }
        throw new AssertionError(label);
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }

    private static void assertFalse(boolean value, String label) {
        assertTrue(!value, label);
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected " + expected + " but was " + actual);
        }
    }
}

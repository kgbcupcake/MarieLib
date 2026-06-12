package dev.marie.MariesLib.command;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.marie.MariesLib.api.ValueDefinition;
import dev.marie.MariesLib.api.registry.ProfileRegistry;
import dev.marie.MariesLib.api.registry.ValueRegistry;
import dev.marie.MariesLib.core.MarieLibContext;
import dev.marie.MariesLib.data.SchemaDefinition;
import dev.marie.MariesLib.runtime.SourceRegistry;
import dev.marie.MariesLib.scanner.ItemScanner;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class MarieCommandSupport {

    static final Component NO_CONSUMER_MESSAGE =
            Component.literal("No mod has registered with MarieLib.");

    private static final Map<UUID, String> ACTIVE_PROFILES = new ConcurrentHashMap<>();

    static final SuggestionProvider<CommandSourceStack> VALUE_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(registeredValueKeys(), builder);

    static final SuggestionProvider<CommandSourceStack> PROFILE_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    ProfileRegistry.getAll().stream().map(p -> p.getId()),
                    builder
            );

    static final List<SchemaDefinition> BUILTIN_SCHEMAS = List.of(
            SchemaDefinition.forValue(),
            SchemaDefinition.forSourceClassification(),
            SchemaDefinition.forEffect(),
            SchemaDefinition.forSynergy(),
            SchemaDefinition.forSourcePairSynergy(),
            SchemaDefinition.forMilestone(),
            SchemaDefinition.forTrackingProfile(),
            SchemaDefinition.forCompat()
    );

    static final SuggestionProvider<CommandSourceStack> SCHEMA_TYPE_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    BUILTIN_SCHEMAS.stream().map(SchemaDefinition::getTypeName).toList(),
                    builder);

    private MarieCommandSupport() {}

    static void onPlayerLoggedOut(ServerPlayer player) {
        ACTIVE_PROFILES.remove(player.getUUID());
    }

    static void onServerStopped() {
        ACTIVE_PROFILES.clear();
    }

    static String activeProfile(ServerPlayer player) {
        return ACTIVE_PROFILES.getOrDefault(player.getUUID(), "none");
    }

    static void setActiveProfile(ServerPlayer player, String profile) {
        ACTIVE_PROFILES.put(player.getUUID(), profile);
    }

    static boolean ensureConsumerRegistered(CommandSourceStack source) {
        if (!MarieLibContext.isRegistered()) {
            source.sendFailure(NO_CONSUMER_MESSAGE);
            return false;
        }
        return true;
    }

    static List<String> registeredValueKeys() {
        return ValueRegistry.getAll().stream().map(ValueDefinition::getId).toList();
    }

    static boolean isKnownValueKey(String key) {
        return ValueRegistry.get(key) != null;
    }

    static boolean isClassified(ResourceLocation itemId, Holder<Item> holder) {
        Map<String, Float> external = SourceRegistry.getExternalClassification(itemId);
        if (external != null && !external.isEmpty()) {
            return true;
        }
        return ItemScanner.hasValueTag(new ItemStack(holder.value()));
    }

    static Component copyableGreenLine(String text) {
        return Component.literal(text).withStyle(style -> style
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text)));
    }
}

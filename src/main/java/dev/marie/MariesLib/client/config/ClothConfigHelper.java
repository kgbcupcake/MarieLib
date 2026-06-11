package dev.marie.MariesLib.client.config;

import dev.marie.MariesLib.config.LockRegistry;
import dev.marie.MariesLib.config.MariesLibConfigHolder;
import dev.marie.MariesLib.config.MariesLibConfigKeys;
import dev.marie.MariesLib.core.MariesLib;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Shared Cloth Config helpers: lock filtering, translation keys, entry builders.
 */
final class ClothConfigHelper {

    private ClothConfigHelper() {}

    static String key(String suffix) {
        return "config." + MariesLib.MOD_ID + "." + suffix;
    }

    static Component t(String suffix) {
        return Component.translatable(key(suffix));
    }

    static boolean shouldShow(String lockKey) {
        return !LockRegistry.isLocked(lockKey);
    }

    static boolean isEditable(String lockKey) {
        if (LockRegistry.isServerOnly(lockKey)) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getConnection() != null && !mc.hasSingleplayerServer()) {
                return false;
            }
        }
        return true;
    }

    static void addBool(
            ConfigCategory category,
            ConfigEntryBuilder entryBuilder,
            String lockKey,
            String labelSuffix,
            boolean current,
            boolean defaultValue,
            java.util.function.Consumer<Boolean> save) {
        if (!shouldShow(lockKey)) {
            return;
        }
        AbstractConfigListEntry<?> entry = entryBuilder.startBooleanToggle(t(labelSuffix), current)
                .setDefaultValue(defaultValue)
                .setSaveConsumer(v -> { if (isEditable(lockKey)) save.accept(v); })
                .build();
        entry.setEditable(isEditable(lockKey));
        category.addEntry(entry);
    }

    static void addInt(
            ConfigCategory category,
            ConfigEntryBuilder entryBuilder,
            String lockKey,
            String labelSuffix,
            int current,
            int defaultValue,
            int min,
            int max,
            java.util.function.Consumer<Integer> save) {
        if (!shouldShow(lockKey)) {
            return;
        }
        var field = entryBuilder.startIntField(t(labelSuffix), current)
                .setDefaultValue(defaultValue)
                .setMin(min);
        if (max < Integer.MAX_VALUE) {
            field.setMax(max);
        }
        AbstractConfigListEntry<?> entry = field
                .setSaveConsumer(v -> { if (isEditable(lockKey)) save.accept(v); })
                .build();
        entry.setEditable(isEditable(lockKey));
        category.addEntry(entry);
    }

    static void addLong(
            ConfigCategory category,
            ConfigEntryBuilder entryBuilder,
            String lockKey,
            String labelSuffix,
            long current,
            long defaultValue,
            long min,
            long max,
            java.util.function.Consumer<Long> save) {
        if (!shouldShow(lockKey)) {
            return;
        }
        var field = entryBuilder.startLongField(t(labelSuffix), current)
                .setDefaultValue(defaultValue)
                .setMin(min);
        if (max < Long.MAX_VALUE / 2) {
            field.setMax(max);
        }
        AbstractConfigListEntry<?> entry = field
                .setSaveConsumer(v -> { if (isEditable(lockKey)) save.accept(v); })
                .build();
        entry.setEditable(isEditable(lockKey));
        category.addEntry(entry);
    }

    static void addFloat(
            ConfigCategory category,
            ConfigEntryBuilder entryBuilder,
            String lockKey,
            String labelSuffix,
            float current,
            float defaultValue,
            float min,
            float max,
            java.util.function.Consumer<Float> save) {
        if (!shouldShow(lockKey)) {
            return;
        }
        var field = entryBuilder.startFloatField(t(labelSuffix), current)
                .setDefaultValue(defaultValue)
                .setMin(min);
        if (max < Float.MAX_VALUE / 2f) {
            field.setMax(max);
        }
        AbstractConfigListEntry<?> entry = field
                .setSaveConsumer(v -> { if (isEditable(lockKey)) save.accept(v); })
                .build();
        entry.setEditable(isEditable(lockKey));
        category.addEntry(entry);
    }

    static void addDouble(
            ConfigCategory category,
            ConfigEntryBuilder entryBuilder,
            String lockKey,
            String labelSuffix,
            double current,
            double defaultValue,
            double min,
            double max,
            java.util.function.Consumer<Double> save) {
        if (!shouldShow(lockKey)) {
            return;
        }
        AbstractConfigListEntry<?> entry = entryBuilder.startDoubleField(t(labelSuffix), current)
                .setDefaultValue(defaultValue)
                .setMin(min)
                .setMax(max)
                .setSaveConsumer(v -> { if (isEditable(lockKey)) save.accept(v); })
                .build();
        entry.setEditable(isEditable(lockKey));
        category.addEntry(entry);
    }

    static MariesLibConfigHolder holder() {
        return MariesLibConfigHolder.get();
    }

    static ConfigCategory category(ConfigBuilder builder, String suffix) {
        return builder.getOrCreateCategory(t("category." + suffix));
    }
}

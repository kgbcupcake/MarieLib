package dev.marie.MariesLib.tracking;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import dev.marie.MariesLib.api.ApiStatus;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Per-player cumulative intake totals and one-shot milestone completion bookkeeping.
 * Separate from {@link TrackingData}; consumed by {@link MilestoneTracker}.
 */
@ApiStatus.Internal
public final class MilestoneProgressData {

    private static Supplier<MilestoneProgressData> instanceFactory = MilestoneProgressData::new;

    private static final String CUMULATIVE_INTAKE_CODEC_KEY = "cumulative_intake";
    private static final String COMPLETED_MILESTONE_IDS_CODEC_KEY = "completed_milestone_ids";

    private static final Decoder<MilestoneProgressData> DECODER = new Decoder<>() {
        @Override
        public <T> DataResult<Pair<MilestoneProgressData, T>> decode(DynamicOps<T> ops, T input) {
            return decodeData(ops, input).map(data -> Pair.of(data, input));
        }
    };

    public static final Codec<MilestoneProgressData> CODEC = Codec.of(MilestoneProgressData::encode, DECODER);

    private final Map<String, Float> cumulativeIntake = new LinkedHashMap<>();
    private final Set<String> completedMilestoneIds = new HashSet<>();

    public static MilestoneProgressData createNew() {
        return instanceFactory.get();
    }

    private MilestoneProgressData() {}

    public float getCumulativeIntake(String valueKey) {
        return cumulativeIntake.getOrDefault(valueKey, 0f);
    }

    public void addIntake(String valueKey, float amount) {
        float current = cumulativeIntake.getOrDefault(valueKey, 0f);
        cumulativeIntake.put(valueKey, current + amount);
    }

    public boolean isCompleted(String milestoneId) {
        return completedMilestoneIds.contains(milestoneId);
    }

    public void markCompleted(String milestoneId) {
        completedMilestoneIds.add(milestoneId);
    }

    private static <T> DataResult<T> encode(MilestoneProgressData data, DynamicOps<T> ops, T prefix) {
        RecordBuilder<T> map = ops.mapBuilder();
        map.add(CUMULATIVE_INTAKE_CODEC_KEY,
                Codec.unboundedMap(Codec.STRING, Codec.FLOAT).encodeStart(ops, data.cumulativeIntake));
        map.add(COMPLETED_MILESTONE_IDS_CODEC_KEY,
                Codec.STRING.listOf().encodeStart(ops, List.copyOf(data.completedMilestoneIds)));
        return map.build(prefix);
    }

    private static <T> DataResult<MilestoneProgressData> decodeData(DynamicOps<T> ops, T input) {
        MapLike<T> map = ops.getMap(input).getOrThrow();

        MilestoneProgressData data = createNew();

        T cumulativeVal = map.get(CUMULATIVE_INTAKE_CODEC_KEY);
        if (cumulativeVal != null) {
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT)
                    .parse(ops, cumulativeVal)
                    .result()
                    .ifPresent(data.cumulativeIntake::putAll);
        }

        T completedVal = map.get(COMPLETED_MILESTONE_IDS_CODEC_KEY);
        if (completedVal != null) {
            Codec.STRING.listOf()
                    .parse(ops, completedVal)
                    .result()
                    .ifPresent(data.completedMilestoneIds::addAll);
        }

        return DataResult.success(data);
    }
}

package org.graylog2.datatier.tier.hot;

import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.datatier.tier.DataTier;
import org.graylog2.datatier.tier.DataTierType;
import org.graylog2.datatier.tier.DataTierValidator;
import org.graylog2.indexer.IndexSetValidator;
import org.joda.time.DurationFieldType;
import org.joda.time.Period;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.graylog2.configuration.ElasticsearchConfiguration.TIME_SIZE_OPTIMIZING_RETENTION_FIXED_LEEWAY;
import static org.graylog2.configuration.ElasticsearchConfiguration.TIME_SIZE_OPTIMIZING_ROTATION_PERIOD;
import static org.graylog2.shared.utilities.StringUtils.f;

public class HotTierValidator implements DataTierValidator {

    private final ElasticsearchConfiguration elasticsearchConfiguration;

    @Inject
    public HotTierValidator(ElasticsearchConfiguration elasticsearchConfiguration) {
        this.elasticsearchConfiguration = elasticsearchConfiguration;
    }

    @Override
    public Optional<IndexSetValidator.Violation> validate(@Nullable List<DataTier> dataTiers) {

        if(dataTiers == null){
            return Optional.empty();
        }

        Optional<DataTier> hotTierOpt = dataTiers.stream()
                .filter(dataTier2 -> DataTierType.HOT.equals(dataTier2.getTier()))
                .findFirst();

        if(hotTierOpt.isEmpty()){
            return Optional.empty();
        }

        DataTier config = hotTierOpt.get();
        final Period leeway = config.indexLifetimeMax().minus(config.indexLifetimeMin());
        if (leeway.toStandardSeconds().getSeconds() < 0) {
            return Optional.of(IndexSetValidator.Violation.create(f("%s <%s> is shorter than %s <%s>", HotTier.INDEX_LIFETIME_MAX, config.indexLifetimeMax(),
                    HotTier.INDEX_LIFETIME_MIN, config.indexLifetimeMin())));
        }

        if (leeway.toStandardSeconds().isLessThan(elasticsearchConfiguration.getTimeSizeOptimizingRotationPeriod().toStandardSeconds())) {
            return Optional.of(IndexSetValidator.Violation.create(f("The duration between %s and %s <%s> cannot be shorter than %s <%s>", HotTier.INDEX_LIFETIME_MAX, HotTier.INDEX_LIFETIME_MIN,
                    leeway, TIME_SIZE_OPTIMIZING_ROTATION_PERIOD, elasticsearchConfiguration.getTimeSizeOptimizingRotationPeriod())));
        }

        Period fixedLeeway = elasticsearchConfiguration.getTimeSizeOptimizingRetentionFixedLeeway();
        if (Objects.nonNull(fixedLeeway) && leeway.toStandardSeconds().isLessThan(fixedLeeway.toStandardSeconds())) {
            return Optional.of(IndexSetValidator.Violation.create(f("The duration between %s and %s <%s> cannot be shorter than %s <%s>", HotTier.INDEX_LIFETIME_MAX, HotTier.INDEX_LIFETIME_MIN,
                    leeway, TIME_SIZE_OPTIMIZING_RETENTION_FIXED_LEEWAY, fixedLeeway)));
        }


        final Period maxRetentionPeriod = elasticsearchConfiguration.getMaxIndexRetentionPeriod();
        if (maxRetentionPeriod != null
                && config.indexLifetimeMax().toStandardSeconds().isGreaterThan(maxRetentionPeriod.toStandardSeconds())) {
            return Optional.of(IndexSetValidator.Violation.create(f("Lifetime setting %s <%s> exceeds the configured maximum of %s=%s.",
                    HotTier.INDEX_LIFETIME_MAX, config.indexLifetimeMax(),
                    ElasticsearchConfiguration.MAX_INDEX_RETENTION_PERIOD, maxRetentionPeriod)));
        }

        if (periodOtherThanDays(config.indexLifetimeMax())) {
            return Optional.of(IndexSetValidator.Violation.create(f("Lifetime setting %s <%s> can only be a multiple of days",
                    HotTier.INDEX_LIFETIME_MAX, config.indexLifetimeMax())));
        }
        if (periodOtherThanDays(config.indexLifetimeMin())) {
            return Optional.of(IndexSetValidator.Violation.create(f("Lifetime setting %s <%s> can only be a multiple of days",
                    HotTier.INDEX_LIFETIME_MIN, config.indexLifetimeMin())));
        }
        return Optional.empty();
    }

    boolean periodOtherThanDays(Period period) {
        return Arrays.stream(period.getFieldTypes())
                .filter(type -> !type.equals(DurationFieldType.days()))
                .anyMatch(type -> period.get(type) != 0);
    }
}

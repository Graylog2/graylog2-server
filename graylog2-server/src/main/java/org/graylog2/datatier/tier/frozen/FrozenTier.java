package org.graylog2.datatier.tier.frozen;

import org.graylog2.datatier.tier.DataTier;
import org.graylog2.datatier.tier.DataTierType;
import org.joda.time.Period;

public class FrozenTier implements DataTier {

    @Override
    public DataTierType getTier() {
        return DataTierType.FROZEN;
    }

    @Override
    public String getType() {
        return "FROZEN";
    }

    @Override
    public Period indexLifetimeMax() {
        return null;
    }

    @Override
    public Period indexLifetimeMin() {
        return null;
    }
}

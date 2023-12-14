/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.views.search.engine.monitoring.data.histogram.creation;

import org.graylog.plugins.views.search.engine.monitoring.data.histogram.Histogram;
import org.graylog.plugins.views.search.engine.monitoring.data.histogram.MultiValueBin;
import org.graylog.plugins.views.search.engine.monitoring.data.histogram.NamedBinDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class TimerangeHistogramCreation<T, S> {

    static final String OUTSIDE_AVAILABLE_BINS_BIN_NAME = "Longer periods";
    static final String TIMERANGE = "Timerange";

    private final BinChooser<T, S> binChooser;
    private final List<T> availableBins;

    private final Map<String, BiFunction<Collection<S>, Integer, Number>> valueFunctions;

    private final List<String> schema = new ArrayList<>(4);

    public TimerangeHistogramCreation(final Collection<T> availableBins,
                                      final BinChooser<T, S> binChooser,
                                      final Map<String, BiFunction<Collection<S>, Integer, Number>> valueFunctions) {
        this.availableBins = new ArrayList<>(availableBins);
        this.binChooser = binChooser;
        this.binChooser.getBinComparator().ifPresent(this.availableBins::sort);
        this.valueFunctions = valueFunctions;
        this.schema.add(TIMERANGE);
        this.schema.addAll(valueFunctions.keySet());
    }

    public Histogram create(final Collection<S> executionStats) {

        int numTotalStats = executionStats.size();

        Map<T, Collection<S>> separatedInBins = new LinkedHashMap<>();
        availableBins.forEach(p -> separatedInBins.put(p, new LinkedList<>()));
        Collection<S> overBiggestBin = new LinkedList<>();
        executionStats.forEach(queryExecutionStats -> {
            final Optional<T> chosenBin = binChooser.chooseBin(availableBins, queryExecutionStats);
            chosenBin.ifPresentOrElse(
                    per -> separatedInBins.get(per).add(queryExecutionStats),
                    () -> overBiggestBin.add(queryExecutionStats)
                    );
                }
        );

        final List<MultiValueBin<NamedBinDefinition>> bins = separatedInBins.entrySet().stream()
                .map(entry -> entryToBin(entry.getKey().toString(), entry.getValue(), numTotalStats))
                .collect(Collectors.toCollection(ArrayList::new));
        bins.add(entryToBin(OUTSIDE_AVAILABLE_BINS_BIN_NAME, overBiggestBin, numTotalStats));
        return new Histogram(schema, bins);
    }


    private MultiValueBin<NamedBinDefinition> entryToBin(final String binName,
                                                         final Collection<S> executionStats,
                                                         final int numTotalStats) {
        final NamedBinDefinition binDefinition = new NamedBinDefinition(binName);
        final List<Number> values = valueFunctions.values().stream().map(func -> func.apply(executionStats, numTotalStats)).toList();
        return new MultiValueBin<>(binDefinition, values);
    }
}

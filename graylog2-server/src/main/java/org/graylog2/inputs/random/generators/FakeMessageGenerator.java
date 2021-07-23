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
package org.graylog2.inputs.random.generators;

import com.google.common.primitives.Ints;
import org.graylog2.plugin.configuration.Configuration;

import java.util.List;
import java.util.Random;

import static org.graylog2.inputs.transports.RandomMessageTransport.CK_SOURCE;

public abstract class FakeMessageGenerator {
    protected static final Random RANDOM = new Random();
    protected static final int MAX_WEIGHT = 50;
    protected long msgSequenceNumber;
    protected String source;

    public FakeMessageGenerator(Configuration configuration) {
        this.source = configuration.getString(CK_SOURCE);
        this.msgSequenceNumber = 1;
    }

    public GeneratorState generateState() {
        throw(new AbstractMethodError("Needs to be implemented"));
    }

    public static int rateDeviation(int val, int maxDeviation, Random rand) {
        final int deviationPercent = rand.nextInt(maxDeviation);
        final double x = val / 100.0 * deviationPercent;

        // Add or substract?
        final double result;
        if (rand.nextBoolean()) {
            result = val - x;
        } else {
            result = val + x;
        }

        if (result < 0) {
            return 1;
        } else {
            return Ints.saturatedCast(Math.round(result));
        }
    }

    protected Weighted getWeighted(List<? extends Weighted> list) {
        while (true) {
            int x = RANDOM.nextInt(MAX_WEIGHT);
            Weighted obj = list.get(RANDOM.nextInt(list.size()));

            if (obj.getWeight() >= x) {
                return obj;
            }
        }
    }

    protected static abstract class Weighted {

        protected final int weight;

        protected Weighted(int weight) {
            if (weight <= 0 || weight > MAX_WEIGHT) {
                throw new RuntimeException("Invalid resource weight: " + weight);
            }

            this.weight = weight;
        }

        public int getWeight() {
            return weight;
        }
    }

    public static class GeneratorState {

    }
}

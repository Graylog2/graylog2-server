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
package org.graylog.plugins.pipelineprocessor.functions.strings;

import java.util.Map;
import java.util.stream.Collectors;

/*
 * Shannon Entropy is a common method for quantifying the randomness/sameness of the types withing a collection of
 * entities. If entities are all the same type, then the entropy is 0. As more and more types are introduced,
 * the entropy increases. This Java implementation applies a Shannon Entropy calculation to characters within a
 * string.
 * <br>
 * Examples:
 * - Input: 1111, Entropy: 0
 * - Input: 5555555555, Entropy: 0.0
 * - Input: 5555555555, Entropy: 0.0
 * - Input: 1555555555, Entropy: 0.47
 * - Input: 1111155555, Entropy: 1.0
 * - Input: 1234567890, Entropy: 3.32
 * - Input: 1234567890qwertyuiopasdfghjklzxcvbnm, Entropy: 5.17
 */
public class ShannonEntropy {

    /**
     * Calculate Shannon Entropy for the characters in a string.
     *
     * @param input The input string.
     * @return a double representing the entropy.
     */
    public static double calculateForChars(String input) {
        final Map<Character, Long> charCountMap = input.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.groupingBy(p -> p, Collectors.counting()));

        double result = 0;
        for (Character c : charCountMap.keySet()) {
            double probabilityForChar = (double) charCountMap.get(c) / input.length();
            result += probabilityForChar * logBaseTwo(1 / probabilityForChar);
        }
        return result;
    }

    private static double logBaseTwo(double input) {
        return Math.log(input) / Math.log(2);
    }
}

/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.graylog2.inputs.random.generators;

import java.util.Random;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Tools {

    public static int deviation(int val, int maxDeviation, Random rand) {
        int deviationPercent = rand.nextInt(maxDeviation);

        double x = val/100.0*deviationPercent;

        // Add or substract?
        double result = 0;
        if (rand.nextBoolean()) {
            result = val-x;
        } else {
            result = val+x;
        }

        if (result < 0) {
            return 1;
        } else {
            return Math.round((int) result);
        }
    }

}

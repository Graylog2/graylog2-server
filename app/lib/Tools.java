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
package lib;

import java.util.Random;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Tools {

    private Tools() { /* pure utility class */ }

    public static int random(int min, int max) {
        Random rand = new Random();
        return rand.nextInt(max - min + 1) + min;
    }

    public static Object removeTrailingNewline(Object x) {
        if (x == null) {
            return x;
        }

        if (x instanceof String) {
            String s = (String) x;
            if (s.endsWith("\n") || s.endsWith("\r")) {
                return s.substring(0, s.length()-1);
            } else {
                return x;
            }
        } else {
            return x;
        }
    }

}

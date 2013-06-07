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
package org.graylog2.streams.matchers;

import com.google.common.primitives.Ints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class MatcherBase {

    private static final Logger LOG = LoggerFactory.getLogger(MatcherBase.class);

    /**
     * Convert something to an int in a fast way having a good guess
     * that it is an int. This is perfect for MongoDB data that *should*
     * have been stored as integers already so there is a high probability
     * of easy converting.
     *
     * @param x The object to convert to an int
     * @return Converted object, 0 if empty or something went wrong.
     */
    public static Integer getInt(Object x) {
        if (x == null) {
            return null;
        }

        if (x instanceof Integer) {
            return (Integer) x;
        }

        if (x instanceof String) {
            String s = x.toString();
            if (s == null || s.isEmpty()) {
                return null;
            }
        }

        /*
         * This is the last and probably expensive fallback. This should be avoided by
         * only passing in Integers, Longs or stuff that can be parsed from it's String
         * representation. You might have to build cached objects that did a safe conversion
         * once for example. There is no way around for the actual values we compare if the
         * user sent them in as non-numerical type.
         */
        LOG.debug("Warning: Trying to convert a <{}> to int. This should be avoided.", x.getClass().getCanonicalName());
        Integer result = Ints.tryParse(x.toString());
        if (result == null) {
            return null;
        } else {
            return result;
        }
    }

}

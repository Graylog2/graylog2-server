/*
 * Copyright 2012-2014 TORCH GmbH
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
 */

package org.graylog2.plugin.streams;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public enum StreamRuleType {
    EXACT {
        @Override
        public Integer toInteger() {
            return 1;
        }
    },
    GREATER {
        @Override
        public Integer toInteger() {
            return 3;
        }
    },
    SMALLER {
        @Override
        public Integer toInteger() {
            return 4;
        }
    },
    REGEX {
        @Override
        public Integer toInteger() {
            return 2;
        }
    },
    PRESENCE {
        @Override
        public Integer toInteger() {
            return 5;
        }
    };

    public abstract Integer toInteger();

    public static StreamRuleType fromInteger(int numberic) {
        switch (numberic) {
            case 1:
                return EXACT;
            case 2:
                return REGEX;
            case 3:
                return GREATER;
            case 4:
                return SMALLER;
            case 5:
                return PRESENCE;
            default:
                return null;
        }
    }
}

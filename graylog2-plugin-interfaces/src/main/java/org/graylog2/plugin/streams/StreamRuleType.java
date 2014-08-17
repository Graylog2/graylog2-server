/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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

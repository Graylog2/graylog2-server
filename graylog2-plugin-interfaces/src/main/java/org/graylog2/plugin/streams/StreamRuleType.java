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

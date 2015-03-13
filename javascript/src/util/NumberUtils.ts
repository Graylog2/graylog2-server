'use strict';

var NumberUtils = {
    normalizeNumber(number) {
        switch (number) {
            case "NaN":
                return NaN;
            case "Infinity":
                return Number.MAX_VALUE;
            case "-Infinity":
                return Number.MIN_VALUE;
            default:
                return number;
        }
    }
};

export = NumberUtils;
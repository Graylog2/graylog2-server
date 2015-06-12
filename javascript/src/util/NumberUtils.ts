/// <reference path="../../declarations/node/node.d.ts" />

'use strict';

var numeral = require('numeral');

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
    },
    normalizeGraphNumber(number) {
        switch (number) {
            case "NaN":
            case "Infinity":
            case "-Infinity":
                return 0;
            default:
                return number;
        }
    },
    formatNumber(number) {
        try {
            return numeral(NumberUtils.normalizeNumber(number)).format('0,0.[00]');
        } catch(e) {
            return number;
        }
    },
    formatPercentage(percentage) {
        try {
            return numeral(NumberUtils.normalizeNumber(percentage)).format("0.00%");
        } catch (e) {
            return percentage;
        }
    },
};

export = NumberUtils;
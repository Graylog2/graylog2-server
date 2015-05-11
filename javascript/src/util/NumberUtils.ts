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
    formatNumber(number) {
        try {
            return numeral(NumberUtils.normalizeNumber(number)).format('0,0.[00]');
        } catch(e) {
            return number;
        }
    }
};

export = NumberUtils;
'use strict';

declare var $: any;

import UserNotification = require("../../util/UserNotification");

var DEFAULT_MAX_DATA_POINTS = 4000;
var HISTOGRAM_URL = '/a/search/histogram';

var HistogramDataStore = {
    loadHistogramData(range: number, sourceNames: Array<string>, maxDataPoints: number, callback: (histogramData: any) => void) {
        var url = HISTOGRAM_URL;
        if (typeof maxDataPoints === 'undefined') {
            maxDataPoints = DEFAULT_MAX_DATA_POINTS;
        }
        url += "?maxDataPoints=" + maxDataPoints;
        var q = "";
        if (typeof sourceNames !== 'undefined' && sourceNames instanceof Array) {
            q = encodeURIComponent(sourceNames.map((source) => "source:" + source).join(" OR "));
        }
        if (typeof range !== 'undefined') {
            var interval = 'minute';
            var rangeAsNumber = Number(range);
            if (rangeAsNumber >= 365 * 24 * 60 * 60 || rangeAsNumber === 0) {
                // for years and all interval will be day
                interval = 'day';
            } else if (rangeAsNumber >= 31 * 24 * 60 * 60) {
                // for months interval will be day
                interval = 'hour';
            }
            url += "&q=" + q + "&rangetype=relative&relative=" + range + "&interval=" + interval;
        }
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.warning("Loading of histogram data failed with status: " + errorThrown,
                "Could not load histogram data");
        };
        $.getJSON(url, callback).fail(failCallback);
    }
};

export = HistogramDataStore;

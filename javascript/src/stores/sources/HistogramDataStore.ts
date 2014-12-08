declare var $: any;

var DEFAULT_MAX_DATA_POINTS = 4000;
var HISTOGRAM_URL = '/a/search/histogram';

var HistogramDataStore = {
    loadHistogramData(range, sourceNames, maxDataPoints, callback) {
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
            url += "&q=" + q + "&rangetype=relative&relative=" +  range + "&interval=" + interval;
        }
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            console.error("Loading of histogram data failed with status: " + textStatus);
            console.error("Error", errorThrown);
        };
        $.getJSON(url, callback).fail(failCallback);
    }
};

export = HistogramDataStore;

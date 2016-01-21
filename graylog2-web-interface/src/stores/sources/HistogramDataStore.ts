import $ = require('jquery');
import UserNotification = require('util/UserNotification');
const URLUtils = require('util/URLUtils');
const fetch = require('logic/rest/FetchProvider').default;

const DEFAULT_MAX_DATA_POINTS = 4000;

// search/histogram?maxDataPoints=1680&q=&rangetype=relative&relative=3600&interval=minute

const HistogramDataStore = {
    HISTOGRAM_URL: URLUtils.appPrefixed('/search/universal/relative/histogram'),
    loadHistogramData(range: number, sourceNames: Array<string>, maxDataPoints: number, callback: (histogramData: any) => void) {
        var url = URLUtils.qualifyUrl(this.HISTOGRAM_URL);
        // TODO: Handle max data points
        // if (typeof maxDataPoints === 'undefined') {
        //     maxDataPoints = DEFAULT_MAX_DATA_POINTS;
        // }
        // url += "?maxDataPoints=" + maxDataPoints;

        var query = "";
        if (typeof sourceNames !== 'undefined' && sourceNames instanceof Array) {
            query = encodeURIComponent(sourceNames.map((source) => "source:" + source).join(" OR "));
        } else {
            query = "*";
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
            url += "?query=" + query + "&range=" + range + "&interval=" + interval;
        }
        fetch('GET', url)
            .then(response => {
                const formattedResults = $.map(response.results, (value, timestamp) => {
                    return {x: Number(timestamp), y: value};
                })

                response.values = formattedResults;
                return response;
            })
            .then(callback)
            .catch((errorThrown) => {
                UserNotification.warning("Loading of histogram data failed with status: " + errorThrown,
                    "Could not load histogram data");
            });
    }
};

export = HistogramDataStore;

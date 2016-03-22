/// <reference path="../../../declarations/bluebird/bluebird.d.ts" />
/// <reference path='../../../node_modules/immutable/dist/immutable.d.ts'/>
/// <reference path="../../../declarations/node/node.d.ts" />

const fetch = require('logic/rest/FetchProvider').default;
import Immutable = require('immutable');
import ApiRoutes = require('routing/ApiRoutes');
const URLUtils = require('util/URLUtils');
const UserNotification = require('util/UserNotification');

const StoreProvider = require('injection/StoreProvider');
const SearchStore = StoreProvider.getStore('Search');

const FieldStatisticsStore = {
    FUNCTIONS: Immutable.OrderedMap({
        count: 'Total',
        mean: 'Mean',
        min: 'Minimum',
        max: 'Maximum',
        std_deviation: 'Std. deviation',
        variance: 'Variance',
        sum: 'Sum',
        cardinality: 'Cardinality',
    }),
    getFieldStatistics(field: string): Promise<string[]> {
        var originalSearchURLParams = SearchStore.getOriginalSearchURLParams();
        var streamId = SearchStore.searchInStream ? SearchStore.searchInStream.id : null;

        var rangeType = originalSearchURLParams.get('rangetype');
        var timerange = {};
        switch(rangeType) {
            case 'relative':
                timerange['range'] = originalSearchURLParams.get('relative');
                break;
            case 'absolute':
                timerange['from'] = originalSearchURLParams.get('from');
                timerange['to'] = originalSearchURLParams.get('to');
                break;
            case 'keyword':
                timerange['keyword'] = originalSearchURLParams.get('keyword');
                break;
        }

        var url = ApiRoutes.UniversalSearchApiController.fieldStats(
            rangeType,
            originalSearchURLParams.get('q') || '*',
            field,
            timerange,
            streamId
        ).url;

        url = URLUtils.qualifyUrl(url);

        var promise = fetch('GET', url);
        promise.catch(error => {
            UserNotification.error("Loading field statistics failed with status: " + error,
                "Could not load field statistics");
        });

        return promise;
    }
};

module.exports = FieldStatisticsStore;

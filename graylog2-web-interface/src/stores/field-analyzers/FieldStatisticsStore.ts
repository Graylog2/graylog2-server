/// <reference path="../../../declarations/bluebird/bluebird.d.ts" />
/// <reference path='../../../node_modules/immutable/dist/immutable.d.ts'/>
/// <reference path="../../../declarations/node/node.d.ts" />

const fetch = require('logic/rest/FetchProvider').default;
import Immutable = require('immutable');
import jsRoutes = require('routing/jsRoutes');
const URLUtils = require('util/URLUtils');
const UserNotification = require('util/UserNotification');
import SearchStore = require('stores/search/SearchStore');
const Qs = require('qs');

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

        var url = jsRoutes.controllers.api.UniversalSearchApiController.fieldStats(
            rangeType,
            originalSearchURLParams.get('q') || '*',
            field,
            Qs.stringify(timerange),
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

export default FieldStatisticsStore;

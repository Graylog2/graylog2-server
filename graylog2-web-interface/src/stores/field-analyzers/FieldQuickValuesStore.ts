/// <reference path="../../../declarations/bluebird/bluebird.d.ts" />

const fetch = require('logic/rest/FetchProvider').default;
import SearchStore = require('stores/search/SearchStore');
const UserNotification = require('util/UserNotification');
import jsRoutes = require('routing/jsRoutes');
const URLUtils = require('util/URLUtils');
const Qs = require('qs');

const FieldQuickValuesStore = {
    getQuickValues(field: string): Promise<string[]> {
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

        var url = jsRoutes.controllers.api.UniversalSearchApiController.fieldTerms(
            rangeType,
            originalSearchURLParams.get('q') || '*',
            field,
            Qs.stringify(timerange),
            streamId
        ).url;

        url = URLUtils.qualifyUrl(url);

        var promise = fetch('GET', url);
        promise.catch(error => {
            UserNotification.error('Loading quick values failed with status: ' + error,
                'Could not load quick values');
        });

        return promise;
    },
};

export default FieldQuickValuesStore;

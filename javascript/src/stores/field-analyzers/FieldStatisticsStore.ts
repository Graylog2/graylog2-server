/// <reference path="../../../declarations/jquery/jquery.d.ts" />
/// <reference path='../../../node_modules/immutable/dist/immutable.d.ts'/>

'use strict';

declare var $: any;
declare var jsRoutes: any;

import Immutable = require('immutable');

import UserNotification = require("../../util/UserNotification");
import SearchStore = require('../search/SearchStore');

var FieldStatisticsStore = {
    FUNCTIONS: Immutable.OrderedMap({
        count: 'Total',
        mean: 'Mean',
        min: 'Minimum',
        max: 'Maximum',
        std_deviation: 'Std. deviation',
        variance: 'Variance',
        sum: 'Sum',
    }),
    getFieldStatistics(field: string): JQueryPromise<string[]> {
        var originalSearchURLParams = SearchStore.getOriginalSearchURLParams();
        var searchInStream = SearchStore.searchInStream;
        var streamId = searchInStream === undefined ? null : searchInStream.id;

        var url = jsRoutes.controllers.api.SearchApiController.fieldStats(
            originalSearchURLParams.get('q'),
            field,
            originalSearchURLParams.get('rangetype'),
            originalSearchURLParams.get('relative'),
            originalSearchURLParams.get('from'),
            originalSearchURLParams.get('to'),
            originalSearchURLParams.get('keyword'),
            streamId
        ).url;

        var promise = $.getJSON(url);
        promise.fail((jqXHR, textStatus, errorThrown) => {
            if (jqXHR.status === 400) {
                UserNotification.error("Field statistics are only available for numeric fields",
                    "Could not load field statistics");
            } else {
                UserNotification.error("Loading field statistics failed with status: " + errorThrown,
                    "Could not load field statistics");
            }
        });

        return promise;
    }
};

export = FieldStatisticsStore;

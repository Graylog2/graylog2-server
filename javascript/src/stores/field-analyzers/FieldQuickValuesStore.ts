/// <reference path="../../../declarations/jquery/jquery.d.ts" />

'use strict';

declare
var $: any;
declare
var jsRoutes: any;

import SearchStore = require('../search/SearchStore');
import UserNotification = require('../../util/UserNotification');

var FieldQuickValuesStore = {
    getQuickValues(field: string): JQueryPromise<string[]> {
        var originalSearchURLParams = SearchStore.getOriginalSearchURLParams();

        var url = jsRoutes.controllers.api.SearchApiController.fieldTerms(
            originalSearchURLParams.get('q'),
            field,
            originalSearchURLParams.get('rangetype'),
            originalSearchURLParams.get('relative'),
            originalSearchURLParams.get('from'),
            originalSearchURLParams.get('to'),
            originalSearchURLParams.get('keyword')
        ).url;

        var promise = $.getJSON(url);
        promise.fail((jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Loading quick values failed with status: " + errorThrown,
                "Could not load quick values");
        });

        return promise;
    }
};

export = FieldQuickValuesStore;

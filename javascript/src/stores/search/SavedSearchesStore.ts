/// <reference path="../../../declarations/jquery/jquery.d.ts" />

'use strict';

declare
var $: any;
declare
var jsRoutes: any;

import SearchStore = require('../search/SearchStore');
import UserNotification = require("../../util/UserNotification");

var SavedSearchesStore = {
    create(title: string): JQueryPromise<string[]> {
        var originalSearchParams = SearchStore.getOriginalSearchParamsWithFields();
        var queryParams = originalSearchParams.set('rangeType', originalSearchParams.get('range_type')).delete('range_type');
        var params = {title: title, query: queryParams.toJS()};

        var url = jsRoutes.controllers.api.SavedSearchesApiController.create().url;
        var promise = $.ajax({
            type: "POST",
            url: url,
            data: JSON.stringify(params),
            dataType: 'json',
            contentType: 'application/json'
        });

        promise.done(() => UserNotification.success("Search criteria saved as '" + title + "'."));
        promise.fail((jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Saving search criteria failed with status: " + errorThrown,
                "Could not save search criteria");
        });

        return promise;
    }
};

export = SavedSearchesStore;
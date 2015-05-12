/// <reference path="../../../declarations/jquery/jquery.d.ts" />
/// <reference path='../../../node_modules/immutable/dist/immutable.d.ts'/>

'use strict';

declare var $: any;
declare var jsRoutes: any;

import Immutable = require('immutable');

import SearchStore = require('../search/SearchStore');
import UserNotification = require("../../util/UserNotification");
import URLUtils = require('../../util/URLUtils');

interface SavedSearch {
    id: string;
    title: string;
}

class SavedSearchesStore {
    private _savedSearches: Immutable.List<SavedSearch>;
    private _onSavedSearchesChanged: {(savedSearches: Immutable.List<SavedSearch>): void; }[] = [];

    constructor() {
        this._savedSearches = Immutable.List<SavedSearch>();
        this._updateSavedSearches();
    }

    get savedSearches(): Immutable.List<SavedSearch> {
        return this._savedSearches;
    }

    set savedSearches(newSavedSearches: Immutable.List<SavedSearch>) {
        this._savedSearches = newSavedSearches;
        this.onSavedSearchesChanged.forEach((callback) => callback(newSavedSearches));
    }

    get onSavedSearchesChanged(): {(savedSearches: Immutable.List<SavedSearch>): void}[] {
        return this._onSavedSearchesChanged;
    }

    addOnSavedSearchesChangedListener(savedSearchesCallback: (savedSearches: Immutable.List<SavedSearch>) => void) {
        this._onSavedSearchesChanged.push(savedSearchesCallback);
    }

    _updateSavedSearches() {
        var promise = this.list();
        promise.done((savedSearches) => this.savedSearches = Immutable.List<SavedSearch>(savedSearches));
    }

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

        promise.done(() => {
            UserNotification.success("Search criteria saved as '" + title + "'.");
            this._updateSavedSearches();
        });
        promise.fail((jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Saving search criteria failed with status: " + errorThrown,
                "Could not save search criteria");
        });

        return promise;
    }

    list(): JQueryPromise<string[]> {
        var url = jsRoutes.controllers.api.SavedSearchesApiController.list().url;
        var promise = $.getJSON(url);

        promise.fail((jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Fetching saved searches failed with status: " + errorThrown,
                "Could not get saved searches");
        });

        return promise;
    }

    execute(searchId: string, streamId?: string, width?: string) {
        var url = jsRoutes.controllers.SavedSearchesController.execute(searchId, streamId, width).url;
        URLUtils.openLink(url, false);
    }
}

var savedSearchesStore = new SavedSearchesStore();
export = savedSearchesStore;
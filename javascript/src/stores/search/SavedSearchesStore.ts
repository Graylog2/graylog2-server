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

    _createOrUpdate(title: string, stringId?: string): JQueryPromise<string[]> {
        var originalSearchParams = SearchStore.getOriginalSearchParamsWithFields();
        var queryParams = originalSearchParams.set('rangeType', originalSearchParams.get('range_type')).delete('range_type');
        var params = {title: title, query: queryParams.toJS()};

        var url;
        var verb;

        if (typeof stringId === 'undefined') {
            url = jsRoutes.controllers.api.SavedSearchesApiController.create().url;
            verb = 'POST';
        } else {
            url = jsRoutes.controllers.api.SavedSearchesApiController.update(stringId).url;
            verb = 'PUT';
        }

        return $.ajax({
            type: verb,
            url: url,
            data: JSON.stringify(params),
            dataType: 'json',
            contentType: 'application/json'
        });
    }

    create(title: string): JQueryPromise<string[]> {
        var promise = this._createOrUpdate(title);

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

    update(stringId: string, title: string): JQueryPromise<string[]> {
        var promise = this._createOrUpdate(title, stringId);

        promise.done(() => {
            UserNotification.success("Saved search '" + title + "' was updated.");
            this._updateSavedSearches();
        });
        promise.fail((jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Updating saved search '" + title + "' failed with status: " + errorThrown,
                "Could not update saved search");
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

    getSavedSearch(searchId: string): SavedSearch {
        return this.savedSearches.find((savedSearch) => savedSearch.id === searchId);
    }

    isValidTitle(searchId: string, title: string): boolean {
        return !this.savedSearches.some((savedSearch) => savedSearch.id !== searchId && savedSearch.title === title);
    }

    execute(searchId: string, streamId?: string, width?: string) {
        var url = jsRoutes.controllers.SavedSearchesController.execute(searchId, streamId, width).url;
        URLUtils.openLink(url, false);
    }

    delete(searchId: string): JQueryPromise<string[]> {
        var url = jsRoutes.controllers.SavedSearchesController.delete(searchId).url;
        var promise = $.ajax({
            type: 'DELETE',
            url: url
        });

        promise.done(() => {
            UserNotification.success("Saved search '" + this.savedSearches[searchId] + "' was deleted successfully.");
            $(document).trigger('deleted.graylog.saved-search', {savedSearchId: searchId});
        });
        promise.fail((jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Deleting saved search '" + this.savedSearches[searchId] + "' failed with status: " + errorThrown,
                "Could not delete saved search");
        });

        return promise;
    }
}

var savedSearchesStore = new SavedSearchesStore();
export = savedSearchesStore;
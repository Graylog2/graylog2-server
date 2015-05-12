/// <reference path="../../../declarations/jquery/jquery.d.ts" />
/// <reference path="../../../declarations/node/node.d.ts" />
/// <reference path='../../../node_modules/immutable/dist/immutable.d.ts'/>

'use strict';

declare
var jsRoutes: any;

import Immutable = require('immutable');
var Qs = require('qs');
var URLUtils = require('../../util/URLUtils');

class SearchStore {
    static NOT_OPERATOR = "NOT";
    static OR_OPERATOR = "OR";
    static AND_OPERATOR = "AND";

    private _query: string;
    private _rangeType: string;
    private _rangeParams: Immutable.Map<string, any>;
    private _page: number;
    private _resolution: string;
    private _fields: Immutable.Set<string>;
    originalSearch: Immutable.Map<string, any>;
    onParamsChanged: (query: Object)=>void;
    onSubmitSearch: ()=>void;

    constructor() {
        var parsedSearch = Immutable.Map<string, any>(URLUtils.getParsedSearch(window.location));
        this.originalSearch = SearchStore._initializeOriginalSearch(parsedSearch);
        this.query = this.originalSearch.get('query');
        this.rangeType = this.originalSearch.get('rangeType');
        this.rangeParams = this.originalSearch.get('rangeParams');
        this.page = this.originalSearch.get('page');
        this.resolution = this.originalSearch.get('resolution');

        $(document).on('add-search-term.graylog.search', this._addSearchTerm.bind(this));
        $(document).on('get-original-search.graylog.search', this._getOriginalSearchRequest.bind(this));
        $(document).on('change-timerange.graylog.search', this._changeTimeRange.bind(this));
        $(document).on('execute.graylog.search', this._submitSearch.bind(this));
    }

    initializeFieldsFromHash() {
        var parsedSearch = Immutable.Map<string, any>(URLUtils.getParsedSearch(window.location));
        var parsedHash = Immutable.Map<string, any>(URLUtils.getParsedHash(window.location));
        var fieldsFromHash = parsedHash.get('fields');
        var fieldsFromQuery = parsedSearch.get('fields');
        if (fieldsFromHash === undefined) {
            // no hash value, fall back to query if present
            if (fieldsFromQuery === undefined) {
                // neither hash nor query set, fall back to defaults
                this.fields = Immutable.Set<string>(['message', 'source']);
            } else {
                this.fields = Immutable.Set<string>(fieldsFromQuery.split(','));
            }
        } else {
            // hash value, if present, always wins
            this.fields = Immutable.Set<string>(fieldsFromHash.split(','));
        }
    }

    get query(): string {
        return this._query;
    }

    set query(newQuery: string) {
        this._query = newQuery;
        if (this.onParamsChanged !== undefined) {
            this.onParamsChanged(this.getParams());
        }
    }

    get page(): number {
        return this._page;
    }

    set page(newPage: number) {
        if (this._page === undefined) {
            this._page = newPage;
        } else {
            this._reloadSearchWithNewParam('page', newPage);
        }
    }

    get rangeType(): string {
        return this._rangeType;
    }

    set rangeType(newRangeType: string) {
        this._rangeType = newRangeType;
        this.rangeParams = (this.originalSearch.get('rangeType') === newRangeType) ? this.originalSearch.get('rangeParams') : Immutable.Map<string, any>();

        if (this.onParamsChanged !== undefined) {
            this.onParamsChanged(this.getParams());
        }
    }

    get rangeParams(): Immutable.Map<string, any> {
        return this._rangeParams;
    }

    set rangeParams(value: Immutable.Map<string, any>) {
        this._rangeParams = value;
        if (this.onParamsChanged !== undefined) {
            this.onParamsChanged(this.getParams());
        }
    }

    get resolution(): string {
        return this._resolution;
    }

    set resolution(newResolution: string) {
        if (this._resolution === undefined) {
            this._resolution = newResolution;
        } else {
            this._reloadSearchWithNewParam('interval', newResolution);
        }
    }

    get fields(): Immutable.Set<string> {
        return this._fields;
    }

    set fields(newFields: Immutable.Set<string>) {
        URLUtils.replaceHashParam('fields', newFields.join(','));
        this._fields = newFields;
    }

    static _initializeOriginalSearch(parsedSearch: Immutable.Map<string, any>): Immutable.Map<string, any> {
        var originalSearch = Immutable.Map<string, any>();
        originalSearch = originalSearch.set('query', parsedSearch.get('q', ''));
        originalSearch = originalSearch.set('resolution', parsedSearch.get('interval'));
        originalSearch = originalSearch.set('page', parsedSearch.get('page', 1));
        originalSearch = originalSearch.set('rangeType', parsedSearch.get('rangetype', 'relative'));
        var rangeParams;

        switch (originalSearch.get('rangeType')) {
            case 'relative':
                rangeParams = Immutable.Map<string, any>({relative: Number(parsedSearch.get('relative', 5 * 60))});
                break;
            case 'absolute':
                rangeParams = Immutable.Map<string, any>({
                    from: parsedSearch.get('from', null),
                    to: parsedSearch.get('to', null)
                });
                break;
            case 'keyword':
                rangeParams = Immutable.Map<string, any>({keyword: parsedSearch.get('keyword', '')});
                break;
            default:
                throw('Unsupported range type ' + originalSearch.get('rangeType'));
        }

        return originalSearch.set('rangeParams', rangeParams);
    }

    _addSearchTerm(event, data) {
        var term = data.hasOwnProperty('field') ? data.field + ":" : "";
        term += SearchStore.escape(data.value);
        this.addQueryTerm(term, SearchStore.AND_OPERATOR);
    }

    _getOriginalSearchRequest(event, data) {
        data.callback(this.getOriginalSearchParams());
    }

    _changeTimeRange(event, data) {
        var newRangeType = data['rangeType'];
        var newRangeParams = Immutable.Map<string, any>(data['rangeParams']);

        this.rangeType = newRangeType;
        this.rangeParams = newRangeParams;
    }

    _submitSearch(event) {
        if (this.onSubmitSearch !== undefined) {
            this.onSubmitSearch();
        }
    }

    static escape(source) {
        // Escape all lucene special characters from the source: && || : \ / + - ! ( ) { } [ ] ^ " ~ * ?
        return String(source).replace(/(&&|\|\||[\:\\\/\+\-\!\(\)\{\}\[\]\^\"\~\*\?])/g, "\\$&");
    }

    queryContainsTerm(termInQuestion: string): boolean {
        return this.query.indexOf(termInQuestion) != -1;
    }

    addQueryTerm(term: string, operator: string): string {
        if (this.queryContainsTerm(term)) {
            return;
        }
        var newQuery = "";
        if (typeof operator !== 'undefined' && this.query !== "") {
            newQuery = this.query + " " + operator + " ";
        }
        newQuery += term;
        this.query = newQuery;
    }

    getParams(): Object {
        return {
            query: this.query,
            rangeType: this.rangeType,
            rangeParams: this.rangeParams
        };
    }

    // Get initial search params, with names used in AJAX requests
    getOriginalSearchParams(): Immutable.Map<string,any> {
        var orignalParams = Immutable.Map<string, any>();
        orignalParams = orignalParams.set('range_type', this.originalSearch.get('rangeType'));
        orignalParams = orignalParams.merge(this.originalSearch.get('rangeParams'));
        orignalParams = orignalParams.set('query', this.originalSearch.get('query'));
        orignalParams = orignalParams.set('interval', this.originalSearch.get('resolution'));

        return orignalParams;
    }

    // Get initial search params with the current selected fields
    getOriginalSearchParamsWithFields(): Immutable.Map<string,any> {
        var orignalParams = Immutable.Map<string, any>();
        orignalParams = orignalParams.set('range_type', this.originalSearch.get('rangeType'));
        orignalParams = orignalParams.merge(this.originalSearch.get('rangeParams'));
        orignalParams = orignalParams.set('query', this.originalSearch.get('query'));
        orignalParams = orignalParams.set('interval', this.originalSearch.get('resolution'));
        orignalParams = orignalParams.set('fields', this.fields.join(','));

        return orignalParams;
    }

    // Get initial search params, with the names used in a search URL request
    getOriginalSearchURLParams(): Immutable.Map<string, any> {
        var simplifiedParams = Immutable.Map<string, any>();
        simplifiedParams = simplifiedParams.set('rangetype', this.originalSearch.get('rangeType'));
        simplifiedParams = simplifiedParams.merge(this.originalSearch.get('rangeParams'));
        simplifiedParams = simplifiedParams.set('q', this.originalSearch.get('query'));
        simplifiedParams = simplifiedParams.set('interval', this.originalSearch.get('resolution'));
        simplifiedParams = simplifiedParams.set('page', this.originalSearch.get('page'));
        simplifiedParams = simplifiedParams.set('fields', this.fields.join(','));

        return simplifiedParams;
    }

    _reloadSearchWithNewParam(param: string, value: any) {
        var searchURLParams = this.getOriginalSearchURLParams();
        searchURLParams = searchURLParams.set(param, value);
        URLUtils.openLink(jsRoutes.controllers.SearchControllerV2.index().url + "?" + Qs.stringify(searchURLParams.toJS()));
    }
}

var searchStore = new SearchStore();

export = searchStore;

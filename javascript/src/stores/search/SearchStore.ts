/// <reference path="../../../declarations/jquery/jquery.d.ts" />
/// <reference path="../../../declarations/node/node.d.ts" />
/// <reference path='../../../node_modules/immutable/dist/Immutable.d.ts'/>

'use strict';

declare var jsRoutes: any;

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
    originalSearch: Immutable.Map<string, any>;
    onParamsChanged: (query: Object)=>void;

    constructor() {
        var parsedSearch = Immutable.Map<string, any>(URLUtils.getParsedSearch(window.location));
        this.originalSearch = SearchStore._initializeOriginalSearch(parsedSearch);
        this.query = this.originalSearch.get('query');
        this.rangeType = this.originalSearch.get('rangeType');
        this.rangeParams = this.originalSearch.get('rangeParams');

        $(document).on('add-search-term.graylog.universalsearch', this._addSearchTerm.bind(this));
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
        this._page = newPage;
        var searchURLParams = this.getSearchURLParams();
        searchURLParams = searchURLParams.set('page', newPage);
        URLUtils.openLink(jsRoutes.controllers.SearchControllerV2.index().url + "?" + Qs.stringify(searchURLParams.toJS()));
    }

    get rangeType(): string {
        return this._rangeType;
    }

    set rangeType(newRangeType: string) {
        this._rangeType = newRangeType;
        this.rangeParams = Immutable.Map<string, any>();
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

    static _initializeOriginalSearch(parsedSearch: Immutable.Map<string, any>): Immutable.Map<string, any> {
        var originalSearch = Immutable.Map<string, any>();
        originalSearch = originalSearch.set('query', parsedSearch.get('q', ""));
        originalSearch = originalSearch.set('rangeType', parsedSearch.get('rangetype', 'relative'));
        var rangeParams;

        switch (originalSearch.get('rangeType')) {
            case 'relative':
                rangeParams = Immutable.Map<string, any>({relative: Number(parsedSearch.get('relative', 5 * 60))});
                break;
            case 'absolute':
                rangeParams = Immutable.Map<string, any>({from: parsedSearch.get('from', ''), to: parsedSearch.get('to', '')});
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

    static escape(source) {
        // Escape all lucene special characters from the source: && || : \ / + - ! ( ) { } [ ] ^ " ~ * ?
        return source.replace(/(&&|\|\||[\:\\\/\+\-\!\(\)\{\}\[\]\^\"\~\*\?])/g, "\\$&");
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

    getSearchURLParams(): Immutable.Map<string, any> {
        var filteredSearchParams = Immutable.Map<string, any>();
        filteredSearchParams = filteredSearchParams.set('rangetype', this.originalSearch.get('rangeType'));
        filteredSearchParams = filteredSearchParams.merge(this.originalSearch.get('rangeParams'));
        filteredSearchParams = filteredSearchParams.set('q', this.originalSearch.get('query'));

        return filteredSearchParams;
    }
}

var searchStore = new SearchStore();

export = searchStore;

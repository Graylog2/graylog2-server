'use strict';

import * as Immutable from 'immutable';
const Routes = require('routing/Routes');
const Qs = require('qs');
const URLUtils = require('util/URLUtils');
const history = require('util/History');
const DateTime = require('logic/datetimes/DateTime');

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
    private _highlightMessage: string;
    public sortField: string;
    public sortOrder: string;
    public width: number;
    savedSearch: string;
    originalSearch: Immutable.Map<string, any>;
    onParamsChanged: (query: Object)=>void;
    onSubmitSearch: ()=>void;
    onAddQueryTerm: ()=>void;
    searchInStream: any;

    constructor() {
        this.load(true);
        window.addEventListener('resize', () => this.width = window.innerWidth);
    }

    load(firstLoad) {
        var parsedSearch = Immutable.Map<string, any>(URLUtils.getParsedSearch(window.location));
        this.originalSearch = SearchStore._initializeOriginalSearch(parsedSearch);
        if (firstLoad) {
            this.query = this.originalSearch.get('query');
            this.rangeType = this.originalSearch.get('rangeType');
            this.rangeParams = this.originalSearch.get('rangeParams');
            this.page = this.originalSearch.get('page');
            this.resolution = this.originalSearch.get('resolution');
            this.highlightMessage = this.originalSearch.get('highlightMessage');
        } else {
            this._query = this.originalSearch.get('query');
            this._rangeType = this.originalSearch.get('rangeType');
            this._rangeParams = this.originalSearch.get('rangeParams');
            this._page = this.originalSearch.get('page');
            this._resolution = this.originalSearch.get('resolution');
            this._highlightMessage = this.originalSearch.get('highlightMessage');
        }
        this.savedSearch = this.originalSearch.get('saved');
        this.sortField = this.originalSearch.get('sortField');
        this.sortOrder = this.originalSearch.get('sortOrder');
        this.width = window.innerWidth;
    }

    unload() {
        window.removeEventListener('resize', () => this.width = window.innerWidth);
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

    /*
     * This returns the current search query introduced in the search bar. Use "originalQuery()" if you need the
     * query for the last executed search.
     */
    get query(): string {
        return this._query;
    }

    set query(newQuery: string) {
        this._query = newQuery;
        if (this.onParamsChanged !== undefined) {
            this.onParamsChanged(this.getParams());
        }
    }

    get originalQuery(): string {
        const query = this.originalSearch.get('query');
        return (query.length > 0 ? query : '*');
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

    /*
     * This returns the current range type introduced in the search bar. Use "originalRangeType()" if you need the
     * range type for the last executed search.
     */
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

    get originalRangeType(): string {
        return this.originalSearch.get('rangeType');
    }

    /*
     * This returns the current range parameters introduced in the search bar. Use "originalRangeParams()" if you
     * need the range parameters for the last executed search.
     */
    get rangeParams(): Immutable.Map<string, any> {
        return this._rangeParams;
    }

    set rangeParams(value: Immutable.Map<string, any>) {
        this._rangeParams = value;
        if (this.onParamsChanged !== undefined) {
            this.onParamsChanged(this.getParams());
        }
    }

    get originalRangeParams(): Immutable.Map<string, any> {
        return this.originalSearch.get('rangeParams');
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
        // TODO: Add parameters once we know how to handle them
        //URLUtils.replaceHashParam('fields', newFields.join(','));
        this._fields = newFields;
    }

    get highlightMessage(): string {
        return this._highlightMessage;
    }

    set highlightMessage(id: string) {
        this._highlightMessage = id;
    }

    sort(sortField: string, sortOrder: string): void {
        this._reloadSearchWithNewParams(Immutable.Map<string, any>({sortField: sortField, sortOrder: sortOrder}));
    }

    static _initializeOriginalSearch(parsedSearch: Immutable.Map<string, any>): Immutable.Map<string, any> {
        var originalSearch = Immutable.Map<string, any>();
        originalSearch = originalSearch.set('query', parsedSearch.get('q', ''));
        originalSearch = originalSearch.set('resolution', parsedSearch.get('interval'));
        originalSearch = originalSearch.set('page', Math.max(parsedSearch.get('page', 1), 1));
        originalSearch = originalSearch.set('rangeType', parsedSearch.get('rangetype', 'relative'));
        originalSearch = originalSearch.set('sortField', parsedSearch.get('sortField', 'timestamp'));
        originalSearch = originalSearch.set('sortOrder', parsedSearch.get('sortOrder', 'desc'));
        originalSearch = originalSearch.set('highlightMessage', parsedSearch.get('highlightMessage', ''));

        if (parsedSearch.get('saved') !== undefined) {
            originalSearch = originalSearch.set('saved', parsedSearch.get('saved'));
        }

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

    appendToQueryString(query: string, field: string, value: any, operator: any) {
      let effectiveValue = value;
      if (field === 'timestamp') {
        // when the timestamp comes from a quickvalues source, it is a unix timestamp with millis
        // otherwise it is a datetime string in UTC (from a message list detail view)
        if (isNaN(Number(value))) {
          // convert back to UTC because DateTime always gives us the user timezone
          const dateTime = new DateTime(value).toTimeZone('UTC');
          // search expects UTC datetimes
          effectiveValue = dateTime.toString(DateTime.Formats.TIMESTAMP);
        } else {
          // when this comes from quickvalues (stacked or not) the value is a unix millis timestamp and needs to be
          // converted to a UTC datetime string for search
          effectiveValue = new DateTime(new Date(Number(value))).toTimeZone('UTC').toString(DateTime.Formats.TIMESTAMP);
        }
      }
      const term = `${field}:${SearchStore.escape(effectiveValue)}`;
      const effectiveOperator = operator || SearchStore.AND_OPERATOR;
      const newQuery = this.addQueryTerm(query, term, effectiveOperator);

      return newQuery;
    }

    appendFieldQueryObjectToQueryString(query: string, fieldQueryObjects: Array<Array<{field: string, value: string}>>, operator: any) {
      // We transform field query objects like
      //   [
      //     [
      //       { field: 'a', value: 'foo' }
      //       { field: 'b', value: 'bar' }
      //     ],
      //     [
      //       { field: 'c', value: 'baz' }
      //       { field: 'd', value: 'hey' }
      //     ],
      //   ]
      //
      // into a query string that looks like this:
      //
      //   "(a:foo AND b:bar) OR (c:baz AND d:hey)"
      //
      const newQuery = fieldQueryObjects.map((subquery) => {
        const q = subquery.reduce((sq, o) => {
          return this.appendToQueryString(sq, o.field, o.value, SearchStore.AND_OPERATOR);
        }, '');

        return `(${q})`;
      }).join(` ${SearchStore.OR_OPERATOR} `);

      if (query && query !== '' && query !== '*') {
        return `${query} ${operator || SearchStore.AND_OPERATOR} (${newQuery})`;
      } else {
        return newQuery;
      }
    }

    addSearchTerm(field, value, operator) {
        const newQuery = this.appendToQueryString(this.query, field, value, operator);

        if (this.query !== newQuery) {
          this.query = newQuery;
          if (this.onAddQueryTerm !== undefined) {
            this.onAddQueryTerm();
          }
        }
    }

    addSearchTermWithMapping(mapping, field, value, operator) {
        if (!mapping[value]) {
          return this.addSearchTerm(field, value, operator);
        }

        mapping[value].forEach((m) => {
          this.addSearchTerm(m.field, m.value, operator);
        });
    }

    changeTimeRange(newRangeType: string, newRangeParams: Object) {
        this.rangeType = newRangeType;
        this.rangeParams = Immutable.fromJS(newRangeParams);
    }

    _submitSearch(event) {
        if (this.onSubmitSearch !== undefined) {
            this.onSubmitSearch();
        }
    }

    savedSearchDeleted(savedSearchId) {
        if (savedSearchId === this.savedSearch) {
            this._submitSearch(null);
        }
    }

    static isPhrase(searchTerm) {
        return String(searchTerm).indexOf(" ") !== -1;
    }

    static escape(searchTerm) {
        var escapedTerm = String(searchTerm);

        // Replace newlines.
        escapedTerm = escapedTerm.replace(/\r\n/g, " ");
        escapedTerm = escapedTerm.replace(/\n/g, " ");
        escapedTerm = escapedTerm.replace(/<br>/g, " ");

        if (this.isPhrase(escapedTerm)) {
            escapedTerm = String(escapedTerm).replace(/(\"|\\)/g, '\\$&');
            escapedTerm = '"' + escapedTerm + '"';
        } else {
            // Escape all lucene special characters from the source: && || : \ / + - ! ( ) { } [ ] ^ " ~ * ?
            escapedTerm = String(escapedTerm).replace(/(&&|\|\||[\:\\\/\+\-\!\(\)\{\}\[\]\^\"\~\*\?])/g, "\\$&");
        }

        return escapedTerm;
    }

    queryContainsTerm(query: string, termInQuestion: string): boolean {
        return query.indexOf(termInQuestion) != -1;
    }

    addQueryTerm(query: string, term: string, operator: string): string {
        if (this.queryContainsTerm(query, term)) {
            return query;
        }
        let newQuery = "";
        if (typeof operator !== 'undefined' && query !== "" && query !== "*") {
            newQuery = query + " " + operator + " ";
        }
        newQuery += term;

        return newQuery;
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
        var originalParams = Immutable.Map<string, any>();
        originalParams = originalParams.set('range_type', this.originalSearch.get('rangeType'));
        originalParams = originalParams.merge(this.originalSearch.get('rangeParams'));
        originalParams = originalParams.set('query', this.originalSearch.get('query'));
        originalParams = originalParams.set('interval', this.originalSearch.get('resolution'));
        if (this.searchInStream) {
            originalParams = originalParams.set('streamId', this.searchInStream.id);
        }

        return originalParams;
    }

    // Get initial search params with the current selected fields
    getOriginalSearchParamsWithFields(): Immutable.Map<string,any> {
        var originalParams = this.getOriginalSearchParams();
        originalParams = originalParams.set('fields', this.fields.join(','));

        return originalParams;
    }

    // Get initial search params, with the names used in a search URL request
    getOriginalSearchURLParams(): Immutable.Map<string, any> {
        var originalURLParams = Immutable.Map<string, any>();
        originalURLParams = originalURLParams.set('rangetype', this.originalSearch.get('rangeType'));
        originalURLParams = originalURLParams.merge(this.originalSearch.get('rangeParams'));
        originalURLParams = originalURLParams.set('q', this.originalSearch.get('query'));
        originalURLParams = originalURLParams.set('interval', this.originalSearch.get('resolution'));
        originalURLParams = originalURLParams.set('page', this.originalSearch.get('page'));
        originalURLParams = originalURLParams.set('fields', this.fields ? this.fields.join(',') : '');
        originalURLParams = originalURLParams.set('sortField', this.originalSearch.get('sortField'));
        originalURLParams = originalURLParams.set('sortOrder', this.originalSearch.get('sortOrder'));

        if (this.originalSearch.has('saved')) {
            originalURLParams = originalURLParams.set('saved', this.originalSearch.get('saved'));
        }

        return originalURLParams;
    }

    searchBaseLocation(action) {
        var location;
        if (this.searchInStream) {
            location = Routes.stream_search(this.searchInStream.id);
        } else {
            location = Routes.SEARCH;
        }
        return location;
    }

    _reloadSearchWithNewParam(param: string, value: any) {
        var searchURLParams = this.getOriginalSearchURLParams();
        searchURLParams = searchURLParams.set("width", this.width);
        searchURLParams = searchURLParams.set(param, value);
        this.executeSearch(this.searchBaseLocation("index") + "?" + Qs.stringify(searchURLParams.toJS()));
    }

    _reloadSearchWithNewParams(newParams: Immutable.Map<string, any>) {
        var searchURLParams = this.getOriginalSearchURLParams();
        searchURLParams = searchURLParams.set("width", this.width);
        searchURLParams = searchURLParams.merge(newParams);
        this.executeSearch(this.searchBaseLocation("index") + "?" + Qs.stringify(searchURLParams.toJS()));
    }

    executeSearch(url) {
        history.push(url);
    }

    searchSurroundingMessages(messageId: string, fromTime: string, toTime: string, filter: any) {
      var originalParams = this.getOriginalSearchParamsWithFields().toJS();

      var query = Object.keys(filter)
        .filter((key) => filter[key])
        .map((key) => `${key}:"${SearchStore.escape(filter[key])}"`)
        .join(' AND ');

      var params = {
        rangetype: 'absolute',
        from: fromTime,
        to: toTime,
        q: query,
        highlightMessage: messageId,
        fields: originalParams.fields,
      };

      return this.searchBaseLocation('index') + '?' + Qs.stringify(params);
    }
}

var searchStore = new SearchStore();

export default searchStore;

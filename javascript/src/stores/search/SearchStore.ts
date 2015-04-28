/// <reference path="../../../declarations/jquery/jquery.d.ts" />

'use strict';

var initialized = false;

class SearchStore {
    static NOT_OPERATOR = "NOT";
    static OR_OPERATOR = "OR";
    static AND_OPERATOR = "AND";

    private _query: string;
    onQueryChanged: (query: string)=>void;

    constructor() {
        $(document).on('add-search-term.graylog.universalsearch', this._addSearchTerm.bind(this));
    }

    set query(newQuery: string) {
        this._query = newQuery;
        if (this.onQueryChanged !== undefined) {
            this.onQueryChanged(this.query);
        }
    }

    get query(): string {
        return this._query;
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
}

var searchStore = new SearchStore();

export = searchStore;

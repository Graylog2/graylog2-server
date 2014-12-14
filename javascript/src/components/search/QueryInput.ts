/// <reference path="../../../declarations/jquery/jquery.d.ts" />
/// <reference path="../../../declarations/typeahead/typeahead.d.ts" />

'use strict';

import FieldsStore = require('../../stores/fields/FieldsStore');
import queryParser = require('../../logic/search/queryParser');

class QueryInput {
    private typeAheadConfig: any;
    private typeAheadSource: any;
    private fields: string[];
    private fieldsPromise: JQueryPromise<string[]>;
    private limit: number;
    private displayKey: string;

    constructor(private queryInputContainer: Element) {
        this.fieldsPromise = FieldsStore.loadFields();
        this.limit = 10;
        this.displayKey = 'value';
        this.typeAheadConfig = {
            hint: true,
            highlight: true,
            minLength: 1
        };
        this.typeAheadSource = {
            name: 'fields',
            displayKey: this.displayKey,
            source: this.codeCompletionProvider.bind(this)
        };
    }
    display() {
        this.fieldsPromise.done((fields) => {
            this.fields = fields;
            $(this.queryInputContainer).typeahead(this.typeAheadConfig, this.typeAheadSource);
        });
    }

    private filter(prefix: string, query: string, possibleMatches: string[], matches: Array<any>, startOnly: boolean=true) {
        possibleMatches.forEach((possibleMatch) => {
            var isMatch = (startOnly ?
                            possibleMatch.indexOf(query) === 0 :
                            possibleMatch.indexOf(query) !== -1 && possibleMatch.indexOf(query) !== 0)
            if (matches.length < this.limit && isMatch) {
                var match = {};
                match[this.displayKey] = prefix + possibleMatch;
                matches.push(match);
            }
        });
    }

    private codeCompletionProvider(query: string, callback: (matches: Array<any>) => void) {
        var matches:Array<any> = [];
        var possibleMatches = [];
        var parser = new queryParser.QueryParser(query);
        if (parser.currentFollowSetContainsAny(queryParser.TokenType.PHRASE, queryParser.TokenType.TERM)) {
            possibleMatches = possibleMatches.concat(this.fieldsCompletions());
        }
        // TODO: need to set the prefix to be part of query already matched
        // completion will then suggest the complete query matches to far
        // TODO: do we need to check the cursor position?
        var prefix = "";
        this.filter(prefix, query, possibleMatches, matches, true);
        this.filter(prefix, query, possibleMatches, matches, false);
        callback(matches);
    }

    private fieldsCompletions(): string[] {
        var possibleMatches = [];

        this.fields.forEach((field) => {
            possibleMatches.push(field + ":");
            possibleMatches.push("_exists_:" + field);
            possibleMatches.push("_missing_:" + field);
        });
        return possibleMatches;
    }

}

export = QueryInput;
/// <reference path="../../../declarations/jquery/jquery.d.ts" />
/// <reference path="../../../declarations/typeahead/typeahead.d.ts" />

'use strict';

import FieldsStore = require('../../stores/fields/FieldsStore');
import UniversalSearch = require('../../logic/search/UniversalSearch');

class QueryInput {
    constructor(private queryInputContainer: Element) {
    }
    display() {
        FieldsStore.loadFields().done((fields) => {
            var possibleMatches = [];

            fields.forEach((field) => {
                possibleMatches.push(field + ":");
                possibleMatches.push("_exists_:" + field);
                possibleMatches.push("_missing_:" + field);
            });
            $(this.queryInputContainer).typeahead(
                {
                    hint: true,
                    highlight: true,
                    minLength: 1
                },
                {
                    name: 'fields',
                    displayKey: 'value',
                    source: UniversalSearch.substringMatcher(possibleMatches, 'value', 6)
                });
        });
    }
}

export = QueryInput;
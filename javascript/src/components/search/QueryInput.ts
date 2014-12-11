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
            // TODO: Add all the other pseudo-field stuff here
            //fields.push("_missing_");
            $(this.queryInputContainer).typeahead(
                {
                    hint: true,
                    highlight: true,
                    minLength: 1
                },
                {
                    name: 'fields',
                    displayKey: 'value',
                    source: UniversalSearch.substringMatcher(fields, 'value', 6)
                });
        });
    }
}

export = QueryInput;
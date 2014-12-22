/// <reference path="../../../declarations/jquery/jquery.d.ts" />
/// <reference path="../../../declarations/typeahead/typeahead.d.ts" />

'use strict';

import FieldsStore = require('../../stores/fields/FieldsStore');
import queryParser = require('../../logic/search/queryParser');
import SerializeVisitor = require('../../logic/search/visitors/serializeVisitor');
import DumpVisitor = require('../../logic/search/visitors/dumpVisitor');

interface Match {
    match: string;
    currentSegment: string;
    prefix: string;
    value: string;
}

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
            source: this.codeCompletionProvider.bind(this),
            templates: {
                suggestion: (match: Match) => {
                    var previousTerms = match.prefix;
                    var matchPrefix = match.match.substring(0, match.match.indexOf(match.currentSegment));
                    var currentMatch = match.currentSegment;
                    var matchSuffix = match.match.substring(match.match.indexOf(match.currentSegment) + match.currentSegment.length);
                    return '<p><strong>' + previousTerms + '</strong>' + matchPrefix +  '<strong>' + currentMatch + '</strong>' + matchSuffix +'</p>';
                }
            }
        };
    }
    display() {
        this.fieldsPromise.done((fields) => {
            this.fields = fields;
            $(this.queryInputContainer).typeahead(this.typeAheadConfig, this.typeAheadSource);
        });
    }

    private filter(prefix: string, query: string, possibleMatches: string[], matches: Array<Match>, config?: {prefixOnly: boolean}) {
        possibleMatches.forEach((possibleMatch) => {
            var isMatch = ((config && config.prefixOnly) ?
                                possibleMatch.indexOf(query) === 0 :
                                possibleMatch.indexOf(query) !== -1 && possibleMatch.indexOf(query) !== 0);
            if (matches.length < this.limit && isMatch) {
                var match: Match = {
                    value: (prefix + possibleMatch).trim(),
                    match: possibleMatch,
                    currentSegment: query,
                    prefix: prefix
                };
                matches.push(match);
            }
        });
    }

    private codeCompletionProvider(query: string, callback: (matches: Array<any>) => void) {
        var prefix = "";
        var matches:Array<any> = [];
        var possibleMatches = [];
        var parser = new queryParser.QueryParser(query);
        var ast = parser.parse();
        var visitor = new SerializeVisitor();
        visitor.visit(ast);
        var serializedAst: queryParser.AST[] = visitor.result();

        if (serializedAst.length === 0) {
            possibleMatches = possibleMatches.concat(this.fieldsCompletions());
        } else {
            var currentAST =  serializedAst.pop();
            if (currentAST instanceof queryParser.TermAST) {
                possibleMatches = possibleMatches.concat(this.fieldsCompletions());
            }

            var querySegmentVisitor = new DumpVisitor();
            querySegmentVisitor.visit(currentAST);
            query = querySegmentVisitor.result();

            var prefixVisitor = new DumpVisitor(currentAST);
            prefixVisitor.visit(ast);
            prefix = prefixVisitor.result();
        }
        this.filter(prefix, query, possibleMatches, matches, {prefixOnly: true});
        this.filter(prefix, query, possibleMatches, matches);
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
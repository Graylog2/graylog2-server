/// <reference path="../../../declarations/bluebird/bluebird.d.ts" />
/// <reference path="../../../declarations/typeahead/typeahead.d.ts" />

'use strict';

const StoreProvider = require('injection/StoreProvider');
const FieldsStore = StoreProvider.getStore('Fields');
import queryParser = require('../../logic/search/queryParser');
import SerializeVisitor = require('../../logic/search/visitors/SerializeVisitor');
import DumpVisitor = require('../../logic/search/visitors/DumpVisitor');
const $ = require('jquery');
const Typeahead = require('typeahead.js');

interface Match {
    match: string;
    currentSegment: string;
    prefix: string;
    value: string;
}

interface SplitQuery {
    current: string;
    prefix: string;
}

class QueryInput {
    private typeAheadConfig: any;
    private typeAheadSource: any;
    private fields: string[];
    private fieldsPromise: Promise<string[]>;
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
                // TODO: highlight errors on suggestions once query parser is completed
                suggestion: (match: Match) => {
                    var previousTerms = match.prefix;
                    var matchPrefix = match.match.substring(0, match.match.indexOf(match.currentSegment));
                    var currentMatch = match.currentSegment;
                    var matchSuffix = match.match.substring(match.match.indexOf(match.currentSegment) + match.currentSegment.length);
                    return '<p><strong>' + previousTerms + '</strong>' + matchPrefix + '<strong>' + currentMatch + '</strong>' + matchSuffix + '</p>';
                }
            }
        };
    }

    display() {
        this.fieldsPromise.then((fields) => {
            this.fields = fields;
            $(this.queryInputContainer).typeahead(this.typeAheadConfig, this.typeAheadSource);
        });
    }

    _value() {
        return $(this.queryInputContainer).typeahead('val');
    }

    update(newValue) {
        if (this._value() !== newValue) {
            $(this.queryInputContainer).typeahead('val', newValue);
        }
    }

    private codeCompletionProvider(query: string, callback: (matches: Array<any>) => void) {
        var prefix = "";
        var matches: Array<any> = [];
        var possibleMatches = [];
        var parser = new queryParser.QueryParser(query);
        var ast = parser.parse();
        var visitor = new SerializeVisitor();
        visitor.visit(ast);
        var serializedAst: queryParser.AST[] = visitor.result();

        if (serializedAst.length === 0) {
            possibleMatches = possibleMatches.concat(this.fieldsCompletions());
            possibleMatches = possibleMatches.concat(this.unaryOperatorsCompletions());
        } else {
            var currentAST = serializedAst[serializedAst.length - 1];
            var previousAST = serializedAst[serializedAst.length - 2];
            var twoASTago = serializedAst[serializedAst.length - 3];
            var firstAST = serializedAst[0];

            var splitQuery = this.splitQuery(ast, currentAST);
            query = splitQuery.current;
            prefix = splitQuery.prefix;

            if (currentAST instanceof queryParser.TermAST) {
                possibleMatches = possibleMatches.concat(this.fieldsCompletions());

                var offerOperatorsCompletion = !(previousAST instanceof queryParser.ModifierAST);
                if (offerOperatorsCompletion) {
                    var offerBinaryOperatorsCompletion = (serializedAst.length > 1) && !(previousAST instanceof queryParser.ExpressionAST);

                    if (offerBinaryOperatorsCompletion) {
                        var includeNotCompletion = !((previousAST instanceof queryParser.ModifierAST) && ((<queryParser.ModifierAST>previousAST).isNOTModifier()));
                        possibleMatches = possibleMatches.concat(this.binaryOperatorsCompletions(includeNotCompletion));
                    }
                    possibleMatches = possibleMatches.concat(this.unaryOperatorsCompletions());
                }
            }

            // Don't offer completion on MissingAST when there are alternative MissingASTs (e.g. "foo AND AND AND")
            // or when the query starts with an operator (e.g. "AND").
            var shouldCompleteMissingAST = !(twoASTago instanceof queryParser.MissingAST) && !(firstAST instanceof queryParser.ExpressionAST);
            if ((currentAST instanceof queryParser.MissingAST) && shouldCompleteMissingAST) {
                var lastCharacter = prefix.charAt(prefix.length - 1);
                var lastNonWSCharacter = prefix.trim().charAt(prefix.trim().length - 1);
                if (lastCharacter !== " " && (["+", "-", "!"].indexOf(lastNonWSCharacter) === -1)) {
                    prefix += " ";
                }
                possibleMatches = possibleMatches.concat(this.fieldsCompletions());
                possibleMatches = possibleMatches.concat(this.unaryOperatorsCompletions());
            }
        }
        this.filterCompletionMatches(prefix, query, possibleMatches, matches, {prefixOnly: true});
        this.filterCompletionMatches(prefix, query, possibleMatches, matches);
        callback(matches);
    }

    private splitQuery(ast: queryParser.AST, currentAST: queryParser.AST): SplitQuery {
        var querySegmentVisitor = new DumpVisitor();
        querySegmentVisitor.visit(currentAST);
        var query = querySegmentVisitor.result();

        var prefixVisitor = new DumpVisitor(currentAST);
        prefixVisitor.visit(ast);
        var prefix = prefixVisitor.result();

        return {current: query, prefix: prefix};
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

    private unaryOperatorsCompletions(): string[] {
        var possibleMatches = ["+", "-", "!", "NOT"];
        return possibleMatches;
    }

    private binaryOperatorsCompletions(includeNotCompletion?: boolean): string[] {
        var possibleMatches = [];

        possibleMatches.push("&&", "AND");
        if (includeNotCompletion) {
            possibleMatches.push("!", "NOT");
        }
        possibleMatches.push("||", "OR");

        return possibleMatches;
    }

    private filterCompletionMatches(prefix: string, query: string, possibleMatches: string[], matches: Array<Match>, config?: {prefixOnly: boolean}) {
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
}

export = QueryInput;

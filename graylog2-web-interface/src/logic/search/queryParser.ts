'use strict';

// parser for http://lucene.apache.org/core/4_10_2/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#package_description
// http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl-query-string-query.html
// Reference to original lucene query parser https://svn.apache.org/repos/asf/lucene/dev/trunk/lucene/queryparser/src/java/org/apache/lucene/queryparser/classic/QueryParser.jj

export enum TokenType {
    EOF, WS, TERM, PHRASE, AND, OR, NOT, COLON, MUST, MUST_NOT, ERROR
}

export class AST {
    hiddenPrefix: Array<Token> = [];
    hiddenSuffix: Array<Token> = [];
}

export class MissingAST extends AST {
}

export class ModifierAST extends AST {
    hiddenModifierPrefix: Array<Token> = [];
    hiddenModifierSuffix: Array<Token> = [];

    constructor(public modifier: Token, public right: AST) {
        super();
    }

    isNOTModifier() {
        return this.modifier.type === TokenType.NOT;
    }
}

export class ExpressionAST extends AST {
    hiddenOpPrefix: Array<Token> = [];
    hiddenOpSuffix: Array<Token> = [];

    constructor(public left: TermAST, public op: Token,
                public right: AST) {
        super();
    }
}

export class TermAST extends AST {
    constructor(public term: Token) {
        super();
    }

    isPhrase() {
        return this.term.asString().indexOf(" ") !== -1;
    }

    isInclusiveRange() {
        return this.term.asString().indexOf("[") === 0;
    }

    isExclusiveRange() {
        return this.term.asString().indexOf("{") === 0;
    }
}

export class TermWithFieldAST extends TermAST {
    hiddenColonPrefix: Array<Token> = [];
    hiddenColonSuffix: Array<Token> = [];

    constructor(public field: Token, public colon: Token, term: Token) {
        super(term);
    }

}

export class ExpressionListAST extends AST {
    public expressions = Array<AST>();

    constructor(...expressions: AST[]) {
        super();
        this.expressions = this.expressions.concat(expressions);
    }

    add(expr: AST) {
        this.expressions.push(expr);
    }
}

export class Token {
    public typeName: string;

    constructor(private input: string, public type: TokenType, public beginPos: number, public endPos: number) {
        this.typeName = TokenType[type];
    }

    asString() {
        return this.input.substring(this.beginPos, this.endPos);
    }

}

class QueryLexer {
    public pos: number;
    private eofToken: Token;

    constructor(private input: string) {
        this.pos = 0;
        this.eofToken = new Token(this.input, TokenType.EOF, input.length, input.length);
    }

    next(): Token {
        var token;
        var la = this.lookAhead();
        if (la === null) {
            token = this.eofToken;
        } else if (this.isWhitespace(la)) {
            token = this.whitespace();
        } else if (this.isKeyword("OR") || this.isKeyword("||")) {
            token = this.or();
        } else if (this.isKeyword("AND") || this.isKeyword("&&")) {
            token = this.and();
        } else if (this.isKeyword("NOT") || this.isPrefix("!")) {
            token = this.not();
        } else if (this.isPrefix("+")) {
            token = this.must();
        } else if (this.isPrefix("-")) {
            token = this.mustNot();
        } else if (la === '"') {
            token = this.phrase();
        } else if (la === ':') {
            var startPos = this.pos;
            this.consume();
            token = new Token(this.input, TokenType.COLON, startPos, this.pos);
        } else if (la[0] === '\\' && la.length === 1) {
            // we have an escape character, but nothing that is escaped, we consider this an error
            this.pos--;
            var startPos = this.pos;
            this.consume();
            token = new Token(this.input, TokenType.ERROR, startPos, this.pos);
        } else if (this.isTermStart(la)) {
            if (this.isRangeStart(la)) {
                token = this.range();
            } else {
                token = this.term();
            }
        } else {
            var startPos = this.pos;
            this.consume();
            token = new Token(this.input, TokenType.ERROR, startPos, this.pos);
        }
        return token;
    }

    // Check if the keyword is on the current position.
    // Returns the keyword length if it was found, -1 in other case.
    private lookAheadKeyword(keyword: string): number {
        for (var i = 0; i < keyword.length; i++) {
            if (this.lookAhead(i) !== keyword[i]) {
                return -1;
            }
        }
        return i;
    }

    isKeyword(keyword: string): boolean {
        var i = this.lookAheadKeyword(keyword);
        if (i < 0) {
            return false;
        }
        // be sure that it is not a prefix of something else
        return this.isWhitespace(this.lookAhead(i)) || this.lookAhead(i) === null;
    }

    isPrefix(keyword: string): boolean {
        var i = this.lookAheadKeyword(keyword);
        if (i < 0) {
            return false;
        }
        // be sure that it is a prefix
        return !this.isWhitespace(this.lookAhead(i)) && this.lookAhead(i) !== null;
    }

    or() {
        var startPos = this.pos;
        this.consume(2);
        return new Token(this.input, TokenType.OR, startPos, this.pos);
    }

    and() {
        var startPos = this.pos;
        this.consume(this.lookAhead() === '&' ? 2 : 3);
        return new Token(this.input, TokenType.AND, startPos, this.pos);
    }

    not() {
        var startPos = this.pos;
        this.consume(this.lookAhead() === '!' ? 1 : 3);
        return new Token(this.input, TokenType.NOT, startPos, this.pos);
    }

    must() {
        var startPos = this.pos;
        this.consume(1);
        return new Token(this.input, TokenType.MUST, startPos, this.pos);
    }

    mustNot() {
        var startPos = this.pos;
        this.consume(1);
        return new Token(this.input, TokenType.MUST_NOT, startPos, this.pos);
    }

    whitespace() {
        var startPos = this.pos;
        var la = this.lookAhead();
        while (this.isWhitespace(la)) {
            this.consume();
            la = this.lookAhead();
        }
        return new Token(this.input, TokenType.WS, startPos, this.pos);
    }

    term() {
        var startPos = this.pos;
        // consume start character
        this.consume();
        var la = this.lookAhead();
        while (this.isTerm(la)) {
            this.consume();
            la = this.lookAhead();
        }
        return new Token(this.input, TokenType.TERM, startPos, this.pos);
    }

    range() {
        var startPos = this.pos;
        var rangeStartCharacter = this.lookAhead();
        // consume start character
        this.consume();
        var rangeEndChecker = this.isExclusiveRangeStart(rangeStartCharacter) ? this.isExclusiveRangeEnd : this.isInclusiveRangeEnd;
        var la = this.lookAhead();
        while (la !== null && !rangeEndChecker(la)) {
            this.consume();
            la = this.lookAhead();
        }

        if (la === null) {
            // We expect a closing bracket but we found EOF
            return new Token(this.input, TokenType.ERROR, startPos, this.pos);
        }

        // consume range close bracket
        this.consume();
        return new Token(this.input, TokenType.TERM, startPos, this.pos);
    }

    phrase() {
        var startPos = this.pos;
        this.consume(); // skip starting "
        var la = this.lookAhead();
        while (la !== null && la !== '"') {
            this.consume();
            la = this.lookAhead();
        }
        this.consume(); // skip ending "
        return new Token(this.input, TokenType.PHRASE, startPos, this.pos);
    }

    isDigit(char) {
        return char !== null && (('a' <= char && char <= 'z') || ('A' <= char && char <= 'Z') || ('0' <= char && char <= '9'));
    }

    isOneOf(set: string, char: string) {
        return set.indexOf(char) !== -1;
    }

    isWhitespace(char) {
        return this.isOneOf(' \t\n\r\u3000', char);
    }

    isSpecial(char) {
        return this.isOneOf('+-!():^[]"{}~*?\\/', char);
    }

    isRangeStart(char) {
        return this.isInclusiveRangeStart(char) || this.isExclusiveRangeStart(char);
    }

    isInclusiveRangeStart(char) {
        return char === '[';
    }

    isExclusiveRangeStart(char) {
        return char === '{';
    }

    isInclusiveRangeEnd(char) {
        return char === ']';
    }

    isExclusiveRangeEnd(char) {
        return char === '}';
    }

    isTermStart(char) {
        return char !== null && !this.isWhitespace(char) && (!this.isSpecial(char) || this.isRangeStart(char));
    }

    isTerm(char) {
        return this.isTermStart(char) || this.isOneOf('+-', char);
    }

    isEscaped(char) {
        return char.length === 2 && char[0] === '\\';
    }

    consume(n: number = 1) {
        this.pos += n;
    }

    lookAhead(la: number = 0): string {
        var index = this.pos + la;
        var char = (this.input.length <= index) ? null : this.input[index];
        if (char === '\\') {
            this.consume();
            if (this.input.length <= index) {
                var escapedChar = this.input[index];
                char += escapedChar;
            }
        }
        return char;
    }
}

export interface ErrorObject {
    position: number;
    message: string;
}

interface Rule2Token {
    [index: string]: TokenType[];
}


export class QueryParser {
    private static firstSets: Rule2Token = {
        termOrPhrase: [TokenType.TERM, TokenType.PHRASE],
        expr: [TokenType.TERM, TokenType.PHRASE],
        operator: [TokenType.OR, TokenType.AND],
        modifier: [TokenType.NOT, TokenType.MUST, TokenType.MUST_NOT]
    };
    private static followSets: Rule2Token = {
        expr: QueryParser.firstSets["expr"].concat(QueryParser.firstSets["operator"]).concat(QueryParser.firstSets["modifier"]),
        modifier: QueryParser.firstSets["expr"].concat(QueryParser.firstSets["operator"]).concat(QueryParser.firstSets["modifier"])
    };
    private lexer: QueryLexer;
    private tokenBuffer: Token[];
    public errors: ErrorObject[] = [];
    private ruleStack: string[] = [];

    constructor(private input: string) {
        this.lexer = new QueryLexer(input);
        this.tokenBuffer = [];
    }

    private consume() {
        this.tokenBuffer.splice(0, 1);
    }

    private lookAhead(la: number = 0): Token {
        // fill token buffer until we can look ahead far enough
        while (la >= this.tokenBuffer.length) {
            var token = this.lexer.next();
            if (token.type === TokenType.EOF) {
                return token;
            }
            this.tokenBuffer.push(token);
        }
        return this.tokenBuffer[la];
    }

    private skipHidden(): Array<Token> {
        var skippedTokens = this.syncWhile(TokenType.WS, TokenType.ERROR);
        skippedTokens.filter((token) => token.type === TokenType.ERROR).forEach((errorToken) => {
            this.errors.push({
                position: errorToken.beginPos,
                message: "Unexpected input: '" + errorToken.asString() + "'"
            });
        });
        return skippedTokens;
    }

    private syncWhile(...syncWhile: TokenType[]): Array<Token> {
        var skippedTokens = [];
        while (syncWhile.some((type) => type === this.lookAhead().type)) {
            skippedTokens.push(this.lookAhead());
            this.consume();
        }
        return skippedTokens;
    }

    private syncTo(syncTo: TokenType[]): Array<Token> {
        var skippedTokens = [];
        while (this.lookAhead().type !== TokenType.EOF && syncTo.every((type) => type !== this.lookAhead().type)) {
            skippedTokens.push(this.lookAhead());
            this.consume();
        }
        return skippedTokens;
    }

    private unexpectedToken() {
        var syncTo = this.currentFollowSet();
        this.errors.push({
            position: this.lookAhead().beginPos,
            message: "Unexpected input"
        });
        return this.syncTo(syncTo);
    }

    private missingToken(tokenName: string) {
        var syncTo = this.currentFollowSet();
        this.errors.push({
            position: this.lookAhead().beginPos,
            message: "Missing " + tokenName
        });
        return this.syncTo(syncTo);
    }

    private enterRule(name: string) {
        this.ruleStack.push(name);
    }

    private exitRule(name: string) {
        var actualName = this.ruleStack.pop();
        if (actualName !== name) {
            throw new Error("Unmatched rule name (was " + actualName + ", but should have been " + name + ")");
        }
    }

    private currentRule(): string {
        var ruleName = this.ruleStack.pop();
        if (ruleName) {
            this.ruleStack.push(ruleName);
        }
        return ruleName;
    }

    private isFirstOf(tokenTypes: TokenType[]) {
        return tokenTypes.some((tokenType) => this.lookAhead().type === tokenType);
    }

    private isInFirstSetOf(ruleName: string) {
        return this.isFirstOf(QueryParser.firstSets[ruleName]);
    }

    private isOperatorOrExpression() {
        return this.isInFirstSetOf("expr") || this.isInFirstSetOf("operator");
    }

    private isExpr() {
        return this.isInFirstSetOf("expr");
    }

    private isOperator() {
        return this.isInFirstSetOf("operator");
    }

    private isModifier() {
        return this.isInFirstSetOf("modifier");
    }

    private isEOF() {
        return this.isFirstOf([TokenType.EOF]);
    }

    protected currentFollowSet(): TokenType[] {
        var currentRule = this.currentRule();
        if (currentRule) {
            return QueryParser.followSets[currentRule];
        } else {
            return QueryParser.firstSets["expr"];
        }
    }

    parse(): AST {
        this.errors = [];
        var ast: AST;
        var prefix = this.skipHidden();

        if (this.isOperatorOrExpression() || this.isModifier()) {
            ast = this.exprs();
        } else {
            ast = new MissingAST();
        }

        if (!this.isEOF()) {
            this.unexpectedToken();
        }
        ast.hiddenPrefix = ast.hiddenPrefix.concat(prefix);
        var trailingSuffix = this.skipHidden();
        ast.hiddenSuffix = ast.hiddenSuffix.concat(trailingSuffix);
        return ast;
    }

    exprs(): AST {
        this.enterRule("exprs");
        try {
            var expr: AST = null;

            if (this.isOperatorOrExpression()) {
                expr = this.expr();
            } else if (this.isModifier()) {
                expr = this.modifier();
            }

            if (this.isOperatorOrExpression() || this.isModifier()) {
                var expressionList = new ExpressionListAST();
                expressionList.add(expr);
                while (this.isOperatorOrExpression() || this.isModifier()) {
                    if (this.isOperatorOrExpression()) {
                        expr = this.expr();
                    } else {
                        expr = this.modifier();
                    }
                    expressionList.add(expr);
                }
                return expressionList;
            } else {
                return expr;
            }
        } finally {
            this.exitRule("exprs");
        }
    }

    modifier(): AST {
        this.enterRule("modifier");
        try {
            var modifier: Token = null;
            var right: AST = null;

            var hiddenModifierPrefix = this.skipHidden();
            modifier = this.lookAhead();
            this.consume();
            var hiddenModifierSuffix = this.skipHidden();

            if (this.isExpr()) {
                right = this.expr();
            } else {
                this.missingToken("right side of expression");
                right = new MissingAST();
            }
            var modifierAST = new ModifierAST(modifier, right);
            modifierAST.hiddenModifierPrefix = hiddenModifierPrefix;
            modifierAST.hiddenModifierSuffix = hiddenModifierSuffix;
            return modifierAST;
        } finally {
            this.exitRule("modifier");
        }
    }

    expr(): AST {
        this.enterRule("expr");
        try {
            var left: TermAST = null;
            var op: Token = null;
            var right: AST = null;
            var hiddenOpPrefix = [];

            if (this.isExpr()) {
                left = this.termOrPhrase();
                hiddenOpPrefix = this.skipHidden();
            } else if (this.isOperator()) {
                this.missingToken("left side of expression");
                right = new MissingAST();
            } else {
                this.unexpectedToken();
            }

            if (!this.isOperator()) {
                left.hiddenSuffix = left.hiddenSuffix.concat(hiddenOpPrefix);
                return left;
            } else {
                op = this.lookAhead();
                this.consume();
                var hiddenOpSuffix = this.skipHidden();
                if (this.isExpr()) {
                    right = this.expr();
                } else if (this.isModifier()) {
                    right = this.modifier();
                } else {
                    this.missingToken("right side of expression");
                    right = new MissingAST();
                }
                var expressionAST = new ExpressionAST(left, op, right);
                expressionAST.hiddenOpPrefix = hiddenOpPrefix;
                expressionAST.hiddenOpSuffix = hiddenOpSuffix;
                return expressionAST;
            }
        } finally {
            this.exitRule("expr");
        }
    }

    termOrPhrase() {
        this.enterRule("term");
        try {
            var termOrField = this.lookAhead();
            this.consume();
            var wsAfterTermOrField = this.skipHidden();
            if (this.lookAhead().type === TokenType.COLON) {
                var colon = this.lookAhead();
                this.consume();
                var prefixAfterColon = this.skipHidden();
                if (this.lookAhead().type === TokenType.TERM || this.lookAhead().type === TokenType.PHRASE) {
                    var term = this.lookAhead();
                    this.consume();
                    var ast = new TermWithFieldAST(termOrField, colon, term);
                    ast.hiddenColonPrefix = wsAfterTermOrField;
                    ast.hiddenColonSuffix = prefixAfterColon;
                    return ast;
                } else {
                    var skippedTokens = this.missingToken("term or phrase for field");
                    var ast = new TermWithFieldAST(termOrField, colon, null);
                    ast.hiddenColonPrefix = wsAfterTermOrField;
                    ast.hiddenColonSuffix = prefixAfterColon;
                    ast.hiddenSuffix = skippedTokens;
                    return ast;
                }
            }
            var termAST = new TermAST(termOrField);
            termAST.hiddenSuffix = wsAfterTermOrField;
            return termAST;
        } finally {
            this.exitRule("term");
        }
    }
}


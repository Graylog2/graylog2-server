'use strict';

var queryParser = require("../../src/logic/search/queryParser");
var DumpVisitor = require("../../src/logic/search/visitors/DumpVisitor");

describe('Query Parser', function () {
    var ExpressionAST, ExpressionListAST, ModifierAST, TermAST, TermWithFieldAST, Token, QueryParser, TokenType;

    var expectNoErrors = function (parser) {
        expect(parser.errors.length).toBe(0);
    };

    function expectIdentityDump(query, ignoreErrors) {
        var parser = new QueryParser(query);
        var ast = parser.parse();
        var dumpVisitor = new DumpVisitor();
        dumpVisitor.visit(ast);
        var dumped = dumpVisitor.result();
        if (!ignoreErrors) {
            expectNoErrors(parser);
        }
        expect(dumped).toBe(query);
    }

    beforeEach(function () {
        TermAST = queryParser.TermAST;
        TermWithFieldAST = queryParser.TermWithFieldAST;
        Token = queryParser.Token;
        QueryParser = queryParser.QueryParser;
        TokenType = queryParser.TokenType;
        ExpressionListAST = queryParser.ExpressionListAST;
        ExpressionAST = queryParser.ExpressionAST;
        ModifierAST = queryParser.ModifierAST;
    });

    it('can parse a term', function () {
        var query = "login";
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expectNoErrors(parser);
        expect(ast instanceof TermAST).toBeTruthy();
        expect(ast.term instanceof Token).toBeTruthy();
        expect(ast.term.type).toBe(TokenType.TERM);
        expect(ast.term.asString()).toBe(query);
        expectIdentityDump(query);
    });

    it('can parse two terms', function () {
        var query = "login submit";
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expect(ast instanceof ExpressionListAST).toBeTruthy();
        expect(ast.expressions.length).toBe(2);
        var firstExpr = ast.expressions[0];
        expect(firstExpr instanceof TermAST).toBeTruthy();
        expectIdentityDump(query);
    });

    it('can parse a term in a field', function () {
        var query = "action:login";
        expectIdentityDump(query);
    });

    it('can parse a phrase in a field', function () {
        var query = 'action:"login now"';
        expectIdentityDump(query);
    });

    it('tolerates ws before and after colon in a field expression', function () {
        var query = 'action : login';
        expectIdentityDump(query);
    });

    it('can parse a phrase', function () {
        var query = '"login now"';
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expectNoErrors(parser);
        expect(ast instanceof TermAST).toBeTruthy();
        expect(ast.isPhrase()).toBeTruthy();
        expect(ast.term instanceof Token).toBeTruthy();
        expect(ast.term.type).toBe(TokenType.PHRASE);
        expect(ast.term.asString()).toBe(query);
        expectIdentityDump(query);
    });

    it('can parse a phrase and a term', function () {
        var query = '"login now" submit';
        expectIdentityDump(query);
    });

    it('can parse a term and a phrase', function () {
        var query = 'submit "login now"';
        expectIdentityDump(query);
    });

    it('can parse many terms with ws', function () {
        var query = '  submit  "login now" logout ';
        expectIdentityDump(query);
    });

    it('can parse an OR expression', function () {
        var query = "login OR submit";
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expectNoErrors(parser);
        expect(ast instanceof ExpressionAST).toBeTruthy();
        expect(ast.left instanceof TermAST).toBeTruthy();
        expect(ast.left.term.type).toBe(TokenType.TERM);

        expect(ast.op.type).toBe(TokenType.OR);
        expect(ast.right instanceof TermAST).toBeTruthy();

        expectIdentityDump(query);
    });

    it('preserves whitespace on dump of simple expression', function () {
        var query = " login ";
        expectIdentityDump(query);
    });

    it('preserves whitespace on dump of complex expression', function () {
        var query = "  login  OR  \n \t  submit   ";
        expectIdentityDump(query);
    });

    it('can parse an AND expression', function () {
        var query = "login AND submit";
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expectNoErrors(parser);
        expect(ast instanceof ExpressionAST).toBeTruthy();
        expect(ast.left instanceof TermAST).toBeTruthy();
        expect(ast.left.term.type).toBe(TokenType.TERM);

        expect(ast.op.type).toBe(TokenType.AND);
        expect(ast.right instanceof TermAST).toBeTruthy();

        expectIdentityDump(query);
    });

    it('can parse a complex AND expression', function () {
        var query = "login AND submit AND action:login";
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expectNoErrors(parser);
        expect(ast instanceof ExpressionAST).toBeTruthy();
        expect(ast.left instanceof TermAST).toBeTruthy();
        expect(ast.left.term.type).toBe(TokenType.TERM);

        expect(ast.op.type).toBe(TokenType.AND);
        expect(ast.right instanceof ExpressionAST).toBeTruthy();

        var rightExpressionAST = ast.right;
        expect(rightExpressionAST instanceof ExpressionAST).toBeTruthy();
        expect(rightExpressionAST.left instanceof TermAST).toBeTruthy();
        expect(rightExpressionAST.left.term.type).toBe(TokenType.TERM);

        expect(rightExpressionAST.op.type).toBe(TokenType.AND);
        expect(rightExpressionAST.right instanceof TermAST).toBeTruthy();

        expectIdentityDump(query);
    });

    it('can parse an && expression', function () {
        var query = "login && submit";
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expectNoErrors(parser);
        expect(ast instanceof ExpressionAST).toBeTruthy();
        expect(ast.left instanceof TermAST).toBeTruthy();
        expect(ast.left.term.type).toBe(TokenType.TERM);

        expect(ast.op.type).toBe(TokenType.AND);
        expect(ast.right instanceof TermAST).toBeTruthy();

        expectIdentityDump(query);
    });

    it('can parse an || expression', function () {
        var query = "login || submit";
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expectNoErrors(parser);
        expect(ast instanceof ExpressionAST).toBeTruthy();
        expect(ast.left instanceof TermAST).toBeTruthy();
        expect(ast.left.term.type).toBe(TokenType.TERM);

        expect(ast.op.type).toBe(TokenType.OR);
        expect(ast.right instanceof TermAST).toBeTruthy();

        expectIdentityDump(query);
    });

    it('reports an error when right side of AND is missing', function () {
        var query = "login AND ";
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expect(parser.errors.length).toBe(1);
        expect(parser.errors[0].message).toBe("Missing right side of expression");
        expect(parser.errors[0].position).toBe(10);
        expectIdentityDump(query, true);
    });

    it('reports an error when left side of AND is missing', function () {
        var query = " AND login";
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expect(parser.errors.length).toBe(1);
        expect(parser.errors[0].message).toBe("Missing left side of expression");
        expect(parser.errors[0].position).toBe(1);
        expectIdentityDump(query, true);
    });

    it('reports an error when field is missing term', function () {
        var query = 'action : ';
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expect(parser.errors.length).toBe(1);
        expect(parser.errors[0].message).toBe("Missing term or phrase for field");
        expect(parser.errors[0].position).toBe(9);
        expectIdentityDump(query, true);
    });

    it('reports an error two OR are together with no term in between', function () {
        var query = 'login OR OR submit';
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expect(parser.errors.length).toBe(2);
        expect(parser.errors[0].message).toBe("Missing right side of expression");
        expect(parser.errors[0].position).toBe(9);
        expect(parser.errors[1].message).toBe("Missing left side of expression");
        expect(parser.errors[1].position).toBe(9);
        expectIdentityDump(query, true);
    });

    it('trailing escape character gives error, but can still reproduce', function () {
        var query = '\\';
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expect(parser.errors.length).toBe(1);
        expect(parser.errors[0].message).toBe("Unexpected input: '\\'");
        expect(parser.errors[0].position).toBe(0);
        expectIdentityDump(query, true);
    });

    it('can parse empty query', function () {
        var query = '';
        expectIdentityDump(query);
    });

    it('can reproduce pure WS', function () {
        var query = ' \n\t';
        expectIdentityDump(query);
    });

    // none of those, really:
    // +-!():^[]"{}~*?\\/
    it('can parse everything except for ws and special characters as term', function () {
        var query = ' @$%&§öäüß#=.;_<>°"';
        expectIdentityDump(query);
    });

    it('can parse + and - literally inside term', function () {
        var query = 'start+-end"';
        expectIdentityDump(query);
    });

    it('can parse a unary NOT expression', function () {
        var query = "NOT submit";
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expectNoErrors(parser);
        expect(ast instanceof ModifierAST).toBeTruthy();
        expect(ast.modifier.type).toBe(TokenType.NOT);
        expect(ast.right instanceof TermAST).toBeTruthy();

        expectIdentityDump(query);
    });

    it('can parse a complex expression with NOT, AND and OR', function() {
        var query = "NOT submit AND action:login OR action:logout";
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expectNoErrors(parser);
        expect(ast instanceof ModifierAST).toBeTruthy();
        expect(ast.modifier.type).toBe(TokenType.NOT);
        expect(ast.right instanceof ExpressionAST).toBeTruthy();

        var andAST = ast.right;
        expect(andAST instanceof ExpressionAST).toBeTruthy();
        expect(andAST.left instanceof TermAST).toBeTruthy();
        expect(andAST.op.type).toBe(TokenType.AND);
        expect(andAST.right instanceof ExpressionAST).toBeTruthy();

        var orAST = andAST.right;
        expect(orAST instanceof ExpressionAST).toBeTruthy();
        expect(orAST.left instanceof TermAST).toBeTruthy();
        expect(orAST.op.type).toBe(TokenType.OR);
        expect(orAST.right instanceof TermAST).toBeTruthy();

        expectIdentityDump(query);
    });

    it('reports an error when NOT has no right part', function() {
        var query = 'NOT ';
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expect(parser.errors.length).toBe(1);
        expect(parser.errors[0].message).toBe("Missing right side of expression");
        expect(parser.errors[0].position).toBe(4);
        expectIdentityDump(query, true);
    });

    it('reports an error on double NOT', function() {
        var query = 'NOT NOT login';
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expect(parser.errors.length).toBe(1);
        expect(parser.errors[0].message).toBe("Missing right side of expression");
        expect(parser.errors[0].position).toBe(4);
        expectIdentityDump(query, true);
    });

    it('can parse a NOT expression preceded by a term', function() {
        var query = "login NOT submit";
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expectNoErrors(parser);
        expect(ast instanceof ExpressionListAST).toBeTruthy();
        expect(ast.expressions.length).toBe(2);

        var firstExpr = ast.expressions[0];
        expect(firstExpr instanceof TermAST).toBeTruthy();

        var secondExpr = ast.expressions[1];
        expect(secondExpr instanceof ModifierAST).toBeTruthy();
        expect(secondExpr.modifier.type).toBe(TokenType.NOT);
        expect(secondExpr.right instanceof TermAST).toBeTruthy();

        expectIdentityDump(query);
    });

    it('can parse a ! as NOT expression', function() {
        var query = "login !submit";
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expectNoErrors(parser);
        expect(ast instanceof ExpressionListAST).toBeTruthy();
        expect(ast.expressions.length).toBe(2);

        var firstExpr = ast.expressions[0];
        expect(firstExpr instanceof TermAST).toBeTruthy();

        var secondExpr = ast.expressions[1];
        expect(secondExpr instanceof ModifierAST).toBeTruthy();
        expect(secondExpr.modifier.type).toBe(TokenType.NOT);
        expect(secondExpr.right instanceof TermAST).toBeTruthy();

        expectIdentityDump(query);
    });

    it('can parse a NOT expression preceded by other expressions', function() {
        var query = "action:login OR action:logout NOT submit";
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expectNoErrors(parser);
        expect(ast instanceof ExpressionListAST).toBeTruthy();
        expect(ast.expressions.length).toBe(2);

        var firstExpr = ast.expressions[0];
        expect(firstExpr instanceof ExpressionAST).toBeTruthy();
        expect(firstExpr.left instanceof TermAST).toBeTruthy();
        expect(firstExpr.op.type).toBe(TokenType.OR);
        expect(firstExpr.right instanceof TermAST).toBeTruthy();

        var secondExpr = ast.expressions[1];
        expect(secondExpr instanceof ModifierAST).toBeTruthy();
        expect(secondExpr.modifier.type).toBe(TokenType.NOT);
        expect(secondExpr.right instanceof TermAST).toBeTruthy();

        expectIdentityDump(query);
    });

    it('can parse a chain of NOT expressions', function() {
        var query = "login NOT submit NOT logout NOT now";
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expectNoErrors(parser);
        expect(ast instanceof ExpressionListAST).toBeTruthy();
        expect(ast.expressions.length).toBe(4);

        var firstExpr = ast.expressions[0];
        expect(firstExpr instanceof TermAST).toBeTruthy();

        for(var i = 1; i < ast.expressions.length; i++) {
            var nextExpr = ast.expressions[i];
            expect(nextExpr instanceof ModifierAST).toBeTruthy();
            expect(nextExpr.modifier.type).toBe(TokenType.NOT);
            expect(nextExpr.right instanceof TermAST).toBeTruthy();
        }

        expectIdentityDump(query);
    });

    it('can parse ! at the beginning of the query', function() {
        var query = "!login";
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expectNoErrors(parser);
        expect(ast instanceof ModifierAST).toBeTruthy();
        expect(ast.modifier.type).toBe(TokenType.NOT);
        expect(ast.right instanceof TermAST).toBeTruthy();
        expectIdentityDump(query);
    });

    it('can parse a query where AND is followed by NOT', function() {
        var query = "quick OR brown AND fox AND NOT news";
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expectNoErrors(parser);
        expect(ast instanceof ExpressionAST).toBeTruthy();
        expect(ast.left instanceof TermAST).toBeTruthy();
        expect(ast.op.type).toBe(TokenType.OR);
        expect(ast.right instanceof ExpressionAST).toBeTruthy();

        var firstAndExpression = ast.right;
        expect(firstAndExpression.left instanceof TermAST).toBeTruthy();
        expect(firstAndExpression.op.type).toBe(TokenType.AND);
        expect(firstAndExpression.right instanceof ExpressionAST).toBeTruthy();

        var secondAndExpression = firstAndExpression.right;
        expect(secondAndExpression.left instanceof TermAST).toBeTruthy();
        expect(secondAndExpression.op.type).toBe(TokenType.AND);
        expect(secondAndExpression.right instanceof ModifierAST).toBeTruthy();

        var notExpression = secondAndExpression.right;
        expect(notExpression.modifier.type).toBe(TokenType.NOT);
        expect(notExpression.right instanceof TermAST).toBeTruthy();

        expectIdentityDump(query);
    });

    it('reports an error when AND is used with NOT and no term follows', function() {
        var query = 'AND NOT';
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expect(parser.errors.length).toBe(2);
        expect(parser.errors[0].message).toBe("Missing left side of expression");
        expect(parser.errors[0].position).toBe(0);
        expect(parser.errors[1].message).toBe("Missing right side of expression");
        expect(parser.errors[1].position).toBe(7);
        expectIdentityDump(query, true);
    });

    it('can parse a MUST expression', function () {
        var query = "+submit";
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expectNoErrors(parser);
        expect(ast instanceof ModifierAST).toBeTruthy();
        expect(ast.modifier.type).toBe(TokenType.MUST);
        expect(ast.right instanceof TermAST).toBeTruthy();

        expectIdentityDump(query);
    });

    it('can parse a MUST_NOT expression', function () {
        var query = "-submit";
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expectNoErrors(parser);
        expect(ast instanceof ModifierAST).toBeTruthy();
        expect(ast.modifier.type).toBe(TokenType.MUST_NOT);
        expect(ast.right instanceof TermAST).toBeTruthy();

        expectIdentityDump(query);
    });

    // TODO: This kind of query should not create any errors, the modifier seems to be silently ignored. It applies to +, - and !
    it('does not recognise MUST as modifier when followed by whitespace', function() {
        var query = '+ login';
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expectIdentityDump(query, true);
    });

    it('reports an error on double MUST or MUST_NOT modifier', function() {
        var query = '+-login';
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expect(parser.errors.length).toBe(1);
        expect(parser.errors[0].message).toBe("Missing right side of expression");
        expect(parser.errors[0].position).toBe(1);
        expectIdentityDump(query, true);
    });

    it('+ and - only work as modifiers at the beginning of the term', function() {
        var query = "start+-end";
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expectNoErrors(parser);
        expect(ast instanceof TermAST).toBeTruthy();
    });

    it('can parse an inclusive range search', function() {
        var query = "[400 TO 500]";
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expectNoErrors(parser);
        expect(ast instanceof TermAST).toBeTruthy();
        expect(ast.term.type).toBe(TokenType.TERM);
        expect(ast.isInclusiveRange()).toBeTruthy();
        expect(ast.isExclusiveRange()).toBeFalsy();
        expectIdentityDump(query);
    });

    it('can parse an inclusive range search preceded by a field', function() {
        var query = "http_response_code:[400 TO 500]";
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expectNoErrors(parser);
        expect(ast instanceof TermWithFieldAST).toBeTruthy();
        expect(ast.field.type).toBe(TokenType.TERM);
        expect(ast.term.type).toBe(TokenType.TERM);
        expect(ast.isInclusiveRange()).toBeTruthy();
        expect(ast.isExclusiveRange()).toBeFalsy();
        expectIdentityDump(query);
    });

    it('can parse an exclusive range search', function() {
        var query = "{alpha TO omega}";
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expectNoErrors(parser);
        expect(ast instanceof TermAST).toBeTruthy();
        expect(ast.term.type).toBe(TokenType.TERM);
        expect(ast.isInclusiveRange()).toBeFalsy();
        expect(ast.isExclusiveRange()).toBeTruthy();
        expectIdentityDump(query);
    });

    it('can parse an exclusive range search preceded by a field', function() {
        var query = "character:{alpha TO omega}";
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expectNoErrors(parser);
        expect(ast instanceof TermWithFieldAST).toBeTruthy();
        expect(ast.field.type).toBe(TokenType.TERM);
        expect(ast.term.type).toBe(TokenType.TERM);
        expect(ast.isInclusiveRange()).toBeFalsy();
        expect(ast.isExclusiveRange()).toBeTruthy();
        expectIdentityDump(query);
    });

    it('reports an error when the range brackets are not closed', function() {
        var query = "[400 TO 500";
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expect(parser.errors.length).toBe(1);
        expectIdentityDump(query, true);
    });

    it('reports an error when the range brackets do not match', function() {
        var query = "{400 TO 500]";
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expect(parser.errors.length).toBe(1);
        expectIdentityDump(query, true);
    });
});


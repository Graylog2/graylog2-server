'use strict';

var queryParser = require("../../src/logic/search/queryParser");

describe('Query Parser', function () {
    var ExpressionAST, ExpressionListAST, TermAST, Token, QueryParser, TokenType;

    var expectNoErrors = function (parser) {
        expect(parser.errors.length).toBe(0);
    };

    function expectIdentityDump(query, ignoreErrors) {
        var parser = new QueryParser(query);
        var ast = parser.parse();
        var dumpVisitor = new queryParser.DumpVisitor();
        dumpVisitor.visit(ast);
        var dumped = dumpVisitor.result();
        if (!ignoreErrors) {
            expectNoErrors(parser);
        }
        expect(dumped).toBe(query);
    }

    beforeEach(function () {
        TermAST = queryParser.TermAST;
        Token = queryParser.Token;
        QueryParser = queryParser.QueryParser;
        TokenType = queryParser.TokenType;
        ExpressionListAST = queryParser.ExpressionListAST;
        ExpressionAST = queryParser.ExpressionAST;
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
        expect(parser.errors[0].position).toBe(9);
        expectIdentityDump(query, true);
    });

    it('reports an error when field is missing term', function () {
        var query = 'action : ';
        var parser = new QueryParser(query);
        var ast = parser.parse();
        expect(parser.errors.length).toBe(1);
        expect(parser.errors[0].message).toBe("Missing term or phrase for field");
        expect(parser.errors[0].position).toBe(8);
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

});


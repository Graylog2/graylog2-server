const queryParser = require('logic/search/queryParser');
const DumpVisitor = require('logic/search/visitors/DumpVisitor');

describe('Query Parser', () => {
  let ExpressionAST; let ExpressionListAST; let ModifierAST; let TermAST; let TermWithFieldAST; let Token; let QueryParser; let
    TokenType;

  const expectNoErrors = function (parser) {
    expect(parser.errors.length).toBe(0);
  };

  function expectIdentityDump(query, ignoreErrors) {
    const parser = new QueryParser(query);
    const ast = parser.parse();
    const dumpVisitor = new DumpVisitor();
    dumpVisitor.visit(ast);
    const dumped = dumpVisitor.result();
    if (!ignoreErrors) {
      expectNoErrors(parser);
    }
    expect(dumped).toBe(query);
  }

  beforeEach(() => {
    TermAST = queryParser.TermAST;
    TermWithFieldAST = queryParser.TermWithFieldAST;
    Token = queryParser.Token;
    QueryParser = queryParser.QueryParser;
    TokenType = queryParser.TokenType;
    ExpressionListAST = queryParser.ExpressionListAST;
    ExpressionAST = queryParser.ExpressionAST;
    ModifierAST = queryParser.ModifierAST;
  });

  it('can parse a term', () => {
    const query = 'login';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expectNoErrors(parser);
    expect(ast instanceof TermAST).toBeTruthy();
    expect(ast.term instanceof Token).toBeTruthy();
    expect(ast.term.type).toBe(TokenType.TERM);
    expect(ast.term.asString()).toBe(query);
    expectIdentityDump(query);
  });

  it('can parse two terms', () => {
    const query = 'login submit';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expect(ast instanceof ExpressionListAST).toBeTruthy();
    expect(ast.expressions.length).toBe(2);
    const firstExpr = ast.expressions[0];
    expect(firstExpr instanceof TermAST).toBeTruthy();
    expectIdentityDump(query);
  });

  it('can parse a term in a field', () => {
    const query = 'action:login';
    expectIdentityDump(query);
  });

  it('can parse a phrase in a field', () => {
    const query = 'action:"login now"';
    expectIdentityDump(query);
  });

  it('tolerates ws before and after colon in a field expression', () => {
    const query = 'action : login';
    expectIdentityDump(query);
  });

  it('can parse a phrase', () => {
    const query = '"login now"';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expectNoErrors(parser);
    expect(ast instanceof TermAST).toBeTruthy();
    expect(ast.isPhrase()).toBeTruthy();
    expect(ast.term instanceof Token).toBeTruthy();
    expect(ast.term.type).toBe(TokenType.PHRASE);
    expect(ast.term.asString()).toBe(query);
    expectIdentityDump(query);
  });

  it('can parse a phrase and a term', () => {
    const query = '"login now" submit';
    expectIdentityDump(query);
  });

  it('can parse a term and a phrase', () => {
    const query = 'submit "login now"';
    expectIdentityDump(query);
  });

  it('can parse many terms with ws', () => {
    const query = '  submit  "login now" logout ';
    expectIdentityDump(query);
  });

  it('can parse an OR expression', () => {
    const query = 'login OR submit';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expectNoErrors(parser);
    expect(ast instanceof ExpressionAST).toBeTruthy();
    expect(ast.left instanceof TermAST).toBeTruthy();
    expect(ast.left.term.type).toBe(TokenType.TERM);

    expect(ast.op.type).toBe(TokenType.OR);
    expect(ast.right instanceof TermAST).toBeTruthy();

    expectIdentityDump(query);
  });

  it('preserves whitespace on dump of simple expression', () => {
    const query = ' login ';
    expectIdentityDump(query);
  });

  it('preserves whitespace on dump of complex expression', () => {
    const query = '  login  OR  \n \t  submit   ';
    expectIdentityDump(query);
  });

  it('can parse an AND expression', () => {
    const query = 'login AND submit';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expectNoErrors(parser);
    expect(ast instanceof ExpressionAST).toBeTruthy();
    expect(ast.left instanceof TermAST).toBeTruthy();
    expect(ast.left.term.type).toBe(TokenType.TERM);

    expect(ast.op.type).toBe(TokenType.AND);
    expect(ast.right instanceof TermAST).toBeTruthy();

    expectIdentityDump(query);
  });

  it('can parse a complex AND expression', () => {
    const query = 'login AND submit AND action:login';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expectNoErrors(parser);
    expect(ast instanceof ExpressionAST).toBeTruthy();
    expect(ast.left instanceof TermAST).toBeTruthy();
    expect(ast.left.term.type).toBe(TokenType.TERM);

    expect(ast.op.type).toBe(TokenType.AND);
    expect(ast.right instanceof ExpressionAST).toBeTruthy();

    const rightExpressionAST = ast.right;
    expect(rightExpressionAST instanceof ExpressionAST).toBeTruthy();
    expect(rightExpressionAST.left instanceof TermAST).toBeTruthy();
    expect(rightExpressionAST.left.term.type).toBe(TokenType.TERM);

    expect(rightExpressionAST.op.type).toBe(TokenType.AND);
    expect(rightExpressionAST.right instanceof TermAST).toBeTruthy();

    expectIdentityDump(query);
  });

  it('can parse an && expression', () => {
    const query = 'login && submit';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expectNoErrors(parser);
    expect(ast instanceof ExpressionAST).toBeTruthy();
    expect(ast.left instanceof TermAST).toBeTruthy();
    expect(ast.left.term.type).toBe(TokenType.TERM);

    expect(ast.op.type).toBe(TokenType.AND);
    expect(ast.right instanceof TermAST).toBeTruthy();

    expectIdentityDump(query);
  });

  it('can parse an || expression', () => {
    const query = 'login || submit';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expectNoErrors(parser);
    expect(ast instanceof ExpressionAST).toBeTruthy();
    expect(ast.left instanceof TermAST).toBeTruthy();
    expect(ast.left.term.type).toBe(TokenType.TERM);

    expect(ast.op.type).toBe(TokenType.OR);
    expect(ast.right instanceof TermAST).toBeTruthy();

    expectIdentityDump(query);
  });

  it('reports an error when right side of AND is missing', () => {
    const query = 'login AND ';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expect(parser.errors.length).toBe(1);
    expect(parser.errors[0].message).toBe('Missing right side of expression');
    expect(parser.errors[0].position).toBe(10);
    expectIdentityDump(query, true);
  });

  it('reports an error when left side of AND is missing', () => {
    const query = ' AND login';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expect(parser.errors.length).toBe(1);
    expect(parser.errors[0].message).toBe('Missing left side of expression');
    expect(parser.errors[0].position).toBe(1);
    expectIdentityDump(query, true);
  });

  it('reports an error when field is missing term', () => {
    const query = 'action : ';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expect(parser.errors.length).toBe(1);
    expect(parser.errors[0].message).toBe('Missing term or phrase for field');
    expect(parser.errors[0].position).toBe(9);
    expectIdentityDump(query, true);
  });

  it('reports an error two OR are together with no term in between', () => {
    const query = 'login OR OR submit';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expect(parser.errors.length).toBe(2);
    expect(parser.errors[0].message).toBe('Missing right side of expression');
    expect(parser.errors[0].position).toBe(9);
    expect(parser.errors[1].message).toBe('Missing left side of expression');
    expect(parser.errors[1].position).toBe(9);
    expectIdentityDump(query, true);
  });

  it('trailing escape character gives error, but can still reproduce', () => {
    const query = '\\';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expect(parser.errors.length).toBe(1);
    expect(parser.errors[0].message).toBe("Unexpected input: '\\'");
    expect(parser.errors[0].position).toBe(0);
    expectIdentityDump(query, true);
  });

  it('can parse empty query', () => {
    const query = '';
    expectIdentityDump(query);
  });

  it('can reproduce pure WS', () => {
    const query = ' \n\t';
    expectIdentityDump(query);
  });

  // none of those, really:
  // +-!():^[]"{}~*?\\/
  it('can parse everything except for ws and special characters as term', () => {
    const query = ' @$%&§öäüß#=.;_<>°"';
    expectIdentityDump(query);
  });

  it('can parse + and - literally inside term', () => {
    const query = 'start+-end"';
    expectIdentityDump(query);
  });

  it('can parse wildcards inside term', () => {
    const query = 'st?rt *middle* end?"';
    expectIdentityDump(query);
  });

  it('can parse a unary NOT expression', () => {
    const query = 'NOT submit';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expectNoErrors(parser);
    expect(ast instanceof ModifierAST).toBeTruthy();
    expect(ast.modifier.type).toBe(TokenType.NOT);
    expect(ast.right instanceof TermAST).toBeTruthy();

    expectIdentityDump(query);
  });

  it('can parse a complex expression with NOT, AND and OR', () => {
    const query = 'NOT submit AND action:login OR action:logout';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expectNoErrors(parser);
    expect(ast instanceof ModifierAST).toBeTruthy();
    expect(ast.modifier.type).toBe(TokenType.NOT);
    expect(ast.right instanceof ExpressionAST).toBeTruthy();

    const andAST = ast.right;
    expect(andAST instanceof ExpressionAST).toBeTruthy();
    expect(andAST.left instanceof TermAST).toBeTruthy();
    expect(andAST.op.type).toBe(TokenType.AND);
    expect(andAST.right instanceof ExpressionAST).toBeTruthy();

    const orAST = andAST.right;
    expect(orAST instanceof ExpressionAST).toBeTruthy();
    expect(orAST.left instanceof TermAST).toBeTruthy();
    expect(orAST.op.type).toBe(TokenType.OR);
    expect(orAST.right instanceof TermAST).toBeTruthy();

    expectIdentityDump(query);
  });

  it('reports an error when NOT has no right part', () => {
    const query = 'NOT ';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expect(parser.errors.length).toBe(1);
    expect(parser.errors[0].message).toBe('Missing right side of expression');
    expect(parser.errors[0].position).toBe(4);
    expectIdentityDump(query, true);
  });

  it('reports an error on double NOT', () => {
    const query = 'NOT NOT login';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expect(parser.errors.length).toBe(1);
    expect(parser.errors[0].message).toBe('Missing right side of expression');
    expect(parser.errors[0].position).toBe(4);
    expectIdentityDump(query, true);
  });

  it('can parse a NOT expression preceded by a term', () => {
    const query = 'login NOT submit';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expectNoErrors(parser);
    expect(ast instanceof ExpressionListAST).toBeTruthy();
    expect(ast.expressions.length).toBe(2);

    const firstExpr = ast.expressions[0];
    expect(firstExpr instanceof TermAST).toBeTruthy();

    const secondExpr = ast.expressions[1];
    expect(secondExpr instanceof ModifierAST).toBeTruthy();
    expect(secondExpr.modifier.type).toBe(TokenType.NOT);
    expect(secondExpr.right instanceof TermAST).toBeTruthy();

    expectIdentityDump(query);
  });

  it('can parse a ! as NOT expression', () => {
    const query = 'login !submit';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expectNoErrors(parser);
    expect(ast instanceof ExpressionListAST).toBeTruthy();
    expect(ast.expressions.length).toBe(2);

    const firstExpr = ast.expressions[0];
    expect(firstExpr instanceof TermAST).toBeTruthy();

    const secondExpr = ast.expressions[1];
    expect(secondExpr instanceof ModifierAST).toBeTruthy();
    expect(secondExpr.modifier.type).toBe(TokenType.NOT);
    expect(secondExpr.right instanceof TermAST).toBeTruthy();

    expectIdentityDump(query);
  });

  it('can parse a NOT expression preceded by other expressions', () => {
    const query = 'action:login OR action:logout NOT submit';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expectNoErrors(parser);
    expect(ast instanceof ExpressionListAST).toBeTruthy();
    expect(ast.expressions.length).toBe(2);

    const firstExpr = ast.expressions[0];
    expect(firstExpr instanceof ExpressionAST).toBeTruthy();
    expect(firstExpr.left instanceof TermAST).toBeTruthy();
    expect(firstExpr.op.type).toBe(TokenType.OR);
    expect(firstExpr.right instanceof TermAST).toBeTruthy();

    const secondExpr = ast.expressions[1];
    expect(secondExpr instanceof ModifierAST).toBeTruthy();
    expect(secondExpr.modifier.type).toBe(TokenType.NOT);
    expect(secondExpr.right instanceof TermAST).toBeTruthy();

    expectIdentityDump(query);
  });

  it('can parse a chain of NOT expressions', () => {
    const query = 'login NOT submit NOT logout NOT now';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expectNoErrors(parser);
    expect(ast instanceof ExpressionListAST).toBeTruthy();
    expect(ast.expressions.length).toBe(4);

    const firstExpr = ast.expressions[0];
    expect(firstExpr instanceof TermAST).toBeTruthy();

    for (let i = 1; i < ast.expressions.length; i++) {
      const nextExpr = ast.expressions[i];
      expect(nextExpr instanceof ModifierAST).toBeTruthy();
      expect(nextExpr.modifier.type).toBe(TokenType.NOT);
      expect(nextExpr.right instanceof TermAST).toBeTruthy();
    }

    expectIdentityDump(query);
  });

  it('can parse ! at the beginning of the query', () => {
    const query = '!login';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expectNoErrors(parser);
    expect(ast instanceof ModifierAST).toBeTruthy();
    expect(ast.modifier.type).toBe(TokenType.NOT);
    expect(ast.right instanceof TermAST).toBeTruthy();
    expectIdentityDump(query);
  });

  it('can parse a query where AND is followed by NOT', () => {
    const query = 'quick OR brown AND fox AND NOT news';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expectNoErrors(parser);
    expect(ast instanceof ExpressionAST).toBeTruthy();
    expect(ast.left instanceof TermAST).toBeTruthy();
    expect(ast.op.type).toBe(TokenType.OR);
    expect(ast.right instanceof ExpressionAST).toBeTruthy();

    const firstAndExpression = ast.right;
    expect(firstAndExpression.left instanceof TermAST).toBeTruthy();
    expect(firstAndExpression.op.type).toBe(TokenType.AND);
    expect(firstAndExpression.right instanceof ExpressionAST).toBeTruthy();

    const secondAndExpression = firstAndExpression.right;
    expect(secondAndExpression.left instanceof TermAST).toBeTruthy();
    expect(secondAndExpression.op.type).toBe(TokenType.AND);
    expect(secondAndExpression.right instanceof ModifierAST).toBeTruthy();

    const notExpression = secondAndExpression.right;
    expect(notExpression.modifier.type).toBe(TokenType.NOT);
    expect(notExpression.right instanceof TermAST).toBeTruthy();

    expectIdentityDump(query);
  });

  it('reports an error when AND is used with NOT and no term follows', () => {
    const query = 'AND NOT';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expect(parser.errors.length).toBe(2);
    expect(parser.errors[0].message).toBe('Missing left side of expression');
    expect(parser.errors[0].position).toBe(0);
    expect(parser.errors[1].message).toBe('Missing right side of expression');
    expect(parser.errors[1].position).toBe(7);
    expectIdentityDump(query, true);
  });

  it('can parse a MUST expression', () => {
    const query = '+submit';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expectNoErrors(parser);
    expect(ast instanceof ModifierAST).toBeTruthy();
    expect(ast.modifier.type).toBe(TokenType.MUST);
    expect(ast.right instanceof TermAST).toBeTruthy();

    expectIdentityDump(query);
  });

  it('can parse a MUST_NOT expression', () => {
    const query = '-submit';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expectNoErrors(parser);
    expect(ast instanceof ModifierAST).toBeTruthy();
    expect(ast.modifier.type).toBe(TokenType.MUST_NOT);
    expect(ast.right instanceof TermAST).toBeTruthy();

    expectIdentityDump(query);
  });

  // TODO: This kind of query should not create any errors, the modifier seems to be silently ignored. It applies to +, - and !
  it('does not recognise MUST as modifier when followed by whitespace', () => {
    const query = '+ login';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expectIdentityDump(query, true);
  });

  it('reports an error on double MUST or MUST_NOT modifier', () => {
    const query = '+-login';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expect(parser.errors.length).toBe(1);
    expect(parser.errors[0].message).toBe('Missing right side of expression');
    expect(parser.errors[0].position).toBe(1);
    expectIdentityDump(query, true);
  });

  it('+ and - only work as modifiers at the beginning of the term', () => {
    const query = 'start+-end';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expectNoErrors(parser);
    expect(ast instanceof TermAST).toBeTruthy();
  });

  it('can parse an inclusive range search', () => {
    const query = '[400 TO 500]';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expectNoErrors(parser);
    expect(ast instanceof TermAST).toBeTruthy();
    expect(ast.term.type).toBe(TokenType.TERM);
    expect(ast.isInclusiveRange()).toBeTruthy();
    expect(ast.isExclusiveRange()).toBeFalsy();
    expectIdentityDump(query);
  });

  it('can parse an inclusive range search preceded by a field', () => {
    const query = 'http_response_code:[400 TO 500]';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expectNoErrors(parser);
    expect(ast instanceof TermWithFieldAST).toBeTruthy();
    expect(ast.field.type).toBe(TokenType.TERM);
    expect(ast.term.type).toBe(TokenType.TERM);
    expect(ast.isInclusiveRange()).toBeTruthy();
    expect(ast.isExclusiveRange()).toBeFalsy();
    expectIdentityDump(query);
  });


  it('can parse pure wildcard', () => {
    const query = 'http_response_code:*"';
    expectIdentityDump(query);
  });

  it('reports an error when using question mark as pure wildcard', () => {
    const query = 'http_response_code:?"';
    const parser = new QueryParser(query);
    parser.parse();
    expect(parser.errors.length).toBe(1);
    expectIdentityDump(query, true);
  });

  it('can parse an exclusive range search', () => {
    const query = '{alpha TO omega}';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expectNoErrors(parser);
    expect(ast instanceof TermAST).toBeTruthy();
    expect(ast.term.type).toBe(TokenType.TERM);
    expect(ast.isInclusiveRange()).toBeFalsy();
    expect(ast.isExclusiveRange()).toBeTruthy();
    expectIdentityDump(query);
  });

  it('can parse an exclusive range search preceded by a field', () => {
    const query = 'character:{alpha TO omega}';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expectNoErrors(parser);
    expect(ast instanceof TermWithFieldAST).toBeTruthy();
    expect(ast.field.type).toBe(TokenType.TERM);
    expect(ast.term.type).toBe(TokenType.TERM);
    expect(ast.isInclusiveRange()).toBeFalsy();
    expect(ast.isExclusiveRange()).toBeTruthy();
    expectIdentityDump(query);
  });

  it('reports an error when the range brackets are not closed', () => {
    const query = '[400 TO 500';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expect(parser.errors.length).toBe(1);
    expectIdentityDump(query, true);
  });

  it('reports an error when the range brackets do not match', () => {
    const query = '{400 TO 500]';
    const parser = new QueryParser(query);
    const ast = parser.parse();
    expect(parser.errors.length).toBe(1);
    expectIdentityDump(query, true);
  });
});

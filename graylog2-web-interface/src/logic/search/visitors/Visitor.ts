'use strict';

import queryParser = require('../queryParser');
// TODO: Is there a better way of importing all these classes? I don't want to prepend the module name everywhere.
import AST = queryParser.AST;
import MissingAST = queryParser.MissingAST;
import TermAST = queryParser.TermAST;
import TermWithFieldAST = queryParser.TermWithFieldAST;
import ExpressionAST = queryParser.ExpressionAST;
import ExpressionListAST = queryParser.ExpressionListAST;
import ModifierAST = queryParser.ModifierAST;

interface Visitor {
    visit(ast: AST);
    visitMissingAST(ast: MissingAST);
    visitTermAST(ast: TermAST);
    visitTermWithFieldAST(ast: TermWithFieldAST);
    visitModifierAST(ast: ModifierAST);
    visitExpressionAST(ast: ExpressionAST);
    visitExpressionListAST(ast: ExpressionListAST);
}

export = Visitor;

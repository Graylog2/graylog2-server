'use strict';

import BaseVisitor = require('./BaseVisitor');
import queryParser = require('../queryParser');
import AST = queryParser.AST;
import MissingAST = queryParser.MissingAST;
import TermAST = queryParser.TermAST;
import TermWithFieldAST = queryParser.TermWithFieldAST;
import ExpressionAST = queryParser.ExpressionAST;
import ExpressionListAST = queryParser.ExpressionListAST;
import ModifierAST = queryParser.ModifierAST;

class SerializeVisitor extends BaseVisitor {
    private serializedAST: AST[] = [];

    visitMissingAST(ast: MissingAST) {
        this.serialize(ast);
    }

    visitTermAST(ast: TermAST) {
        this.serialize(ast);
    }

    visitTermWithFieldAST(ast: TermWithFieldAST) {
        this.serialize(ast);
    }

    visitModifierAST(ast: ModifierAST) {
        this.serialize(ast);
        this.visit(ast.right);
    }

    visitExpressionAST(ast: ExpressionAST) {
        this.visit(ast.left);
        this.serialize(ast);
        this.visit(ast.right);
    }

    visitExpressionListAST(ast: ExpressionListAST) {
        this.serialize(ast);
        ast.expressions.forEach((expr) => this.visit(expr));
    }

    result() {
        return this.serializedAST;
    }

    private serialize(ast) {
        this.serializedAST.push(ast);
    }
}

export = SerializeVisitor;

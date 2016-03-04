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
import Token = queryParser.Token;

class DumpVisitor extends BaseVisitor {
    private buffer: string[] = [];
    private skipASTs: AST[] = [];

    constructor(...skipASTs: AST[]) {
        super();
        this.skipASTs = skipASTs;
    }

    visit(ast: AST) {
        if (this.skipASTs.indexOf(ast) !== -1) {
            return;
        } else {
            super.visit(ast);
        }
    }

    visitMissingAST(ast: MissingAST) {
        this.dumpPrefix(ast);
        this.dumpSuffix(ast);
    }

    visitTermAST(ast: TermAST) {
        this.dumpWithPrefixAndSuffix(ast);
    }

    visitTermWithFieldAST(ast: TermWithFieldAST) {
        this.dumpWithPrefixAndSuffixWithField(ast);
    }

    visitModifierAST(ast: ModifierAST) {
        this.dumpHidden(ast.hiddenModifierPrefix);
        this.dumpToken(ast.modifier);
        this.dumpHidden(ast.hiddenModifierSuffix);
        this.visit(ast.right);
    }

    visitExpressionAST(ast: ExpressionAST) {
        this.dumpPrefix(ast);
        this.visit(ast.left);
        this.dumpHidden(ast.hiddenOpPrefix);
        this.dumpToken(ast.op);
        this.dumpHidden(ast.hiddenOpSuffix);
        this.visit(ast.right);
        this.dumpSuffix(ast);
    }

    visitExpressionListAST(ast: ExpressionListAST) {
        this.dumpPrefix(ast);
        var exprList = <ExpressionListAST>ast;
        exprList.expressions.forEach((expr) => this.visit(expr));
        this.dumpSuffix(ast);
    }

    private dumpWithPrefixAndSuffix(ast: TermAST) {
        this.dumpPrefix(ast);
        this.dumpToken(ast.term);
        this.dumpSuffix(ast);
    }

    private dumpWithPrefixAndSuffixWithField(ast: TermWithFieldAST) {
        this.dumpPrefix(ast);
        this.dumpToken(ast.field);
        this.dumpHidden(ast.hiddenColonPrefix);
        this.dumpToken(ast.colon);
        this.dumpHidden(ast.hiddenColonSuffix);
        this.dumpToken(ast.term);
        this.dumpSuffix(ast);
    }

    private dumpSuffix(ast: AST) {
        this.dumpHidden(ast.hiddenSuffix);
    }

    private dumpHidden(hidden: Array<Token>) {
        hidden.forEach((token) => this.dumpToken(token));
    }

    private dumpPrefix(ast: AST) {
        this.dumpHidden(ast.hiddenPrefix);
    }

    private dumpToken(token: Token) {
        token !== null && this.buffer.push(token.asString());
    }

    result() {
        return this.buffer.join("");
    }
}

export = DumpVisitor;

'use strict';

import Visitor = require('./Visitor');
import queryParser = require('../queryParser');
// TODO: Is there a better way of importing all these classes? I don't want to prepend the module name everywhere.
import AST = queryParser.AST;
import MissingAST = queryParser.MissingAST;
import TermAST = queryParser.TermAST;
import TermWithFieldAST = queryParser.TermWithFieldAST;
import ExpressionAST = queryParser.ExpressionAST;
import ExpressionListAST = queryParser.ExpressionListAST;
import ModifierAST = queryParser.ModifierAST;

class BaseVisitor implements Visitor {
    visit(ast: AST) {
        if (ast === null) {
            return;
        } else if (ast instanceof ExpressionListAST) {
            this.visitExpressionListAST(<ExpressionListAST>ast);
        } else if (ast instanceof ExpressionAST) {
            this.visitExpressionAST(<ExpressionAST>ast);
        } else if (ast instanceof ModifierAST) {
            this.visitModifierAST(<ModifierAST>ast);
        } else if (ast instanceof TermWithFieldAST) {
            this.visitTermWithFieldAST(<TermWithFieldAST>ast);
        } else if (ast instanceof TermAST) {
            this.visitTermAST(<TermAST>ast);
        } else if (ast instanceof MissingAST) {
            this.visitMissingAST(<MissingAST>ast);
        } else {
            throw Error("Encountered AST of unknown type: " + JSON.stringify(ast));
        }
    }

    visitMissingAST(ast: MissingAST) {
    }

    visitTermAST(ast: TermAST) {
    }

    visitTermWithFieldAST(ast: TermWithFieldAST) {
    }

    visitModifierAST(ast: ModifierAST) {
    }

    visitExpressionAST(ast: ExpressionAST) {
    }

    visitExpressionListAST(ast: ExpressionListAST) {
    }
}

export = BaseVisitor;

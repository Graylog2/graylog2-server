package org.graylog.plugins.messageprocessor.parser;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.mina.util.IdentityHashSet;
import org.graylog.plugins.messageprocessor.ast.Rule;
import org.graylog.plugins.messageprocessor.ast.expressions.AndExpression;
import org.graylog.plugins.messageprocessor.ast.expressions.BinaryExpression;
import org.graylog.plugins.messageprocessor.ast.expressions.BooleanExpression;
import org.graylog.plugins.messageprocessor.ast.expressions.ComparisonExpression;
import org.graylog.plugins.messageprocessor.ast.expressions.DoubleExpression;
import org.graylog.plugins.messageprocessor.ast.expressions.EqualityExpression;
import org.graylog.plugins.messageprocessor.ast.expressions.Expression;
import org.graylog.plugins.messageprocessor.ast.expressions.FieldAccessExpression;
import org.graylog.plugins.messageprocessor.ast.expressions.FieldRefExpression;
import org.graylog.plugins.messageprocessor.ast.expressions.FunctionExpression;
import org.graylog.plugins.messageprocessor.ast.expressions.LogicalExpression;
import org.graylog.plugins.messageprocessor.ast.expressions.LongExpression;
import org.graylog.plugins.messageprocessor.ast.expressions.MessageRefExpression;
import org.graylog.plugins.messageprocessor.ast.expressions.NotExpression;
import org.graylog.plugins.messageprocessor.ast.expressions.OrExpression;
import org.graylog.plugins.messageprocessor.ast.expressions.StringExpression;
import org.graylog.plugins.messageprocessor.ast.expressions.VarRefExpression;
import org.graylog.plugins.messageprocessor.ast.statements.FunctionStatement;
import org.graylog.plugins.messageprocessor.ast.statements.Statement;
import org.graylog.plugins.messageprocessor.ast.statements.VarAssignStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RuleParser {

    private final FunctionRegistry functionRegistry;

    @Inject
    public RuleParser(FunctionRegistry functionRegistry) {
        this.functionRegistry = functionRegistry;
    }

    private static final Logger log = LoggerFactory.getLogger(RuleParser.class);
    public static final ParseTreeWalker WALKER = ParseTreeWalker.DEFAULT;

    public Rule parseRule(String rule) throws ParseException {
        final RuleLangLexer lexer = new RuleLangLexer(new ANTLRInputStream(rule));
        final RuleLangParser parser = new RuleLangParser(new CommonTokenStream(lexer));

        final RuleLangParser.RuleDeclarationContext ruleDeclaration = parser.ruleDeclaration();

        final ParseContext parseContext = new ParseContext();

        // parsing stages:
        // 1. build AST nodes, checks for invalid var, function refs
        // 2. checker: static type check w/ coercion nodes
        // 3. optimizer: TODO

        WALKER.walk(new AstBuilder(parseContext), ruleDeclaration);
        WALKER.walk(new TypeChecker(parseContext), ruleDeclaration);

        if (parseContext.getErrors().isEmpty()) {
            return parseContext.getRule();
        }
        throw new ParseException(parseContext.getErrors());
    }

    private class AstBuilder extends RuleLangBaseListener {

        private final ParseContext parseContext;
        private final ParseTreeProperty<Map<String, Expression>> args;
        private final ParseTreeProperty<Expression> exprs;

        private final Set<String> definedVars = Sets.newHashSet();

        // this is true for nested field accesses
        private boolean idIsFieldAccess = false;

        public AstBuilder(ParseContext parseContext) {
            this.parseContext = parseContext;
            args = parseContext.arguments();
            exprs = parseContext.expressions();
        }

        @Override
        public void exitFuncStmt(RuleLangParser.FuncStmtContext ctx) {
            final Expression expr = exprs.get(ctx.functionCall());
            final FunctionStatement functionStatement = new FunctionStatement(expr);
            parseContext.statements.add(functionStatement);
        }

        @Override
        public void exitVarAssignStmt(RuleLangParser.VarAssignStmtContext ctx) {
            final String name = ctx.varName.getText();
            final Expression expr = exprs.get(ctx.expression());
            definedVars.add(name);
            parseContext.statements.add(new VarAssignStatement(name, expr));
        }

        @Override
        public void exitFunctionCall(RuleLangParser.FunctionCallContext ctx) {
            final String name = ctx.funcName.getText();
            final Map<String, Expression> args = this.args.get(ctx.arguments());

            if (functionRegistry.resolve(name) == null) {
                parseContext.addError(new UndeclaredFunction(ctx));
            }

            final FunctionExpression expr = new FunctionExpression(name, args, functionRegistry.resolveOrError(name));

            log.info("FUNC: ctx {} => {}", ctx, expr);
            exprs.put(ctx, expr);
        }

        @Override
        public void exitNamedArgs(RuleLangParser.NamedArgsContext ctx) {
            final Map<String, Expression> argMap = Maps.newHashMap();
            final int argCount = ctx.Identifier().size();
            for (int i = 0; i < argCount; i++) {
                final String argName = ctx.Identifier(i).getText();
                final Expression argValue = exprs.get(ctx.expression(i));
                argMap.put(argName, argValue);
            }
            args.put(ctx, argMap);
        }

        @Override
        public void exitSingleDefaultArg(RuleLangParser.SingleDefaultArgContext ctx) {
            final Expression expr = exprs.get(ctx.expression());
            final HashMap<String, Expression> singleArg = Maps.newHashMap();
            // null key means to use the single declared argument for this function, it's syntactic sugar
            // this gets validated and expanded in a later parsing stage
            singleArg.put(null, expr);
            args.put(ctx, singleArg);
        }

        @Override
        public void exitRuleDeclaration(RuleLangParser.RuleDeclarationContext ctx) {
            final Rule.Builder ruleBuilder = Rule.builder();
            ruleBuilder.name(ctx.name.getText());
            if (ctx.stage != null) {
                ruleBuilder.stage(Integer.parseInt(ctx.stage.getText()));
            }
            ruleBuilder.when((LogicalExpression) exprs.get(ctx.condition));
            ruleBuilder.then(parseContext.statements);
            final Rule rule = ruleBuilder.build();
            parseContext.setRule(rule);
            log.info("Declaring rule {}", rule);
        }

        @Override
        public void enterNested(RuleLangParser.NestedContext ctx) {
            // nested field access is ok, these are not rule variables
            idIsFieldAccess = true;
        }

        @Override
        public void exitNested(RuleLangParser.NestedContext ctx) {
            idIsFieldAccess = false; // reset for error checks
            final Expression object = exprs.get(ctx.fieldSet);
            final Expression field = exprs.get(ctx.field);
            final FieldAccessExpression expr = new FieldAccessExpression(object, field);
            log.info("NOT: ctx {} => {}", ctx, expr);
            exprs.put(ctx, expr);
        }

        @Override
        public void exitNot(RuleLangParser.NotContext ctx) {
            final LogicalExpression expression = (LogicalExpression) exprs.get(ctx.expression());
            final NotExpression expr = new NotExpression(expression);
            log.info("NOT: ctx {} => {}", ctx, expr);
            exprs.put(ctx, expr);
        }

        @Override
        public void exitAnd(RuleLangParser.AndContext ctx) {
            final LogicalExpression left = (LogicalExpression) exprs.get(ctx.left);
            final LogicalExpression right = (LogicalExpression) exprs.get(ctx.right);
            final AndExpression expr = new AndExpression(left, right);
            log.info("AND: ctx {} => {}", ctx, expr);
            exprs.put(ctx, expr);
        }

        @Override
        public void exitOr(RuleLangParser.OrContext ctx) {
            final LogicalExpression left = (LogicalExpression) exprs.get(ctx.left);
            final LogicalExpression right = (LogicalExpression) exprs.get(ctx.right);
            final OrExpression expr = new OrExpression(left, right);
            log.info("OR: ctx {} => {}", ctx, expr);
            exprs.put(ctx, expr);
        }

        @Override
        public void exitEquality(RuleLangParser.EqualityContext ctx) {
            final Expression left = exprs.get(ctx.left);
            final Expression right = exprs.get(ctx.right);
            final boolean equals = ctx.equality.getText().equals("==");
            final EqualityExpression expr = new EqualityExpression(left, right, equals);
            log.info("EQUAL: ctx {} => {}", ctx, expr);
            exprs.put(ctx, expr);
        }

        @Override
        public void exitComparison(RuleLangParser.ComparisonContext ctx) {
            final Expression left = exprs.get(ctx.left);
            final Expression right = exprs.get(ctx.right);
            final String operator = ctx.comparison.getText();
            final ComparisonExpression expr = new ComparisonExpression(left, right, operator);
            log.info("COMPARE: ctx {} => {}", ctx, expr);
            exprs.put(ctx, expr);
        }

        @Override
        public void exitInteger(RuleLangParser.IntegerContext ctx) {
            final LongExpression expr = new LongExpression(Long.parseLong(ctx.getText()));
            log.info("INT: ctx {} => {}", ctx, expr);
            exprs.put(ctx, expr);
        }

        @Override
        public void exitFloat(RuleLangParser.FloatContext ctx) {
            final DoubleExpression expr = new DoubleExpression(Double.parseDouble(ctx.getText()));
            log.info("FLOAT: ctx {} => {}", ctx, expr);
            exprs.put(ctx, expr);
        }

        @Override
        public void exitChar(RuleLangParser.CharContext ctx) {
            // TODO
            super.exitChar(ctx);
        }

        @Override
        public void exitString(RuleLangParser.StringContext ctx) {
            final String text = ctx.getText();
            final StringExpression expr = new StringExpression(text.substring(1, text.length()-1));
            log.info("STRING: ctx {} => {}", ctx, expr);
            exprs.put(ctx, expr);
        }

        @Override
        public void exitBoolean(RuleLangParser.BooleanContext ctx) {
            final BooleanExpression expr = new BooleanExpression(Boolean.valueOf(ctx.getText()));
            log.info("BOOL: ctx {} => {}", ctx, expr);
            exprs.put(ctx, expr);
        }

        @Override
        public void exitLiteralPrimary(RuleLangParser.LiteralPrimaryContext ctx) {
            // nothing to do, just propagate the ConstantExpression
            exprs.put(ctx, exprs.get(ctx.literal()));
            parseContext.addInnerNode(ctx);
        }

        @Override
        public void exitParenExpr(RuleLangParser.ParenExprContext ctx) {
            // nothing to do, just propagate
            exprs.put(ctx, exprs.get(ctx.expression()));
            parseContext.addInnerNode(ctx);
        }

        @Override
        public void exitMessageRef(RuleLangParser.MessageRefContext ctx) {
            final MessageRefExpression expr = new MessageRefExpression();
            log.info("$MSG: ctx {} => {}", ctx, expr);
            exprs.put(ctx, expr);
        }

        @Override
        public void exitIdentifier(RuleLangParser.IdentifierContext ctx) {
            final String variableName = ctx.Identifier().getText();
            if (!idIsFieldAccess && !definedVars.contains(variableName)) {
                parseContext.addError(new UndeclaredVariable(ctx));
            }
            final Expression expr;
            if (idIsFieldAccess) {
                expr = new FieldRefExpression(variableName);
            } else {
                expr = new VarRefExpression(variableName);
            }
            log.info("VAR: ctx {} => {}", ctx, expr);
            exprs.put(ctx, expr);
        }

        @Override
        public void exitPrimaryExpression(RuleLangParser.PrimaryExpressionContext ctx) {
            // nothing to do, just propagate
            exprs.put(ctx, exprs.get(ctx.primary()));
            parseContext.addInnerNode(ctx);
        }

        @Override
        public void exitFunc(RuleLangParser.FuncContext ctx) {
            // nothing to do, just propagate
            exprs.put(ctx, exprs.get(ctx.functionCall()));
            parseContext.addInnerNode(ctx);
        }

        @Override
        public void exitNull(RuleLangParser.NullContext ctx) {
            // TODO
            super.exitNull(ctx);
        }
    }

    private class TypeChecker extends RuleLangBaseListener {
        private final ParseContext parseContext;
        StringBuffer sb = new StringBuffer();
        public TypeChecker(ParseContext parseContext) {
            this.parseContext = parseContext;
        }

        @Override
        public void exitRuleDeclaration(RuleLangParser.RuleDeclarationContext ctx) {
            log.info("Type tree {}", sb.toString());
        }

        @Override
        public void exitAnd(RuleLangParser.AndContext ctx) {
            checkBinaryExpression(ctx);
        }

        @Override
        public void exitOr(RuleLangParser.OrContext ctx) {
            checkBinaryExpression(ctx);
        }

        @Override
        public void exitComparison(RuleLangParser.ComparisonContext ctx) {
            checkBinaryExpression(ctx);
        }

        @Override
        public void exitEquality(RuleLangParser.EqualityContext ctx) {
            checkBinaryExpression(ctx);
        }

        private void checkBinaryExpression(RuleLangParser.ExpressionContext ctx) {
            final BinaryExpression binaryExpr = (BinaryExpression) parseContext.expressions().get(ctx);
            final Class leftType = binaryExpr.left().getType();
            final Class rightType = binaryExpr.right().getType();

            if (!leftType.equals(rightType)) {
                parseContext.addError(new IncompatibleTypes(ctx, binaryExpr));
            }
        }

        @Override
        public void enterEveryRule(ParserRuleContext ctx) {
            final Expression expression = parseContext.expressions().get(ctx);
            if (expression != null && !parseContext.isInnerNode(ctx)) {
                sb.append(" ( ");
                sb.append(expression.getClass().getSimpleName());
                sb.append(":").append(ctx.getClass().getSimpleName()).append(" ");
                sb.append(" <").append(expression.getType().getSimpleName()).append(">");
            }
        }

        @Override
        public void exitEveryRule(ParserRuleContext ctx) {
            final Expression expression = parseContext.expressions().get(ctx);
            if (expression != null && !parseContext.isInnerNode(ctx)) {
                sb.append(" ) ");
            }
        }

    }

    /**
     * Contains meta data about the parse tree, such as AST nodes, link to the function registry etc.
     *
     * Being used by tree walkers or visitors to perform AST construction, type checking and so on.
     */
    private static class ParseContext {
        private final ParseTreeProperty<Expression> exprs = new ParseTreeProperty<>();
        private final ParseTreeProperty<Map<String, Expression>> args = new ParseTreeProperty<>();
        private Set<ParseError> errors = Sets.newHashSet();
        // inner nodes in the parse tree will be ignored during type checker printing, they only transport type information
        private Set<RuleContext> innerNodes = new IdentityHashSet<>();
        public List<Statement> statements = Lists.newArrayList();
        public Rule rule;

        public ParseTreeProperty<Expression> expressions() {
            return exprs;
        }

        public ParseTreeProperty<Map<String, Expression>> arguments() {
            return args;
        }

        public Rule getRule() {
            return rule;
        }

        public void setRule(Rule rule) {
            this.rule = rule;
        }

        public Set<ParseError> getErrors() {
            return errors;
        }

        public void addError(ParseError error) {
            errors.add(error);
        }

        public void addInnerNode(RuleContext node) {
            innerNodes.add(node);
        }

        public boolean isInnerNode(RuleContext node) {
            return innerNodes.contains(node);
        }
    }

}

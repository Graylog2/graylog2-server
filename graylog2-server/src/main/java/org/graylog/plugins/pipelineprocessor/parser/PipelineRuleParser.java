/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.pipelineprocessor.parser;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.mina.util.IdentityHashSet;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.Stage;
import org.graylog.plugins.pipelineprocessor.ast.exceptions.PrecomputeFailure;
import org.graylog.plugins.pipelineprocessor.ast.expressions.AdditionExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.AndExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.ArrayLiteralExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.BinaryExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.BooleanExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.BooleanValuedFunctionWrapper;
import org.graylog.plugins.pipelineprocessor.ast.expressions.ComparisonExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.DoubleExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.EqualityExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.Expression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.FieldAccessExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.FieldRefExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.FunctionExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.IndexedAccessExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.LogicalExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.LongExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.MapLiteralExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.MessageRefExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.MultiplicationExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.NotExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.OrExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.SignedExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.StringExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.VarRefExpression;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.statements.FunctionStatement;
import org.graylog.plugins.pipelineprocessor.ast.statements.Statement;
import org.graylog.plugins.pipelineprocessor.ast.statements.VarAssignStatement;
import org.graylog.plugins.pipelineprocessor.codegen.CodeGenerator;
import org.graylog.plugins.pipelineprocessor.codegen.GeneratedRule;
import org.graylog.plugins.pipelineprocessor.codegen.PipelineClassloader;
import org.graylog.plugins.pipelineprocessor.parser.errors.IncompatibleArgumentType;
import org.graylog.plugins.pipelineprocessor.parser.errors.IncompatibleIndexType;
import org.graylog.plugins.pipelineprocessor.parser.errors.IncompatibleType;
import org.graylog.plugins.pipelineprocessor.parser.errors.IncompatibleTypes;
import org.graylog.plugins.pipelineprocessor.parser.errors.InvalidFunctionArgument;
import org.graylog.plugins.pipelineprocessor.parser.errors.InvalidOperation;
import org.graylog.plugins.pipelineprocessor.parser.errors.MissingRequiredParam;
import org.graylog.plugins.pipelineprocessor.parser.errors.NonIndexableType;
import org.graylog.plugins.pipelineprocessor.parser.errors.OptionalParametersMustBeNamed;
import org.graylog.plugins.pipelineprocessor.parser.errors.ParseError;
import org.graylog.plugins.pipelineprocessor.parser.errors.SyntaxError;
import org.graylog.plugins.pipelineprocessor.parser.errors.UndeclaredFunction;
import org.graylog.plugins.pipelineprocessor.parser.errors.UndeclaredVariable;
import org.graylog.plugins.pipelineprocessor.parser.errors.WrongNumberOfArgs;
import org.graylog.plugins.pipelineprocessor.processors.ConfigurationStateUpdater;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.collect.ImmutableSortedSet.orderedBy;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;

public class PipelineRuleParser {

    private final FunctionRegistry functionRegistry;
    private final CodeGenerator codeGenerator;

    private static AtomicLong uniqueId = new AtomicLong(0);

    @Inject
    public PipelineRuleParser(FunctionRegistry functionRegistry, CodeGenerator codeGenerator) {
        this.functionRegistry = functionRegistry;
        this.codeGenerator = codeGenerator;
    }

    private static final Logger log = LoggerFactory.getLogger(PipelineRuleParser.class);
    public static final ParseTreeWalker WALKER = ParseTreeWalker.DEFAULT;

    public Rule parseRule(String rule, boolean silent) throws ParseException {
        return parseRule(rule, silent, null);
    }

    public Rule parseRule(String rule, boolean silent, PipelineClassloader classLoader) throws ParseException {
        return parseRule("dummy" + uniqueId.getAndIncrement(), rule, silent, classLoader);
    }

    public Rule parseRule(String id, String rule, boolean silent) throws ParseException {
        return parseRule(id, rule, silent, null);
    }

    /**
     * Parses the given rule source and optionally generates a Java class for it if the classloader is not null.
     *
     * @param id the id of the rule, necessary to generate code
     * @param rule rule source code
     * @param silent don't emit status messages during parsing
     * @param ruleClassLoader the classloader to load the generated code into (can be null)
     * @return the parse rule
     * @throws ParseException if a one or more parse errors occur
     */
    public Rule parseRule(String id, String rule, boolean silent, PipelineClassloader ruleClassLoader) throws ParseException {
        final ParseContext parseContext = new ParseContext(silent);
        final SyntaxErrorListener errorListener = new SyntaxErrorListener(parseContext);

        final RuleLangLexer lexer = new RuleLangLexer(new ANTLRInputStream(rule));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);

        final RuleLangParser parser = new RuleLangParser(new CommonTokenStream(lexer));
        parser.setErrorHandler(new DefaultErrorStrategy());
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        final RuleLangParser.RuleDeclarationContext ruleDeclaration = parser.ruleDeclaration();


        // parsing stages:
        // 1. build AST nodes, checks for invalid var, function refs
        // 2. type annotator: infer type information from var refs, func refs
        // 3. checker: static type check w/ coercion nodes
        // 4. optimizer: TODO

        WALKER.walk(new RuleAstBuilder(parseContext), ruleDeclaration);
        WALKER.walk(new RuleTypeAnnotator(parseContext), ruleDeclaration);
        WALKER.walk(new RuleTypeChecker(parseContext), ruleDeclaration);

        if (parseContext.getErrors().isEmpty()) {
            Rule parsedRule = parseContext.getRules().get(0).withId(id);
            if (ruleClassLoader != null && ConfigurationStateUpdater.isAllowCodeGeneration()) {
                try {
                    final Class<? extends GeneratedRule> generatedClass = codeGenerator.generateCompiledRule(parsedRule, ruleClassLoader);
                    if (generatedClass != null) {
                        parsedRule = parsedRule.toBuilder().generatedRuleClass(generatedClass).build();
                    }
                } catch (Exception e) {
                    log.warn("Unable to compile rule {} to native code, falling back to interpreting it: {}", parsedRule.name(), e.getMessage());
                }
            }
            return parsedRule;
        }
        throw new ParseException(parseContext.getErrors());
    }

    public List<Pipeline> parsePipelines(String pipelines) throws ParseException {
        final ParseContext parseContext = new ParseContext(false);
        final SyntaxErrorListener errorListener = new SyntaxErrorListener(parseContext);

        final RuleLangLexer lexer = new RuleLangLexer(new ANTLRInputStream(pipelines));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);

        final RuleLangParser parser = new RuleLangParser(new CommonTokenStream(lexer));
        parser.setErrorHandler(new DefaultErrorStrategy());
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        final RuleLangParser.PipelineDeclsContext pipelineDeclsContext = parser.pipelineDecls();

        WALKER.walk(new PipelineAstBuilder(parseContext), pipelineDeclsContext);

        if (parseContext.getErrors().isEmpty()) {
            return parseContext.pipelines;
        }
        throw new ParseException(parseContext.getErrors());
    }

    public Pipeline parsePipeline(String id, String source) {
        final ParseContext parseContext = new ParseContext(false);
        final SyntaxErrorListener errorListener = new SyntaxErrorListener(parseContext);

        final RuleLangLexer lexer = new RuleLangLexer(new ANTLRInputStream(source));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);

        final RuleLangParser parser = new RuleLangParser(new CommonTokenStream(lexer));
        parser.setErrorHandler(new DefaultErrorStrategy());
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        final RuleLangParser.PipelineContext pipelineContext = parser.pipeline();

        WALKER.walk(new PipelineAstBuilder(parseContext), pipelineContext);

        if (parseContext.getErrors().isEmpty()) {
            final Pipeline pipeline = parseContext.pipelines.get(0);
            return pipeline.withId(id);
        }
        throw new ParseException(parseContext.getErrors());
    }

    public static String unquote(String string, char quoteChar) {
        if (string.length() >= 2 &&
                string.charAt(0) == quoteChar && string.charAt(string.length() - 1) == quoteChar) {
            return string.substring(1, string.length() - 1);
        }
        return string;
    }

    public static String unescape(String string) {
        return StringEscapeUtils.unescapeJava(string);
    }

    private static class SyntaxErrorListener extends BaseErrorListener {
        private final ParseContext parseContext;

        public SyntaxErrorListener(ParseContext parseContext) {
            this.parseContext = parseContext;
        }

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer,
                                Object offendingSymbol,
                                int line,
                                int charPositionInLine,
                                String msg,
                                RecognitionException e) {
            parseContext.addError(new SyntaxError(offendingSymbol, line, charPositionInLine, msg, e));
        }
    }


    private class RuleAstBuilder extends RuleLangBaseListener {

        private final ParseContext parseContext;
        private final ParseTreeProperty<Map<String, Expression>> args;
        private final ParseTreeProperty<List<Expression>> argsList;
        private final ParseTreeProperty<Expression> exprs;

        private final Set<String> definedVars = Sets.newHashSet();

        // this is true for nested field accesses
        private ArrayDeque<Boolean> isIdIsFieldAccess = new ArrayDeque<>();

        public RuleAstBuilder(ParseContext parseContext) {
            this.parseContext = parseContext;
            args = parseContext.arguments();
            argsList = parseContext.argumentLists();
            exprs = parseContext.expressions();
            isIdIsFieldAccess.push(false); // top of stack
        }

        @Override
        public void exitRuleDeclaration(RuleLangParser.RuleDeclarationContext ctx) {
            final Rule.Builder ruleBuilder = Rule.builder();
            ruleBuilder.name(unquote(ctx.name == null ? "" : ctx.name.getText(), '"'));
            final Expression expr = exprs.get(ctx.condition);

            LogicalExpression condition;
            if (expr instanceof LogicalExpression) {
                condition = (LogicalExpression) expr;
            } else if (expr != null && expr.getType().equals(Boolean.class)) {
                condition = new BooleanValuedFunctionWrapper(ctx.getStart(), expr);
            } else {
                condition = new BooleanExpression(ctx.getStart(), false);
                log.debug("Unable to create condition, replacing with 'false'");
            }
            ruleBuilder.when(condition);
            ruleBuilder.then(parseContext.statements);
            final Rule rule = ruleBuilder.build();
            parseContext.addRule(rule);
            log.trace("Declaring rule {}", rule);
        }

        @Override
        public void exitFuncStmt(RuleLangParser.FuncStmtContext ctx) {
            final Expression expr = exprs.get(ctx.functionCall());
            final FunctionStatement functionStatement = new FunctionStatement(expr);
            parseContext.statements.add(functionStatement);
        }

        @Override
        public void exitVarAssignStmt(RuleLangParser.VarAssignStmtContext ctx) {
            final String name = unquote(ctx.varName.getText(), '`');
            final Expression expr = exprs.get(ctx.expression());
            parseContext.defineVar(name, expr);
            definedVars.add(name);
            parseContext.statements.add(new VarAssignStatement(name, expr));
        }

        @Override
        public void exitFunctionCall(RuleLangParser.FunctionCallContext ctx) {
            final String name = ctx.funcName.getText();
            Map<String, Expression> argsMap = this.args.get(ctx.arguments());
            final List<Expression> positionalArgs = this.argsList.get(ctx.arguments());

            final Function<?> function = functionRegistry.resolve(name);
            if (function == null) {
                parseContext.addError(new UndeclaredFunction(ctx));
            } else {
                final ImmutableList<ParameterDescriptor> params = function.descriptor().params();
                final boolean hasOptionalParams = params.stream().anyMatch(ParameterDescriptor::optional);

                if (argsMap != null) {
                    // check for the right number of arguments to the function if the function only has required params
                    if (!hasOptionalParams && params.size() != argsMap.size()) {
                        parseContext.addError(new WrongNumberOfArgs(ctx, function, argsMap.size()));
                    } else {
                        // there are optional parameters, check that all required ones are present
                        final Map<String, Expression> givenArguments = argsMap;
                        final List<ParameterDescriptor> missingParams =
                                params.stream()
                                        .filter(p -> !p.optional())
                                        .map(p -> givenArguments.containsKey(p.name()) ? null : p)
                                        .filter(Objects::nonNull)
                                        .collect(toList());
                        for (ParameterDescriptor param : missingParams) {
                            parseContext.addError(new MissingRequiredParam(ctx, function, param));
                        }
                    }
                } else if (positionalArgs != null) {
                    // use descriptor to turn positional arguments into a map
                    argsMap = Maps.newHashMap();
                    // if we only have required parameters and the number doesn't match, complain
                    if (!hasOptionalParams && positionalArgs.size() != params.size()) {
                        parseContext.addError(new WrongNumberOfArgs(ctx, function, positionalArgs.size()));
                    }
                    // if optional parameters precede any required ones, the function must used named parameters
                    boolean hasError = false;
                    if (hasOptionalParams) {
                        // find the index of the first optional parameter
                        // then check if any non-optional come after it, if so, complain
                        int firstOptional = Integer.MAX_VALUE;
                        boolean requiredAfterOptional = false;
                        int i = 0;
                        for (ParameterDescriptor param : params) {
                            i++;
                            if (param.optional()) {
                                firstOptional = Math.min(firstOptional, i);
                            } else {
                                if (i > firstOptional) {
                                    requiredAfterOptional = true;
                                }
                            }
                        }
                        if (requiredAfterOptional) {
                            parseContext.addError(new OptionalParametersMustBeNamed(ctx, function));
                            hasError = true;
                        } else {
                            final long numberRequiredParams = params.stream()
                                    .filter(p -> !p.optional())
                                    .count();
                            if (numberRequiredParams > positionalArgs.size()) {
                                parseContext.addError(new WrongNumberOfArgs(ctx, function, positionalArgs.size()));
                                hasError = true;
                            }
                        }
                    }

                    if (!hasError) {
                        // only try to assign params if we didn't encounter a problem with position optional params above
                        int i = 0;
                        for (ParameterDescriptor p : params) {
                            if (i >= positionalArgs.size()) {
                                // avoid index out of bounds, we've added an error anyway
                                // the remaining parameters were optional, so we can safely skip them
                                break;
                            }
                            final Expression argExpr = positionalArgs.get(i);
                            argsMap.put(p.name(), argExpr);
                            i++;
                        }
                    }
                } else if(! params.stream().allMatch(ParameterDescriptor::optional)) {
                    // no parameters given but some of them are required
                    parseContext.addError(new WrongNumberOfArgs(ctx, function, 0));
                }
            }

            FunctionExpression expr;
            try {
                expr = new FunctionExpression(
                        ctx.getStart(), new FunctionArgs(functionRegistry.resolveOrError(name), argsMap)
                );
            } catch (PrecomputeFailure precomputeFailure) {
                parseContext.addError(new InvalidFunctionArgument(ctx, function, precomputeFailure));
                expr = new FunctionExpression(ctx.getStart(), new FunctionArgs(Function.ERROR_FUNCTION, argsMap));
            }

            log.trace("FUNC: ctx {} => {}", ctx, expr);
            exprs.put(ctx, expr);
        }

        @Override
        public void exitNamedArgs(RuleLangParser.NamedArgsContext ctx) {
            final Map<String, Expression> argMap = Maps.newHashMap();
            for (RuleLangParser.PropAssignmentContext propAssignmentContext : ctx.propAssignment()) {
                final String argName = unquote(propAssignmentContext.Identifier().getText(), '`');
                final Expression argValue = exprs.get(propAssignmentContext.expression());
                argMap.put(argName, argValue);
            }
            args.put(ctx, argMap);
        }

        @Override
        public void exitPositionalArgs(RuleLangParser.PositionalArgsContext ctx) {
            List<Expression> expressions = Lists.newArrayListWithCapacity(ctx.expression().size());
            expressions.addAll(ctx.expression().stream().map(exprs::get).collect(toList()));
            argsList.put(ctx, expressions);
        }

        @Override
        public void enterNested(RuleLangParser.NestedContext ctx) {
            // nested field access is ok, these are not rule variables
            isIdIsFieldAccess.push(true);
        }

        @Override
        public void exitNested(RuleLangParser.NestedContext ctx) {
            isIdIsFieldAccess.pop(); // reset for error checks
            final Expression object = exprs.get(ctx.fieldSet);
            final Expression field = exprs.get(ctx.field);
            final FieldAccessExpression expr = new FieldAccessExpression(ctx.getStart(), object, field);
            log.trace("FIELDACCESS: ctx {} => {}", ctx, expr);
            exprs.put(ctx, expr);
        }

        @Override
        public void exitNot(RuleLangParser.NotContext ctx) {
            final Expression expression = upgradeBoolFunctionExpression(ctx.expression());
            final NotExpression expr = new NotExpression(ctx.getStart(), expression);
            log.trace("NOT: ctx {} => {}", ctx, expr);
            exprs.put(ctx, expr);
        }

        @Override
        public void exitAnd(RuleLangParser.AndContext ctx) {
            // if the expressions are function calls but boolean valued, upgrade them,
            // we allow testing boolean valued functions without explicit comparison operator
            final Expression left = upgradeBoolFunctionExpression(ctx.left);
            final Expression right = upgradeBoolFunctionExpression(ctx.right);

            final AndExpression expr = new AndExpression(ctx.getStart(), left, right);
            log.trace("AND: ctx {} => {}", ctx, expr);
            exprs.put(ctx, expr);
        }

        private Expression upgradeBoolFunctionExpression(RuleLangParser.ExpressionContext leftExprContext) {
            Expression leftExpr = exprs.get(leftExprContext);
            if (leftExpr instanceof FunctionExpression && leftExpr.getType().equals(Boolean.class)) {
                leftExpr = new BooleanValuedFunctionWrapper(leftExprContext.getStart(), leftExpr);
            }
            return leftExpr;
        }

        @Override
        public void exitOr(RuleLangParser.OrContext ctx) {
            final Expression left = upgradeBoolFunctionExpression(ctx.left);
            final Expression right = upgradeBoolFunctionExpression(ctx.right);
            final OrExpression expr = new OrExpression(ctx.getStart(), left, right);
            log.trace("OR: ctx {} => {}", ctx, expr);
            exprs.put(ctx, expr);
        }

        @Override
        public void exitEquality(RuleLangParser.EqualityContext ctx) {
            final Expression left = exprs.get(ctx.left);
            final Expression right = exprs.get(ctx.right);
            final boolean equals = ctx.equality.getText().equals("==");
            final EqualityExpression expr = new EqualityExpression(ctx.getStart(), left, right, equals);
            log.trace("EQUAL: ctx {} => {}", ctx, expr);
            exprs.put(ctx, expr);
        }

        @Override
        public void exitComparison(RuleLangParser.ComparisonContext ctx) {
            final Expression left = exprs.get(ctx.left);
            final Expression right = exprs.get(ctx.right);
            final String operator = ctx.comparison.getText();
            final ComparisonExpression expr = new ComparisonExpression(ctx.getStart(), left, right, operator);
            log.trace("COMPARE: ctx {} => {}", ctx, expr);
            exprs.put(ctx, expr);
        }

        @Override
        public void exitInteger(RuleLangParser.IntegerContext ctx) {
            // TODO handle different radix and length
            final LongExpression expr = new LongExpression(ctx.getStart(), Long.parseLong(ctx.getText()));
            log.trace("INT: ctx {} => {}", ctx, expr);
            exprs.put(ctx, expr);
        }

        @Override
        public void exitFloat(RuleLangParser.FloatContext ctx) {
            final DoubleExpression expr = new DoubleExpression(ctx.getStart(), Double.parseDouble(ctx.getText()));
            log.trace("FLOAT: ctx {} => {}", ctx, expr);
            exprs.put(ctx, expr);
        }

        @Override
        public void exitChar(RuleLangParser.CharContext ctx) {
            // TODO
            super.exitChar(ctx);
        }

        @Override
        public void exitString(RuleLangParser.StringContext ctx) {
            final String text = unescape(unquote(ctx.getText(), '\"'));
            final StringExpression expr = new StringExpression(ctx.getStart(), text);
            log.trace("STRING: ctx {} => {}", ctx, expr);
            exprs.put(ctx, expr);
        }

        @Override
        public void exitBoolean(RuleLangParser.BooleanContext ctx) {
            final BooleanExpression expr = new BooleanExpression(ctx.getStart(), Boolean.valueOf(ctx.getText()));
            log.trace("BOOL: ctx {} => {}", ctx, expr);
            exprs.put(ctx, expr);
        }

        @Override
        public void exitLiteralPrimary(RuleLangParser.LiteralPrimaryContext ctx) {
            // nothing to do, just propagate the ConstantExpression
            exprs.put(ctx, exprs.get(ctx.literal()));
            parseContext.addInnerNode(ctx);
        }

        @Override
        public void exitArrayLiteralExpr(RuleLangParser.ArrayLiteralExprContext ctx) {
            final List<Expression> elements = ctx.expression().stream().map(exprs::get).collect(toList());
            exprs.put(ctx, new ArrayLiteralExpression(ctx.getStart(), elements));
        }

        @Override
        public void exitMapLiteralExpr(RuleLangParser.MapLiteralExprContext ctx) {
            final HashMap<String, Expression> map = Maps.newHashMap();
            for (RuleLangParser.PropAssignmentContext propAssignmentContext : ctx.propAssignment()) {
                final String key = unquote(propAssignmentContext.Identifier().getText(), '`');
                final Expression value = exprs.get(propAssignmentContext.expression());
                map.put(key, value);
            }
            exprs.put(ctx, new MapLiteralExpression(ctx.getStart(), map));
        }

        @Override
        public void exitParenExpr(RuleLangParser.ParenExprContext ctx) {
            // nothing to do, just propagate
            exprs.put(ctx, exprs.get(ctx.expression()));
            parseContext.addInnerNode(ctx);
        }

        @Override
        public void exitSignedExpression(RuleLangParser.SignedExpressionContext ctx) {
            final Expression right = exprs.get(ctx.expr);
            final boolean isPlus = ctx.sign.getText().equals("+");

            final SignedExpression expr = new SignedExpression(ctx.getStart(), right, isPlus);
            log.trace("SIGN: ctx {} => {}", ctx, expr);
            exprs.put(ctx, expr);
        }

        @Override
        public void exitAddition(RuleLangParser.AdditionContext ctx) {
            final Expression left = exprs.get(ctx.left);
            final Expression right = exprs.get(ctx.right);
            final boolean isPlus = ctx.add.getText().equals("+");

            final AdditionExpression expr = new AdditionExpression(ctx.getStart(), left, right, isPlus);
            log.trace("ADD: ctx {} => {}", ctx, expr);
            exprs.put(ctx, expr);
        }

        @Override
        public void exitMultiplication(RuleLangParser.MultiplicationContext ctx) {
            final Expression left = exprs.get(ctx.left);
            final Expression right = exprs.get(ctx.right);
            final char operator = ctx.mult.getText().charAt(0);

            final MultiplicationExpression expr = new MultiplicationExpression(ctx.getStart(), left, right, operator);
            log.trace("MULT: ctx {} => {}", ctx, expr);
            exprs.put(ctx, expr);
        }

        @Override
        public void enterMessageRef(RuleLangParser.MessageRefContext ctx) {
            // nested field access is ok, these are not rule variables
            isIdIsFieldAccess.push(true);
        }

        @Override
        public void exitMessageRef(RuleLangParser.MessageRefContext ctx) {
            isIdIsFieldAccess.pop(); // reset for error checks
            final Expression fieldExpr = exprs.get(ctx.field);
            final MessageRefExpression expr = new MessageRefExpression(ctx.getStart(), fieldExpr);
            log.trace("$MSG: ctx {} => {}", ctx, expr);
            exprs.put(ctx, expr);
        }

        @Override
        public void exitIdentifier(RuleLangParser.IdentifierContext ctx) {
            // unquote identifier if necessary
            final String identifierName = unquote(ctx.Identifier().getText(), '`');

            if (!isIdIsFieldAccess.peek() && !definedVars.contains(identifierName)) {
                parseContext.addError(new UndeclaredVariable(ctx));
            }
            final Expression expr;
            String type;
            // if the identifier is also a declared variable name prefer the variable
            if (isIdIsFieldAccess.peek() && !definedVars.contains(identifierName)) {
                expr = new FieldRefExpression(ctx.getStart(), identifierName, parseContext.getDefinedVar(identifierName));
                type = "FIELDREF";
            } else {
                expr = new VarRefExpression(ctx.getStart(), identifierName, parseContext.getDefinedVar(identifierName));
                type = "VARREF";
            }
            log.trace("{}: ctx {} => {}", type, ctx, expr);
            exprs.put(ctx, expr);
        }

        @Override
        public void exitFunc(RuleLangParser.FuncContext ctx) {
            // nothing to do, just propagate
            exprs.put(ctx, exprs.get(ctx.functionCall()));
            parseContext.addInnerNode(ctx);
        }

        @Override
        public void exitIndexedAccess(RuleLangParser.IndexedAccessContext ctx) {
            final Expression array = exprs.get(ctx.array);
            final Expression index = exprs.get(ctx.index);

            final IndexedAccessExpression expr = new IndexedAccessExpression(ctx.getStart(), array, index);
            exprs.put(ctx, expr);
            log.trace("IDXACCESS: ctx {} => {}", ctx, expr);
        }
    }

    private static class RuleTypeAnnotator extends RuleLangBaseListener {
        private final ParseContext parseContext;

        public RuleTypeAnnotator(ParseContext parseContext) {
            this.parseContext = parseContext;
        }

        @Override
        public void exitIdentifier(RuleLangParser.IdentifierContext ctx) {
            final Expression expr = parseContext.expressions().get(ctx);
            if (expr instanceof VarRefExpression) {
                final VarRefExpression varRefExpression = (VarRefExpression) expr;
                final String name = varRefExpression.varName();
                final Expression expression = parseContext.getDefinedVar(name);
                if (expression == null) {
                    if (parseContext.isSilent()) {
                        log.debug("Unable to retrieve expression for variable {}, this is a bug", name);
                    } else {
                        log.error("Unable to retrieve expression for variable {}, this is a bug", name);
                    }
                    return;
                }
                log.trace("Inferred type of variable {} to {}", name, expression.getType().getSimpleName());
                varRefExpression.setType(expression.getType());
            }
        }

        @Override
        public void exitAddition(RuleLangParser.AdditionContext ctx) {
            final AdditionExpression expr = (AdditionExpression) parseContext.expressions().get(ctx);
            final Class leftType = expr.left().getType();
            final Class rightType = expr.right().getType();

            if (leftType.equals(rightType)) {
                // propagate left type
                expr.setType(leftType);
            } else if (DateTime.class.equals(leftType) && DateTime.class.equals(rightType)) {
                // fine to subtract two dates from each other, this results in a Duration
                expr.setType(Duration.class);
            } else if (DateTime.class.equals(leftType) && Period.class.equals(rightType) || Period.class.equals(leftType) && DateTime.class.equals(rightType)) {
                expr.setType(DateTime.class);
            } else {
                // this will be detected as an error later
                expr.setType(Void.class);
            }
        }

        @Override
        public void exitMultiplication(RuleLangParser.MultiplicationContext ctx) {
            final MultiplicationExpression expr = (MultiplicationExpression) parseContext.expressions().get(ctx);
            final Class leftType = expr.left().getType();
            final Class rightType = expr.right().getType();

            if (leftType.equals(rightType)) {
                // propagate left type
                expr.setType(leftType);
            } else {
                // this will be detected as an error later
                expr.setType(Void.class);
            }
        }
    }

    private static class RuleTypeChecker extends RuleLangBaseListener {
        private final ParseContext parseContext;
        @SuppressWarnings("JdkObsolete")
        StringBuffer sb = new StringBuffer();

        public RuleTypeChecker(ParseContext parseContext) {
            this.parseContext = parseContext;
        }

        @Override
        public void exitRuleDeclaration(RuleLangParser.RuleDeclarationContext ctx) {
            log.trace("Type tree {}", sb.toString());
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
        public void exitNot(RuleLangParser.NotContext ctx) {
            final Expression expression = parseContext.expressions().get(ctx.expression());
            Class type = expression.getType();
            if (!Boolean.class.isAssignableFrom(type)) {
                parseContext.addError(new IncompatibleType(ctx, Boolean.class, type));
            }
        }

        @Override
        public void exitComparison(RuleLangParser.ComparisonContext ctx) {
            checkBinaryExpression(ctx);
        }

        @Override
        public void exitAddition(RuleLangParser.AdditionContext ctx) {
            final AdditionExpression addExpression = (AdditionExpression) parseContext.expressions().get(ctx);
            final Class leftType = addExpression.left().getType();
            final Class rightType = addExpression.right().getType();

            // special case for DateTime/Period, which are all compatible
            final boolean leftDate = DateTime.class.equals(leftType);
            final boolean rightDate = DateTime.class.equals(rightType);
            final boolean leftPeriod = Period.class.equals(leftType);
            final boolean rightPeriod = Period.class.equals(rightType);
            final boolean leftString = String.class.equals(leftType);
            final boolean rightString = String.class.equals(rightType);

            if (leftDate && rightDate) {
                if (addExpression.isPlus()) {
                    parseContext.addError(new InvalidOperation(ctx, addExpression, "Unable to add two dates"));
                }
                return;
            } else if (leftDate && rightPeriod || leftPeriod && rightDate || leftPeriod && rightPeriod) {
                return;
            } else if (leftString && rightString) {
                if (!addExpression.isPlus()) {
                    parseContext.addError(new InvalidOperation(ctx, addExpression, "Unable to subtract two strings"));
                }
                return;
            }
            // otherwise check generic binary expression
            checkBinaryExpression(ctx);
        }

        @Override
        public void exitMultiplication(RuleLangParser.MultiplicationContext ctx) {
            checkBinaryExpression(ctx);
        }

        @Override
        public void exitEquality(RuleLangParser.EqualityContext ctx) {
            // TODO equality allows arbitrary types, in the future optimize by using specialized operators
        }

        private void checkBinaryExpression(RuleLangParser.ExpressionContext ctx) {
            final BinaryExpression binaryExpr = (BinaryExpression) parseContext.expressions().get(ctx);
            final Class leftType = binaryExpr.left().getType();
            final Class rightType = binaryExpr.right().getType();

            if (!leftType.equals(rightType) || Void.class.equals(leftType) || Void.class.equals(rightType)) {
                parseContext.addError(new IncompatibleTypes(ctx, binaryExpr));
            }
        }

        @Override
        public void exitFunctionCall(RuleLangParser.FunctionCallContext ctx) {
            final FunctionExpression expr = (FunctionExpression) parseContext.expressions().get(ctx);
            final FunctionDescriptor<?> descriptor = expr.getFunction().descriptor();
            final FunctionArgs args = expr.getArgs();
            for (ParameterDescriptor p : descriptor.params()) {
                final Expression argExpr = args.expression(p.name());
                if (argExpr != null && !p.type().isAssignableFrom(argExpr.getType())) {
                    parseContext.addError(new IncompatibleArgumentType(ctx, expr, p, argExpr));
                }
            }
        }

        @Override
        public void exitMessageRef(RuleLangParser.MessageRefContext ctx) {
            final MessageRefExpression expr = (MessageRefExpression) parseContext.expressions().get(ctx);
            if (!expr.getFieldExpr().getType().equals(String.class)) {
                parseContext.addError(new IncompatibleType(ctx, String.class, expr.getFieldExpr().getType()));
            }
        }

        @Override
        public void enterEveryRule(ParserRuleContext ctx) {
            final Expression expression = parseContext.expressions().get(ctx);
            if (expression != null && !parseContext.isInnerNode(ctx)) {
                sb.append(" ( ");
                sb.append(expression.getClass().getSimpleName());
                sb.append(":").append(ctx.getClass().getSimpleName()).append(" ");
                sb.append(" <").append(expression.getType().getSimpleName()).append("> ");
                sb.append(ctx.getText());
            }
        }

        @Override
        public void exitEveryRule(ParserRuleContext ctx) {
            final Expression expression = parseContext.expressions().get(ctx);
            if (expression != null && !parseContext.isInnerNode(ctx)) {
                sb.append(" ) ");
            }
        }

        @Override
        public void exitIndexedAccess(RuleLangParser.IndexedAccessContext ctx) {
            final IndexedAccessExpression idxExpr = (IndexedAccessExpression) parseContext.expressions().get(
                    ctx);

            final Class<?> indexableType = idxExpr.getIndexableObject().getType();
            final Class<?> indexType = idxExpr.getIndex().getType();

            final boolean isMap = Map.class.isAssignableFrom(indexableType);
            if (indexableType.isArray()
                    || List.class.isAssignableFrom(indexableType)
                    || Iterable.class.isAssignableFrom(indexableType)
                    || isMap) {
                // then check if the index type is compatible, must be long for array-like and string for map-like types
                if (isMap) {
                    if (!String.class.equals(indexType)) {
                        // add type error
                        parseContext.addError(new IncompatibleIndexType(ctx, String.class, indexType));
                    }
                } else {
                    if (!Long.class.equals(indexType)) {
                        parseContext.addError(new IncompatibleIndexType(ctx, Long.class, indexType));
                    }
                }
            } else {
                // not an indexable type
                parseContext.addError(new NonIndexableType(ctx, indexableType));
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
        /**
         * Should the parser be more silent about its error logging, useful for interactive parsing in the UI.
         */
        private final boolean silent;
        private ParseTreeProperty<List<Expression>> argsLists = new ParseTreeProperty<>();
        private Set<ParseError> errors = Sets.newHashSet();
        // inner nodes in the parse tree will be ignored during type checker printing, they only transport type information
        private Set<RuleContext> innerNodes = new IdentityHashSet<>();
        public List<Statement> statements = Lists.newArrayList();
        public List<Rule> rules = Lists.newArrayList();
        private Map<String, Expression> varDecls = Maps.newHashMap();
        public List<Pipeline> pipelines = Lists.newArrayList();

        public ParseContext(boolean silent) {
            this.silent = silent;
        }

        public ParseTreeProperty<Expression> expressions() {
            return exprs;
        }

        public ParseTreeProperty<Map<String, Expression>> arguments() {
            return args;
        }

        public List<Rule> getRules() {
            return rules;
        }

        public void setRules(List<Rule> rules) {
            this.rules = rules;
        }
        public void addRule(Rule rule) {
            this.rules.add(rule);
        }

        public List<Pipeline> getPipelines() {
            return pipelines;
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

        /**
         * Links the declared var to its expression.
         *
         * @param name var name
         * @param expr expression
         * @return true if successful, false if previously declared
         */
        public boolean defineVar(String name, Expression expr) {
            return varDecls.put(name, expr) == null;
        }

        public Expression getDefinedVar(String name) {
            return varDecls.get(name);
        }

        public ParseTreeProperty<List<Expression>> argumentLists() {
            return argsLists;
        }

        public boolean isSilent() {
            return silent;
        }
    }

    private static class PipelineAstBuilder extends RuleLangBaseListener {
        private final ParseContext parseContext;

        public PipelineAstBuilder(ParseContext parseContext) {
            this.parseContext = parseContext;
        }

        @Override
        public void exitPipelineDeclaration(RuleLangParser.PipelineDeclarationContext ctx) {
            final Pipeline.Builder builder = Pipeline.builder();

            builder.name(unquote(ctx.name.getText(), '"'));
            final ImmutableSortedSet.Builder<Stage> stages = orderedBy(comparingInt(Stage::stage));

            for (RuleLangParser.StageDeclarationContext stage : ctx.stageDeclaration()) {
                final Stage.Builder stageBuilder = Stage.builder();

                final Token stageToken = stage.stage;
                if (stageToken == null) {
                    parseContext.addError(new SyntaxError(null, 0, 0, "", null));
                    return;
                }
                final int stageNumber = Integer.parseInt(stageToken.getText());
                stageBuilder.stage(stageNumber);

                final Token modifier = stage.modifier;
                if (modifier == null) {
                    parseContext.addError(new SyntaxError(null, stageToken.getLine(), stageToken.getCharPositionInLine(), "", null));
                    return;
                }
                final boolean isAllModifier = modifier.getText().equalsIgnoreCase("all");
                stageBuilder.matchAll(isAllModifier);

                final List<String> ruleRefs = stage.ruleRef().stream()
                        .map(ruleRefContext -> {
                            final Token name = ruleRefContext.name;
                            if (name == null) {
                                final Token symbol = ruleRefContext.Rule().getSymbol();
                                parseContext.addError(new SyntaxError(symbol, symbol.getLine(), symbol.getCharPositionInLine(), "invalid rule reference", null));
                                return "__illegal_reference";
                            }
                            return unquote(name.getText(), '"');
                        })
                        .collect(toList());
                stageBuilder.ruleReferences(ruleRefs);

                stages.add(stageBuilder.build());
            }

            builder.stages(stages.build());
            parseContext.pipelines.add(builder.build());
        }

        @Override
        public void exitInteger(RuleLangParser.IntegerContext ctx) {
            // TODO handle different radix and length
            final LongExpression expr = new LongExpression(ctx.getStart(), Long.parseLong(ctx.getText()));
            log.trace("INT: ctx {} => {}", ctx, expr);
            parseContext.exprs.put(ctx, expr);
        }

        @Override
        public void exitString(RuleLangParser.StringContext ctx) {
            final String text = unescape(unquote(ctx.getText(), '\"'));
            final StringExpression expr = new StringExpression(ctx.getStart(), text);
            log.trace("STRING: ctx {} => {}", ctx, expr);
            parseContext.exprs.put(ctx, expr);
        }

    }
}

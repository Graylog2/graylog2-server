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
package org.graylog.plugins.pipelineprocessor.codegen;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Primitives;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.RuleAstBaseListener;
import org.graylog.plugins.pipelineprocessor.ast.RuleAstWalker;
import org.graylog.plugins.pipelineprocessor.ast.expressions.AdditionExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.AndExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.ArrayLiteralExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.BooleanValuedFunctionWrapper;
import org.graylog.plugins.pipelineprocessor.ast.expressions.ComparisonExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.ConstantExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.DoubleExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.EqualityExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.Expression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.FieldAccessExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.FieldRefExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.FunctionExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.IndexedAccessExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.LongExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.MapLiteralExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.MessageRefExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.MultiplicationExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.NotExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.OrExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.SignedExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.StringExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.VarRefExpression;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.statements.VarAssignStatement;
import org.graylog.plugins.pipelineprocessor.codegen.compiler.JavaCompiler;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.FeatureDescriptor;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.lang.model.element.Modifier;

import static com.google.common.base.MoreObjects.firstNonNull;

public class CodeGenerator {
    private static final Logger log = LoggerFactory.getLogger(CodeGenerator.class);
    private final Provider<JavaCompiler> compilerProvider;

    @Inject
    public CodeGenerator(Provider<JavaCompiler> compilerProvider) {
        this.compilerProvider = compilerProvider;
    }

    public static String sourceCodeForRule(Rule rule) {
        final JavaPoetListener javaPoetListener = new JavaPoetListener();
        new RuleAstWalker().walk(javaPoetListener, rule);
        return javaPoetListener.getSource();
    }

    @SuppressWarnings("unchecked")
    public Class<? extends GeneratedRule> generateCompiledRule(Rule rule, PipelineClassloader ruleClassloader) {
        if (rule.id() == null) {
            throw new IllegalArgumentException("Rules must have an id to generate code for them");
        }
        final String sourceCode = sourceCodeForRule(rule);
        try {
            if (log.isTraceEnabled()) {
                log.trace("Sourcecode:\n{}", sourceCode);
            }
            return (Class<GeneratedRule>) compilerProvider.get().loadFromString(ruleClassloader, "org.graylog.plugins.pipelineprocessor.$dynamic.rules.rule$" + rule.id() , sourceCode);
        } catch (ClassNotFoundException e) {
            log.error("Unable to compile code\n{}", sourceCode);
            return null;
        }

    }

    private static class JavaPoetListener extends RuleAstBaseListener {
        public static final Set<Class<?>> OPERATOR_SAFE_TYPES = Sets.union(Primitives.allPrimitiveTypes(), Primitives.allWrapperTypes());
        private long counter = 0;
        private IdentityHashMap<Expression, CodeBlock> codeSnippet = new IdentityHashMap<>();

        private TypeSpec.Builder classFile;
        private JavaFile generatedFile;
        private MethodSpec.Builder when;
        private MethodSpec.Builder then;

        // points to either when or then
        private MethodSpec.Builder currentMethod;

        /**
         * the unique set of function references in this rule
         */
        private Set<FieldSpec> functionMembers = Sets.newHashSet();
        private Set<FieldSpec> hoistedExpressionMembers = Sets.newHashSet();
        private Set<TypeSpec> functionArgsHolderTypes = Sets.newHashSet();
        private MethodSpec.Builder constructorBuilder;
        private CodeBlock.Builder lateConstructorBlock;
        private CodeBlock.Builder hoistedConstantExpressions;
        private Set<CodeBlock> functionReferences = Sets.newHashSet();

        public String getSource() {
            return generatedFile.toString();
        }

        @Override
        public void enterRule(Rule rule) {
            // generates a new ephemeral unique class name for each generated rule. Only valid for the runtime of the jvm
            classFile = TypeSpec.classBuilder("rule$" + rule.id())
                    .addSuperinterface(GeneratedRule.class)
                    .addModifiers(Modifier.FINAL, Modifier.PUBLIC)
                    .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                            .addMember("value", "$S", "unchecked")
                            .build()
                    )
                    .addMethod(MethodSpec.methodBuilder("name")
                            .returns(String.class)
                            .addAnnotation(Override.class)
                            .addModifiers(Modifier.PUBLIC)
                            .addStatement("return $S", rule.name())
                            .build()
                    );
            constructorBuilder = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(FunctionRegistry.class, "functionRegistry");
            lateConstructorBlock = CodeBlock.builder();
            hoistedConstantExpressions = CodeBlock.builder();
        }

        @Override
        public void exitRule(Rule rule) {
            // create fields for each used function
            classFile.addFields(functionMembers);
            // create fields for hoisted constant expressions
            classFile.addFields(hoistedExpressionMembers);
            // TODO these can be shared and should potentially created by an AnnotationProcessor for each defined function instead of every rule
            classFile.addTypes(functionArgsHolderTypes);

            // resolve functions (but only do so once for each function)
            constructorBuilder.addStatement("// resolve used functions");
            functionReferences.forEach(block -> constructorBuilder.addStatement("$L", block));
            // add initializers for fields that depend on the functions being set
            constructorBuilder.addStatement("// function parameters");
            constructorBuilder.addCode(lateConstructorBlock.build());
            // all the expressions/statements that are constant at compile time
            constructorBuilder.addStatement("// constant expressions");
            constructorBuilder.addCode(hoistedConstantExpressions.build());


            classFile.addMethod(constructorBuilder.build());

            generatedFile = JavaFile.builder("org.graylog.plugins.pipelineprocessor.$dynamic.rules", classFile.build())
                    .build();
        }

        @Override
        public void enterWhen(Rule rule) {
            when = MethodSpec.methodBuilder("when")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(boolean.class)
                    .addParameter(EvaluationContext.class, "context", Modifier.FINAL);
            currentMethod = when;
        }

        @Override
        public void exitWhen(Rule rule) {
            final CodeBlock result = codeSnippet.getOrDefault(rule.when(), CodeBlock.of("$$when"));
            when.addStatement("return $L", result);

            classFile.addMethod(when.build());
            // sanity to catch errors earlier
            currentMethod = null;
        }

        @Override
        public void enterThen(Rule rule) {
            then = MethodSpec.methodBuilder("then")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .addParameter(EvaluationContext.class, "context", Modifier.FINAL);
            currentMethod = then;
        }

        @Override
        public void exitThen(Rule rule) {
            classFile.addMethod(then.build());
            // sanity to catch errors earlier
            currentMethod = null;
        }

        @Override
        public void exitAnd(AndExpression expr) {
            final CodeBlock left = codeSnippet.get(expr.left());
            final CodeBlock right = codeSnippet.get(expr.right());

            codeSnippet.put(expr, CodeBlock.of("($L && $L)", blockOrMissing(left, expr.left()), blockOrMissing(right, expr.right())));
        }

        @Override
        public void exitOr(OrExpression expr) {
            final CodeBlock left = codeSnippet.get(expr.left());
            final CodeBlock right = codeSnippet.get(expr.right());

            codeSnippet.put(expr, CodeBlock.of("($L || $L)", blockOrMissing(left, expr.left()), blockOrMissing(right, expr.right())));
        }

        @Override
        public void exitNot(NotExpression expr) {
            final CodeBlock right = codeSnippet.get(expr.right());

            codeSnippet.put(expr, CodeBlock.of("!$L", blockOrMissing(right, expr.right())));
        }

        @Override
        public void exitFieldRef(FieldRefExpression expr) {
            codeSnippet.put(expr, CodeBlock.of("$L", expr.fieldName()));
        }

        @Override
        public void exitFieldAccess(FieldAccessExpression expr) {
            final CodeBlock object = codeSnippet.get(expr.object());
            final CodeBlock field = codeSnippet.get(expr.field());

            final Object objectRef = blockOrMissing(object, expr.object());

            final Expression o = expr.object();
            final PropertyDescriptor[] propertyDescriptors = PropertyUtils.getPropertyDescriptors(o.getType());
            final ImmutableMap<String, PropertyDescriptor> propertyByName = Maps.uniqueIndex(Iterators.forArray(propertyDescriptors), FeatureDescriptor::getName);

            final String fieldName = field.toString();
            final CodeBlock block;
            if (propertyByName.containsKey(fieldName)) {
                // we have the property, resolve the read method name for it
                final PropertyDescriptor descriptor = propertyByName.get(fieldName);
                final String methodName = descriptor.getReadMethod().getName();
                block = CodeBlock.of("$L.$L()", objectRef, methodName);
            } else if (o instanceof Map) {
                // there wasn't any property, but the object is a Map, translate into .get() call
                block = CodeBlock.of("$L.get($S)", objectRef, field);
            } else {
                // this is basically an error, because we expected either a property to match or a map lookup.
                log.warn("Unable to determine field accessor for property {}", field);
                block = CodeBlock.of("null");
            }

            codeSnippet.put(expr, block);
        }

        @Override
        public void exitFunctionCall(FunctionExpression expr) {
            final String functionValueVarName = subExpressionName();
            final FunctionDescriptor<?> function = expr.getFunction().descriptor();

            final String mangledFunctionName = functionReference(function);
            final String mangledFuncArgsHolder = functionArgsHolder(function);

            // evaluate all the parameters (the parser made sure all required fields are given)
            final FunctionArgs args = expr.getArgs();
            final CodeBlock.Builder argAssignment = CodeBlock.builder();
            args.getArgs().forEach((name, argExpr) -> {
                final Object varRef = blockOrMissing(codeSnippet.get(argExpr), argExpr);
                // hoist constant argument evaluation
                CodeBlock.Builder target = argExpr.isConstant() ? hoistedConstantExpressions : argAssignment;
                target.addStatement("$L.setAndTransform$$$L($L)",
                        mangledFuncArgsHolder,
                        name,
                        varRef);
            });
            currentMethod.addCode(argAssignment.build());

            // actually invoke the function
            CodeBlock functionInvocation = CodeBlock.of("$L.evaluate($L, context)", mangledFunctionName, mangledFuncArgsHolder);

            // don't create intermediate values for void functions (set_fields et al)
            if (Void.class.equals(function.returnType())) {
                currentMethod.addStatement("$L", functionInvocation);
            } else {
                currentMethod.addStatement("$T $L = $L", ClassName.get(function.returnType()), functionValueVarName, functionInvocation);
            }
            // create a field/initializer block for the function reference
            functionMembers.add(
                    FieldSpec.builder(expr.getFunction().getClass(), mangledFunctionName, Modifier.PRIVATE, Modifier.FINAL)
                            .build());
            functionReferences.add(CodeBlock.of("$L = ($T) functionRegistry.resolve($S)",
                    mangledFunctionName,
                    expr.getFunction().getClass(),
                    function.name()));
            codeSnippet.put(expr, CodeBlock.of("$L", functionValueVarName));
        }

        @Nonnull
        private String functionArgsHolder(FunctionDescriptor<?> function) {
            // create the argument holder for the function invocation (and create the holder class if it doesn't exist yet)
            final String functionArgsClassname = functionArgsHolderClass(function);
            final String functionArgsMember = functionReference(function) + "$" + subExpressionName();
            classFile.addField(FieldSpec.builder(
                    ClassName.bestGuess(functionArgsClassname), functionArgsMember, Modifier.PRIVATE)
                    .build());

            lateConstructorBlock.addStatement("$L = new $L()", functionArgsMember, functionArgsClassname);

            return functionArgsMember;
        }

        @Nonnull
        private String functionArgsHolderClass(FunctionDescriptor<?> functionDescriptor) {
            final String funcReferenceName = functionReference(functionDescriptor);

            final String functionArgsClassname = StringUtils.capitalize(funcReferenceName + "$args");
            final TypeSpec.Builder directFunctionArgs = TypeSpec.classBuilder(functionArgsClassname)
                    .addModifiers(Modifier.PRIVATE)
                    .superclass(ClassName.get(FunctionArgs.class));
            final MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                    .addStatement("super($L, $T.emptyMap())", funcReferenceName, ClassName.get(Collections.class));

            final CodeBlock.Builder parameterValues = CodeBlock.builder();
            functionDescriptor.params().forEach(pd -> {
                directFunctionArgs.addMethod(MethodSpec.methodBuilder("setAndTransform$" + pd.name())
                        .returns(TypeName.VOID)
                        .addParameter(ClassName.get(pd.type()), "arg$" + pd.name())
                        .addStatement("transformed$$$L = transformer$$$L.apply(arg$$$L)",
                                pd.name(), pd.name(), pd.name())
                        .build()
                );
                final ParameterizedTypeName transformerType = ParameterizedTypeName.get(Function.class, pd.type(), pd.transformedType());
                directFunctionArgs.addField(
                        transformerType,
                        "transformer$" + pd.name(),
                        Modifier.PRIVATE, Modifier.FINAL);
                directFunctionArgs.addField(ClassName.get(pd.transformedType()), "transformed$" + pd.name());
                constructorBuilder.addStatement("transformer$$$L = ($T) $L.descriptor().param($S).transform()",
                        pd.name(),
                        transformerType,
                        funcReferenceName,
                        pd.name());

                parameterValues.add(CodeBlock.builder()
                        .beginControlFlow("case $S:", pd.name())
                        .addStatement("return transformed$$$L", pd.name())
                        .endControlFlow().build());
            });

            directFunctionArgs.addMethod(MethodSpec.methodBuilder("getPreComputedValue")
                    .returns(TypeName.OBJECT)
                    .addParameter(String.class, "name")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .addCode(CodeBlock.builder()
                            .beginControlFlow("switch (name)")
                            .add(parameterValues.build())
                            .endControlFlow()
                            .addStatement("return null")
                            .build())
                    .build());

            directFunctionArgs.addMethod(constructorBuilder.build());

            final TypeSpec holder = directFunctionArgs.build();
            if (!functionArgsHolderTypes.contains(holder)) {
                functionArgsHolderTypes.add(holder);
            }
            return functionArgsClassname;
        }

        @Nonnull
        private String functionReference(FunctionDescriptor<?> function) {
            return "func$" + function.name();
        }

        @Override
        public void exitEquality(EqualityExpression expr) {
            final String intermediateName = subExpressionName();

            // equality is one of the points we generate intermediate values at
            final CodeBlock leftBlock = codeSnippet.get(expr.left());
            final CodeBlock rightBlock = codeSnippet.get(expr.right());

            final Class leftType = expr.left().getType();
            final Class rightType = expr.right().getType();
            boolean useOperator = false;
            if (OPERATOR_SAFE_TYPES.contains(leftType) && OPERATOR_SAFE_TYPES.contains(rightType)) {
                useOperator = true;
            }
            String statement = "boolean $L = ";
            final boolean checkEquality = expr.isCheckEquality();
            if (useOperator) {
                statement += "$L " + (checkEquality ? "==" : "!=") + " $L";
                currentMethod.addStatement(statement,
                        intermediateName,
                        blockOrMissing(leftBlock, expr.left()),
                        blockOrMissing(rightBlock, expr.right()));
            } else {
                // Dates
                if (DateTime.class.equals(leftType)) {
                    if (DateTime.class.equals(rightType)) {
                        codeSnippet.putIfAbsent(expr, CodeBlock.of("$L.isEqual($L)", leftBlock, rightBlock));
                        return;
                    }
                } else if (Period.class.equals(leftType)) {
                    if (Period.class.equals(rightType)) {
                        codeSnippet.putIfAbsent(expr,
                                CodeBlock.of("$L.toDuration().equals($L.toDuration())", leftBlock, rightBlock));
                        return;
                    }
                }

                statement += (checkEquality ? "" : "!") + "$T.equals($L, $L)";
                currentMethod.addStatement(statement,
                        intermediateName,
                        ClassName.get(Objects.class),
                        blockOrMissing(leftBlock, expr.left()),
                        blockOrMissing(rightBlock, expr.right()));
            }

            codeSnippet.put(expr, CodeBlock.of("$L", intermediateName));
        }

        @Override
        public void exitComparison(ComparisonExpression expr) {
            final CodeBlock left = codeSnippet.get(expr.left());
            final CodeBlock right = codeSnippet.get(expr.right());

            final Class leftType = expr.left().getType();
            final Class rightType = expr.right().getType();

            if (DateTime.class.equals(leftType)) {
                if (DateTime.class.equals(rightType)) {
                    CodeBlock block;
                    switch (expr.getOperator()) {
                        case ">":
                            block = CodeBlock.of("$L.isAfter($L)", left, right);
                            break;
                        case ">=":
                            block = CodeBlock.of("!$L.isBefore($L)", left, right);
                            break;
                        case "<":
                            block = CodeBlock.of("$L.isBefore($L)", left, right);
                            break;
                        case "<=":
                            block = CodeBlock.of("!$L.isAfter($L)", left, right);
                            break;
                        default:
                            block = null;
                    }
                    if (block != null) {
                        codeSnippet.putIfAbsent(expr, block);
                        return;
                    }
                }
            } else if (Period.class.equals(leftType)) {
                if (Period.class.equals(rightType)) {
                    codeSnippet.putIfAbsent(expr,
                            CodeBlock.of("($L.toDuration().getMillis() " + expr.getOperator() + " $L.toDuration().getMillis())",
                                    blockOrMissing(left, expr.left()),
                                    blockOrMissing(right, expr.right())));
                    return;

                }
            }

            codeSnippet.putIfAbsent(expr, CodeBlock.of("($L " + expr.getOperator() + " $L)",
                    blockOrMissing(left, expr.left()),
                    blockOrMissing(right, expr.right())));
        }

        @Override
        public void exitBooleanFuncWrapper(BooleanValuedFunctionWrapper expr) {
            final CodeBlock embeddedExpr = codeSnippet.get(expr.expression());

            // simply forward the other expression's code
            codeSnippet.put(expr, CodeBlock.of("$L", blockOrMissing(embeddedExpr, expr.expression())));
        }

        @Override
        public void exitConstant(ConstantExpression expr) {
            codeSnippet.putIfAbsent(expr, CodeBlock.of("$L", expr.evaluateUnsafe()));
        }

        @Override
        public void exitString(StringExpression expr) {
            // this overrides what exitConstant would do for strings…
            codeSnippet.putIfAbsent(expr, CodeBlock.of("$S", expr.evaluateUnsafe()));
        }

        @Override
        public void exitLong(LongExpression expr) {
            // long needs a suffix
            codeSnippet.putIfAbsent(expr, CodeBlock.of("$LL", expr.evaluateUnsafe()));
        }

        @Override
        public void exitDouble(DoubleExpression expr) {
            // double should have a suffix
            codeSnippet.putIfAbsent(expr, CodeBlock.of("$Ld", expr.evaluateUnsafe()));
        }

        @Override
        public void exitMessageRef(MessageRefExpression expr) {
            final Object field = blockOrMissing(codeSnippet.get(expr.getFieldExpr()), expr.getFieldExpr());

            codeSnippet.putIfAbsent(expr, CodeBlock.of("context.currentMessage().getField($S)", field));
        }

        @Override
        public void exitVariableAssignStatement(VarAssignStatement assign) {
            final Object value = blockOrMissing(codeSnippet.get(assign.getValueExpression()), assign.getValueExpression());
            final Class type = assign.getValueExpression().getType();

            // always hoist declaration
            hoistedExpressionMembers.add(FieldSpec.builder(type, "var$" + assign.getName(), Modifier.PRIVATE).build());
            if (assign.getValueExpression().isConstant()) {
                // also hoist the assignment
                hoistedConstantExpressions.addStatement("var$$$L = $L", assign.getName(), value);
            } else {
                currentMethod.addStatement("var$$$L = $L", assign.getName(), value);
            }
        }

        @Override
        public void exitVariableReference(VarRefExpression expr) {
            codeSnippet.putIfAbsent(expr, CodeBlock.of("var$$$L", expr.varName()));
        }

        @Override
        public void exitMapLiteral(MapLiteralExpression expr) {
            // we need an intermediate value for creating the map
            final String mapName = "mapLiteral$" + subExpressionName();
            final boolean constantMap = expr.isConstant();
            if (constantMap) {
                // we can hoist both the declaration, as well as the definition of the map
                hoistedExpressionMembers.add(FieldSpec.builder(Map.class, mapName, Modifier.PRIVATE, Modifier.FINAL).build());
                hoistedConstantExpressions.addStatement("$L = $T.newHashMap()", mapName, Maps.class);
            } else {
                currentMethod.addStatement("$T $L = $T.newHashMap()", Map.class, mapName, Maps.class);
            }

            expr.entries().forEach(entry -> {
                final String code = "$L.put($S, $L)";
                final Object[] args = {mapName, entry.getKey(), blockOrMissing(codeSnippet.get(entry.getValue()), entry.getValue())};
                // TODO convert to code block
                // hoist only completely constant maps (otherwise we would need to regenerate the non-constant ones per evaluation)
                if (constantMap) {
                    hoistedConstantExpressions.addStatement(code, args);
                } else {
                    currentMethod.addStatement(code, args);
                }
            });
            // add the reference to the map we created
            codeSnippet.putIfAbsent(expr, CodeBlock.of("$L", mapName));
        }

        @Override
        public void exitArrayLiteral(ArrayLiteralExpression expr) {
            final String listName = "arrayLiteral$" + subExpressionName();
            final boolean constantList = expr.isConstant();

            final ImmutableList.Builder<Object> elementsBuilder = ImmutableList.builder();
            expr.children().forEach(expression -> elementsBuilder.add(blockOrMissing(codeSnippet.get(expression), expression)));
            final ImmutableList<Object> elements = elementsBuilder.build();

            // if possible hoist decl to constructor
            if (constantList) {
                hoistedExpressionMembers.add(FieldSpec.builder(List.class, listName, Modifier.PRIVATE, Modifier.FINAL).build());
            }
            final String assignmentFormat = "$L = $T.newArrayList("
                    + Stream.generate(() -> "$L").limit(elements.size()).reduce(Joiner.on(", ")::join).orElseGet(() -> "$")
                    + ")";
            // sigh java varargs
            List<Object> args = Lists.newArrayList(ArrayList.class, listName, Lists.class);
            args.addAll(elements);
            // if constant, initialize completely in constructor
            if (constantList) {
                hoistedConstantExpressions.addStatement(assignmentFormat, args.subList(1, args.size()).toArray());
            } else {
                currentMethod.addStatement("$T " + assignmentFormat, args.toArray());
            }
            codeSnippet.putIfAbsent(expr, CodeBlock.of("$L", listName));
        }

        @Override
        public void exitAddition(AdditionExpression expr) {
            final Object leftBlock = blockOrMissing(codeSnippet.get(expr.left()), expr.left());
            final Object rightBlock = blockOrMissing(codeSnippet.get(expr.right()), expr.right());

            Class leftType = expr.left().getType();
            Class rightType = expr.right().getType();

            if (DateTime.class.equals(leftType)) {
                if (DateTime.class.equals(rightType)) {
                    // calculate duration between two dates (adding two dates is invalid)
                    if (expr.isPlus()) {
                        throw new IllegalStateException("Cannot add two dates, this is a parser bug");
                    }
                    codeSnippet.putIfAbsent(expr, CodeBlock.of(
                            "new $T($L, $L)", Duration.class, leftBlock, rightBlock));
                } else if (Period.class.equals(rightType)) {
                    // new datetime
                    codeSnippet.putIfAbsent(expr,
                            CodeBlock.of("$L." + (expr.isPlus() ? "plus" : "minus") + "($L)", leftBlock, rightBlock));
                }
                return;
            } else if (Period.class.equals(leftType)) {
                if (DateTime.class.equals(rightType)) {
                    // invert the arguments, adding the period to the date, yielding a new DateTime
                    codeSnippet.putIfAbsent(expr,
                            CodeBlock.of("$L." + (expr.isPlus() ? "plus" : "minus") + "($L)", rightBlock, leftBlock));
                } else if (Period.class.equals(rightType)) {
                    // adding two periods yields a new period
                    codeSnippet.putIfAbsent(expr,
                            CodeBlock.of("$L." + (expr.isPlus() ? "plus" : "minus") + "($L)", leftBlock, rightBlock));
                }
                return;
            }

            codeSnippet.putIfAbsent(expr,
                    CodeBlock.of("$L " + (expr.isPlus() ? "+" : "-") + " $L", leftBlock, rightBlock));
        }

        @Override
        public void exitMultiplication(MultiplicationExpression expr) {
            final Object leftBlock = blockOrMissing(codeSnippet.get(expr.left()), expr.left());
            final Object rightBlock = blockOrMissing(codeSnippet.get(expr.right()), expr.right());

            codeSnippet.putIfAbsent(expr,
                    CodeBlock.of("$L " + expr.getOperator() + " $L", leftBlock, rightBlock));
        }

        @Override
        public void exitSigned(SignedExpression expr) {
            final Object rightBlock = blockOrMissing(codeSnippet.get(expr.right()), expr.right());
            codeSnippet.putIfAbsent(expr, CodeBlock.of((expr.isPlus() ? "+" : "-") + "$L", rightBlock));
        }

        @Override
        public void exitIndexedAccess(IndexedAccessExpression expr) {
            final Expression indexableObject = expr.getIndexableObject();
            final Expression index = expr.getIndex();

            final Object objectBlock = blockOrMissing(codeSnippet.get(indexableObject), indexableObject);
            final Object indexBlock = blockOrMissing(codeSnippet.get(index), index);

            final Class indexType = index.getType();
            final Class indexableObjectType = indexableObject.getType();
            CodeBlock block;
            if (Long.class.equals(indexType)) {
                // array indexing
                if (indexableObjectType.isArray()) {
                    block = CodeBlock.of("Arrays.get($L, $L)", objectBlock, indexBlock);
                } else if (List.class.isAssignableFrom(indexableObjectType)) {
                    block = CodeBlock.of("$L.get($T.saturatedCast($L))", objectBlock, ClassName.get(Ints.class), indexBlock);
                } else if (Iterable.class.isAssignableFrom(indexableObjectType)) {
                    block = CodeBlock.of("$T.get($L, $L)", ClassName.get(Iterables.class), objectBlock, indexBlock);
                } else {
                    log.error("Unhandled indexable object type: {}", indexableObject);
                    block = null;
                }
            } else if (String.class.equals(indexType) && Map.class.isAssignableFrom(indexableObjectType)) {
                // map indexing
                block = CodeBlock.of("$L.get($L)", objectBlock, indexBlock);
            } else {
                // illegal
                log.error("Invalid index type: {}", index);
                block = null;
            }
            codeSnippet.putIfAbsent(expr, block);
        }

        @Nonnull
        private String subExpressionName() {
            return "im$" + counter++;
        }

        private Object blockOrMissing(Object block, Expression fallBackExpression) {
            if (block == null) {
                log.warn("Missing code snippet for {}: ", fallBackExpression.nodeType(), fallBackExpression);
            }
            return firstNonNull(block, fallBackExpression);
        }

    }

}

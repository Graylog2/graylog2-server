package org.graylog.plugins.pipelineprocessor.codegen;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import org.apache.commons.beanutils.PropertyUtils;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.RuleAstBaseListener;
import org.graylog.plugins.pipelineprocessor.ast.RuleAstWalker;
import org.graylog.plugins.pipelineprocessor.ast.expressions.AndExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.BooleanValuedFunctionWrapper;
import org.graylog.plugins.pipelineprocessor.ast.expressions.ConstantExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.EqualityExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.Expression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.FieldAccessExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.FieldRefExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.FunctionExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.NotExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.OrExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.StringExpression;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.FeatureDescriptor;
import java.beans.PropertyDescriptor;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nonnull;
import javax.lang.model.element.Modifier;

import static com.google.common.base.MoreObjects.firstNonNull;

public class CodeGenerator {
    private static final Logger log = LoggerFactory.getLogger(CodeGenerator.class);
    private static AtomicLong ruleNameCounter = new AtomicLong();

    public static String codeForRule(Rule rule) {
        final JavaPoetListener javaPoetListener = new JavaPoetListener();
        new RuleAstWalker().walk(javaPoetListener, rule);
        return javaPoetListener.getSource();
    }

    private static class JavaPoetListener extends RuleAstBaseListener {
        private long counter = 0;
        private IdentityHashMap<Expression, CodeBlock> codeSnippet = new IdentityHashMap<>();

        private TypeSpec.Builder classFile;
        private JavaFile generatedFile;
        private MethodSpec.Builder when;
        private MethodSpec.Builder then;

        public String getSource() {
            return generatedFile.toString();
        }

        @Override
        public void enterRule(Rule rule) {
            // generates a new ephemeral unique class name for each generated rule. Only valid for the runtime of the jvm
            classFile = TypeSpec.classBuilder("rule$" + ruleNameCounter.incrementAndGet())
                    .addSuperinterface(GeneratedRule.class)
                    .addModifiers(Modifier.FINAL, Modifier.PUBLIC)
                    .addMethod(MethodSpec.methodBuilder("name")
                            .returns(String.class)
                            .addAnnotation(Override.class)
                            .addStatement("return $S", rule.name())
                            .build()
                    );

        }

        @Override
        public void exitRule(Rule rule) {
            generatedFile = JavaFile.builder("org.graylog.plugins.pipelineprocessor.$dynamic.rules", classFile.build())
                    .build();
        }

        @Override
        public void enterWhen(Rule rule) {
            when = MethodSpec.methodBuilder("when")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(boolean.class)
                    .addParameter(EvaluationContext.class, "context", Modifier.FINAL)
                    .addStatement("boolean $$when = false");
        }

        @Override
        public void exitWhen(Rule rule) {
            final CodeBlock result = codeSnippet.getOrDefault(rule.when(), CodeBlock.of("$$when"));
            when.addStatement("return $L", result);

            classFile.addMethod(when.build());
        }

        @Override
        public void enterThen(Rule rule) {
            then = MethodSpec.methodBuilder("then")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .addParameter(EvaluationContext.class, "context", Modifier.FINAL);
        }

        @Override
        public void exitThen(Rule rule) {
            classFile.addMethod(then.build());
        }

        @Override
        public void exitAnd(AndExpression expr) {
            final CodeBlock left = codeSnippet.get(expr.left());
            final CodeBlock right = codeSnippet.get(expr.right());

            codeSnippet.put(expr, CodeBlock.of("$L && $L", blockOrMissing(left, expr.left()), blockOrMissing(right, expr.right())));
        }

        @Override
        public void exitOr(OrExpression expr) {
            final CodeBlock left = codeSnippet.get(expr.left());
            final CodeBlock right = codeSnippet.get(expr.right());

            codeSnippet.put(expr, CodeBlock.of("$L || $L", blockOrMissing(left, expr.left()), blockOrMissing(right, expr.right())));
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
            final String intermediateName = subExpressionName();
            final FunctionDescriptor<?> function = expr.getFunction().descriptor();

            final String funcReference = "func$" + function.name();
            CodeBlock functionInvocation = CodeBlock.of("$L($L)", funcReference, null);

            when.addStatement("$T $L = $L", ClassName.get(function.returnType()), intermediateName, functionInvocation);
            codeSnippet.put(expr, CodeBlock.of("$L", intermediateName));
        }

        @Override
        public void exitEquality(EqualityExpression expr) {
            final String intermediateName = subExpressionName();

            // equality is one of the points we generate intermediate values at
            final CodeBlock leftBlock = codeSnippet.get(expr.left());
            final CodeBlock rightBlock = codeSnippet.get(expr.right());
            when.addStatement("boolean $L = $L == $L", intermediateName, blockOrMissing(leftBlock, expr.left()), blockOrMissing(rightBlock, expr.right()));
            codeSnippet.put(expr, CodeBlock.of("$L", intermediateName));
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

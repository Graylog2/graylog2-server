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
package org.graylog.plugins.pipelineprocessor.compiler;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.swrve.ratelimitedlogger.RateLimitedLog;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.Stage;
import org.graylog.plugins.pipelineprocessor.ast.expressions.LogicalExpression;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.codahale.metrics.MetricRegistry.name;
import static org.graylog2.plugin.utilities.ratelimitedlog.RateLimitedLogFactory.createDefaultRateLimitedLog;

/** Compiles pipeline rules to hidden classes; rules that can't be compiled keep being interpreted. */
@Singleton
public class RuleCompiler {
    private static final RateLimitedLog LOG = createDefaultRateLimitedLog(RuleCompiler.class);

    private final MetricRegistry metricRegistry;
    private final Consumer<byte[]> bytecodeListener;
    private final FastPathResolver fastPaths = new FastPathResolver();
    private final Timer compileTime;
    private final AtomicInteger lastCompiled = new AtomicInteger();
    private final AtomicInteger lastFallback = new AtomicInteger();

    @Inject
    public RuleCompiler(MetricRegistry metricRegistry) {
        this(metricRegistry, bytes -> {});
    }

    /** {@code bytecodeListener} sees every generated class before it is loaded (tests verify it). */
    public RuleCompiler(MetricRegistry metricRegistry, Consumer<byte[]> bytecodeListener) {
        this.metricRegistry = metricRegistry;
        this.bytecodeListener = bytecodeListener;
        this.compileTime = metricRegistry.timer(name(RuleCompiler.class, "compileTime"));
        final Gauge<Integer> compiledRules = lastCompiled::get;
        final Gauge<Integer> fallbackRules = lastFallback::get;
        metricRegistry.gauge(name(RuleCompiler.class, "compiledRules"), () -> compiledRules);
        metricRegistry.gauge(name(RuleCompiler.class, "fallbackRules"), () -> fallbackRules);
    }

    /** Compiles every distinct rule of the given pipelines and attaches the result to each per-stage rule copy. */
    public void compileAll(Collection<Pipeline> pipelines) {
        int compiled = 0;
        int fallback = 0;
        try (Timer.Context ignored = compileTime.time()) {
            // per-stage rule copies share their AST, so compile each AST once
            final IdentityHashMap<LogicalExpression, Optional<CompiledRule>> byCondition = new IdentityHashMap<>();
            for (Pipeline pipeline : pipelines) {
                for (Stage stage : pipeline.stages()) {
                    final List<Rule> rules = stage.getRules();
                    if (rules == null) {
                        continue;
                    }
                    for (Rule rule : rules) {
                        Optional<CompiledRule> result = byCondition.get(rule.when());
                        if (result == null) {
                            result = compile(rule);
                            byCondition.put(rule.when(), result);
                            if (result.isPresent()) {
                                compiled++;
                            } else {
                                fallback++;
                            }
                        }
                        rule.setCompiled(result.orElse(null));
                    }
                }
            }
        }
        lastCompiled.set(compiled);
        lastFallback.set(fallback);
    }

    /** Compiles a rule, or returns empty (and records why) so the rule keeps being interpreted. */
    public Optional<CompiledRule> compile(Rule rule) {
        try {
            return Optional.of(compileOrThrow(rule));
        } catch (CompilationFailedException e) {
            metricRegistry.counter(name(RuleCompiler.class, "fallback", e.reason)).inc();
            LOG.info("Pipeline rule <{}/{}> is interpreted, compilation failed: {}", rule.name(), rule.id(), e.getMessage());
            return Optional.empty();
        }
    }

    /** Like {@link #compile} but throws, for tests that require compilation. */
    public CompiledRule compileOrThrow(Rule rule) {
        final RuleClassEmitter.Emitted emitted;
        try {
            emitted = new RuleClassEmitter(rule, fastPaths).emit();
        } catch (UnsupportedConstructException e) {
            throw new CompilationFailedException(e.reason(), e);
        } catch (RuntimeException e) { // ASM's MethodTooLargeException/ClassTooLargeException, emitter bugs
            throw new CompilationFailedException(e.getClass().getSimpleName(), e);
        }
        bytecodeListener.accept(emitted.bytes());
        final CompiledRule compiled;
        try {
            compiled = CompiledRuleLoader.load(emitted);
        } catch (VirtualMachineError e) {
            throw e;
        } catch (Throwable t) { // VerifyError, LinkageError, ExceptionInInitializerError
            throw new CompilationFailedException(t.getClass().getSimpleName(), t);
        }
        emitted.escapes().forEach(type -> metricRegistry.counter(name(RuleCompiler.class, "escaped", type)).inc());
        emitted.calls().forEach(call -> metricRegistry.counter(name(RuleCompiler.class, "call", call)).inc());
        return compiled;
    }

    public static final class CompilationFailedException extends RuntimeException {
        private final String reason;

        CompilationFailedException(String reason, Throwable cause) {
            // UnsupportedConstructException's message is its reason, don't repeat it
            super(reason.equals(cause.getMessage()) ? reason : reason + ": " + cause.getMessage(), cause);
            this.reason = reason;
        }
    }
}

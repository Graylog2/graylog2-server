/**
 * This file is part of Graylog Pipeline Processor.
 *
 * Graylog Pipeline Processor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Pipeline Processor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Pipeline Processor.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.benchmarks.pipeline;

import com.google.common.collect.Maps;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.joda.time.DateTime;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.google.common.collect.ImmutableList.of;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.object;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;

public class DeclaringClassBenchmark {

    @State(Scope.Benchmark)
    public static class BState {
        public EvaluationContext context;

        private final Map<Class<?>, Class<?>> declaringClassCache;
        Object[] objects;
        Class<?>[] declaringClass;
        int i = 0;
        public FunctionArgs args;
        private StringConversion conversion;
        private OldStringConversion oldconversion;

        public BState() {
            declaringClassCache = new HashMap<>();
            try {
                declaringClass = new Class<?>[]{
                        DateTime.class.getMethod("toString").getDeclaringClass(),
                        HashSet.class.getMethod("toString").getDeclaringClass(),
                        String.class.getMethod("toString").getDeclaringClass(),
                        ClassLoader.class.getMethod("toString").getDeclaringClass()
                };
                objects = new Object[]{
                        new DateTime(),
                        new HashSet<>(),
                        "hallo",
                        DeclaringClassBenchmark.class.getClassLoader()
                };
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            context = EvaluationContext.emptyContext();
            args = new FunctionArgs(new StringConversion(), Maps.newHashMap());
            conversion = new StringConversion();
            oldconversion = new OldStringConversion();
        }


    }

    @Benchmark
    public void declaringClass(Blackhole bh, BState state) throws NoSuchMethodException {
        final Object obj = state.objects[state.i++ % 4];
        if ((obj.getClass().getMethod("toString").getDeclaringClass() != Object.class)) {
            bh.consume(obj.toString());
        } else {
            bh.consume(obj);
        }
    }

    @Benchmark
    public void declaringClassCached(Blackhole bh, BState state) throws NoSuchMethodException {
        final Object obj = state.objects[state.i % 4];
        state.i++;
        Class<?> declaringClass = state.declaringClassCache.get(obj.getClass());
        if (declaringClass == null) {
            declaringClass = obj.getClass().getMethod("toString").getDeclaringClass();
            state.declaringClassCache.put(obj.getClass(), declaringClass);
        }
        if ((declaringClass != Object.class)) {
            bh.consume(obj.toString());
        } else {
            bh.consume(obj);
        }
    }

    @Benchmark
    public void declaringClassCachedAndSwitch(Blackhole bh, BState state) throws NoSuchMethodException {
        final Object obj = state.objects[state.i % 4];
        state.i++;

        if (obj instanceof DateTime) {
            bh.consume(obj.toString());
        } else if (obj instanceof HashSet) {
            bh.consume(obj.toString());
        } else if (obj instanceof String) {
            bh.consume(obj.toString());
        } else {
            Class<?> declaringClass = state.declaringClassCache.get(obj.getClass());
            if (declaringClass == null) {
                declaringClass = obj.getClass().getMethod("toString").getDeclaringClass();
                state.declaringClassCache.put(obj.getClass(), declaringClass);
            }
            if ((declaringClass != Object.class)) {
                bh.consume(obj.toString());
            } else {
                bh.consume(obj);
            }
        }
    }

    @Benchmark
    public void instanceOfSwitch(Blackhole bh, BState state) {
        final Object obj = state.objects[state.i++ % 4];
        if (obj instanceof DateTime) {
            bh.consume(obj.toString());
        } else if (obj instanceof HashSet) {
            bh.consume(obj.toString());
        } else if (obj instanceof String) {
            bh.consume(obj.toString());
        } else {
            bh.consume(obj);
        }
    }

    @Benchmark
    public void toString(Blackhole bh, BState state) throws NoSuchMethodException {
        final Object obj = state.objects[state.i++ % 4];
        bh.consume(obj.toString());
    }

    @Benchmark
    public void stringConversion(Blackhole bh, BState state) {
        state.args.setPreComputedValue("value", state.objects[state.i++ % 4]);

        bh.consume(state.conversion.evaluate(state.args, state.context));
    }

    @Benchmark
    public void oldStringConversion(Blackhole bh, BState state) {
        state.args.setPreComputedValue("value", state.objects[state.i++ % 4]);

        bh.consume(state.oldconversion.evaluate(state.args, state.context));
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(DeclaringClassBenchmark.class.getSimpleName())
                .warmupIterations(2)
                .measurementIterations(5)
                .threads(1)
                .forks(1)
                .build();

        new Runner(opt).run();
    }

    @SuppressWarnings("Duplicates")
    public static class StringConversion extends AbstractFunction<String> {

        public static final String NAME = "tostring";

        private final ParameterDescriptor<Object, Object> valueParam = object("value").build();
        private final ParameterDescriptor<String, String> defaultParam = string("default").optional().build();

        // this is per-thread to save an expensive concurrent hashmap access
        private final ThreadLocal<LinkedHashMap<Class<?>, Class<?>>> declaringClassCache;

        public StringConversion() {
            declaringClassCache = new ThreadLocal<LinkedHashMap<Class<?>, Class<?>>>() {
                @Override
                protected LinkedHashMap<Class<?>, Class<?>> initialValue() {
                    return new LinkedHashMap<Class<?>, Class<?>>() {
                        @Override
                        protected boolean removeEldestEntry(Map.Entry<Class<?>, Class<?>> eldest) {
                            return size() > 1024;
                        }
                    };
                }
            };
        }

        @Override
        public String evaluate(FunctionArgs args, EvaluationContext context) {
            final Object evaluated = valueParam.required(args, context);
            // fast path for the most common targets
            if (evaluated instanceof String
                    || evaluated instanceof Number
                    || evaluated instanceof Boolean
                    || evaluated instanceof DateTime) {
                return evaluated.toString();
            } else {
                try {
                    // slow path, we aren't sure that the object's class actually overrides toString() so we'll look it up.
                    final Class<?> klass = evaluated.getClass();
                    final LinkedHashMap<Class<?>, Class<?>> classCache = declaringClassCache.get();

                    Class<?> declaringClass = classCache.get(klass);
                    if (declaringClass == null) {
                        declaringClass = klass.getMethod("toString").getDeclaringClass();
                        classCache.put(klass, declaringClass);
                    }
                    if ((declaringClass != Object.class)) {
                        return evaluated.toString();
                    } else {
                        return defaultParam.optional(args, context).orElse("");
                    }
                } catch (NoSuchMethodException ignored) {
                    // should never happen because toString is always there
                    return defaultParam.optional(args, context).orElse("");
                }
            }
        }

        @Override
        public FunctionDescriptor<String> descriptor() {
            return FunctionDescriptor.<String>builder()
                    .name(NAME)
                    .returnType(String.class)
                    .params(of(
                            valueParam,
                            defaultParam
                    ))
                    .build();
        }
    }

    @SuppressWarnings("Duplicates")
    public static class OldStringConversion extends AbstractFunction<String> {

        public static final String NAME = "tostring";

        private final ParameterDescriptor<Object, Object> valueParam = object("value").build();
        private final ParameterDescriptor<String, String> defaultParam = string("default").optional().build();


        @Override
        public String evaluate(FunctionArgs args, EvaluationContext context) {
            final Object evaluated = valueParam.required(args, context);
            if (evaluated instanceof String) {
                return (String) evaluated;
            } else {
                try {
                    if ((evaluated.getClass().getMethod("toString").getDeclaringClass() != Object.class)) {
                        return evaluated.toString();
                    } else {
                        return defaultParam.optional(args, context).orElse("");
                    }
                } catch (NoSuchMethodException ignored) {
                    // should never happen because toString is always there
                    return defaultParam.optional(args, context).orElse("");
                }
            }
        }

        @Override
        public FunctionDescriptor<String> descriptor() {
            return FunctionDescriptor.<String>builder()
                    .name(NAME)
                    .returnType(String.class)
                    .params(of(
                            valueParam,
                            defaultParam
                    ))
                    .build();
        }
    }

}

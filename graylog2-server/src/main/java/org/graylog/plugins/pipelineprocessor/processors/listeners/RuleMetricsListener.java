/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.pipelineprocessor.processors.listeners;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.Stage;
import org.graylog2.plugin.Message;
import org.graylog2.shared.metrics.MetricUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * This interpreter listener maintains timer metrics for rules.
 */
public class RuleMetricsListener implements InterpreterListener {
    public enum Type {
        EXECUTE, EVALUATE
    }

    private final MetricRegistry metricRegistry;
    private final Map<String, Timer.Context> evaluateTimers = new HashMap<>();
    private final Map<String, Timer.Context> executeTimers = new HashMap<>();

    public RuleMetricsListener(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    public static String getMetricName(String ruleId, Type type) {
        return name(Rule.class, ruleId, "trace", type.toString().toLowerCase(Locale.US), "duration");
    }

    private void startTimer(Rule rule, Type type, Map<String, Timer.Context> timers) {
        if (rule.id() != null) {
            final Timer timer = MetricUtils.getOrRegister(metricRegistry, getMetricName(rule.id(), type), new Timer());
            timers.put(rule.id(), timer.time());
        }
    }

    private void stopTimer(Rule rule, Map<String, Timer.Context> timers) {
        if (rule.id() != null) {
            final Timer.Context timer = timers.get(rule.id());
            if (timer != null) {
                timer.stop();
            }
        }
    }

    @Override
    public void startProcessing() {
    }

    @Override
    public void finishProcessing() {
    }

    @Override
    public void processStreams(Message message, Set<Pipeline> pipelines, Set<String> streams) {
    }

    @Override
    public void enterStage(Stage stage) {
    }

    @Override
    public void exitStage(Stage stage) {
    }

    @Override
    public void evaluateRule(Rule rule, Pipeline pipeline) {
        startTimer(rule, Type.EVALUATE, evaluateTimers);
    }

    @Override
    public void failEvaluateRule(Rule rule, Pipeline pipeline) {
        stopTimer(rule, evaluateTimers);
    }

    @Override
    public void satisfyRule(Rule rule, Pipeline pipeline) {
        stopTimer(rule, evaluateTimers);
    }

    @Override
    public void dissatisfyRule(Rule rule, Pipeline pipeline) {
        stopTimer(rule, evaluateTimers);
    }

    @Override
    public void executeRule(Rule rule, Pipeline pipeline) {
        startTimer(rule, Type.EXECUTE, executeTimers);
    }

    @Override
    public void finishExecuteRule(Rule rule, Pipeline pipeline) {
        stopTimer(rule, executeTimers);
    }

    @Override
    public void failExecuteRule(Rule rule, Pipeline pipeline) {
    }

    @Override
    public void continuePipelineExecution(Pipeline pipeline, Stage stage) {
    }

    @Override
    public void stopPipelineExecution(Pipeline pipeline, Stage stage) {
    }
}

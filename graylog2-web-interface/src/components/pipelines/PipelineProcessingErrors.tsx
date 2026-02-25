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
import * as React from 'react';
import { useEffect, useMemo } from 'react';
import numeral from 'numeral';

import { CounterRate } from 'components/metrics';
import usePipelineRulesMetadata from 'components/rules/hooks/usePipelineRulesMetadata';
import type { PipelineType } from 'components/pipelines/types';
import { useStore } from 'stores/connect';
import type { ClusterMetric, Metric, NodeMetric } from 'stores/metrics/MetricsStore';
import { MetricsActions, MetricsStore } from 'stores/metrics/MetricsStore';

type Props = {
  pipeline: PipelineType;
};

const INITIAL_METRIC = {
  full_name: '',
  count: 0,
};

const METRIC_PREFIX = 'org.graylog.plugins.pipelineprocessor.ast.Rule';
const METRIC_SUFFIX = 'failed';

const metricName = (ruleId: string, pipelineId: string, stage: number) =>
  `${METRIC_PREFIX}.${ruleId}.${pipelineId}.${stage}.${METRIC_SUFFIX}`;

const metricCount = (metric?: Metric): number => {
  if (!metric) {
    return 0;
  }

  switch (metric.type) {
    case 'counter':
      return metric.metric.count;
    case 'gauge':
      return metric.metric.value;
    case 'histogram':
      return metric.metric.count;
    case 'meter':
      return metric.metric.rate.total;
    case 'timer':
      return metric.metric.rate.total;
    default:
      return 0;
  }
};

export const getPipelineRuleFailureMetricNames = (pipeline: PipelineType, ruleIds: string[]): Array<string> => {
  const stages = Array.from(new Set(pipeline.stages.map(({ stage }) => stage)));
  const uniqueRuleIds = Array.from(new Set(ruleIds));

  return uniqueRuleIds.flatMap((ruleId) => stages.map((stage) => metricName(ruleId, pipeline.id, stage)));
};

const PipelineProcessingErrors = ({ pipeline }: Props) => {
  const { data: pipelineRulesMetadata } = usePipelineRulesMetadata(pipeline.id, {
    enabled: !!pipeline.id,
  });
  const { metrics }: { metrics: ClusterMetric } = useStore(MetricsStore, (state) => ({
    metrics: state.metrics ?? {},
  }));

  const metricNames = useMemo(
    () => getPipelineRuleFailureMetricNames(pipeline, pipelineRulesMetadata?.rules ?? []),
    [pipeline, pipelineRulesMetadata?.rules],
  );

  useEffect(() => {
    metricNames.forEach((name) => MetricsActions.addGlobal(name));

    return () => {
      metricNames.forEach((name) => MetricsActions.removeGlobal(name));
    };
  }, [metricNames]);

  const totalErrors = useMemo(
    () =>
      Object.values(metrics ?? {}).reduce((clusterTotal: number, nodeMetrics: NodeMetric) => {
        const nodeTotal = metricNames.reduce(
          (sum: number, currentMetricName: string) => sum + metricCount(nodeMetrics[currentMetricName]),
          0,
        );

        return clusterTotal + nodeTotal;
      }, 0),
    [metricNames, metrics],
  );

  return (
    <span>
      <CounterRate
        metric={{
          ...INITIAL_METRIC,
          full_name: `pipeline.${pipeline.id}.failed`,
          count: totalErrors,
        }}
        suffix="errors/s"
      />
      <br />
      <span className="number-format">({numeral(totalErrors).format('0')} total)</span>
    </span>
  );
};

export default PipelineProcessingErrors;

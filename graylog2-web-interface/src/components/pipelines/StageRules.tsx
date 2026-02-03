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
import { useCallback } from 'react';

import { DataTable, Icon } from 'components/common';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import { MetricContainer, CounterRate } from 'components/metrics';
import type { PipelineType, StageType } from 'components/pipelines/types';
import type { RuleType } from 'stores/rules/RulesStore';
import RuleDeprecationInfo from 'components/rules/RuleDeprecationInfo';

type Props = {
  pipeline: PipelineType;
  stage: StageType;
  rules?: Array<RuleType>;
};

type InvalidRule = {
  id: string;
  title: string;
  description: string;
  isInvalid: true;
};

type RuleData = RuleType | InvalidRule;

const StageRules = ({ pipeline, stage, rules = [] }: Props) => {
  const headers = ['Title', 'Description', 'Throughput', 'Errors'];

  const headerCellFormatter = useCallback((header: string) => <th>{header}</th>, []);

  const getMetricName = useCallback(
    (ruleId: string, metricType: 'executed' | 'failed'): string =>
      `org.graylog.plugins.pipelineprocessor.ast.Rule.${ruleId}.${pipeline.id}.${stage.stage}.${metricType}`,
    [pipeline.id, stage.stage],
  );

  const getRuleData = useCallback(
    (ruleArg: RuleType | undefined, ruleIdx: number): RuleData => {
      if (ruleArg) return ruleArg;

      const invalidRuleName = stage.rules?.[ruleIdx] ?? 'unknown';

      return {
        id: `invalid-${ruleIdx}`,
        title: invalidRuleName,
        description: `Rule ${invalidRuleName} has been renamed or removed. This rule will be skipped.`,
        isInvalid: true,
      };
    },
    [stage.rules],
  );

  const ruleRowFormatter = useCallback(
    (ruleArg: RuleType | undefined, ruleIdx: number) => {
      const rule = getRuleData(ruleArg, ruleIdx);
      const isInvalid = 'isInvalid' in rule && rule.isInvalid;

      const ruleTitle = (() => {
        if (isInvalid) {
          return (
            <span>
              <Icon name="warning" className="text-danger" /> {rule.title}
            </span>
          );
        }

        const queryParam = 'rule_builder' in rule && rule.rule_builder ? '?rule_builder=true' : '';

        return <Link to={`${Routes.SYSTEM.PIPELINES.RULE(rule.id)}${queryParam}`}>{rule.title}</Link>;
      })();

      return (
        <tr key={rule.id}>
          <td>
            {ruleTitle}
            {!isInvalid && <RuleDeprecationInfo ruleId={rule.id} />}
          </td>
          <td>{rule.description}</td>
          <td>
            <MetricContainer name={getMetricName(rule.id, 'executed')}>
              <CounterRate suffix="msg/s" />
            </MetricContainer>
          </td>
          <td>
            <MetricContainer name={getMetricName(rule.id, 'failed')}>
              <CounterRate showTotal suffix="errors/s" />
            </MetricContainer>
          </td>
        </tr>
      );
    },
    [getRuleData, getMetricName],
  );

  return (
    <DataTable
      id={`stage-rules-${pipeline.id}-${stage.stage}`}
      className="table-hover"
      headers={headers}
      headerCellFormatter={headerCellFormatter}
      rows={rules}
      dataRowFormatter={ruleRowFormatter}
      noDataText="This stage has no rules yet. Click on edit to add some."
      filterLabel=""
      filterKeys={[]}
    />
  );
};

export default StageRules;

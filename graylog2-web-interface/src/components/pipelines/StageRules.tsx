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
import { useTranslation } from 'react-i18next';

import { Button } from 'components/bootstrap';
import { DataTable, Icon, Link } from 'components/common';
import Routes from 'routing/Routes';
import { MetricContainer, CounterRate } from 'components/metrics';
import type { PipelineType, StageType } from 'components/pipelines/types';
import type { RuleType } from 'components/rules/hooks/useRules';
import RuleDeprecationInfo from 'components/rules/RuleDeprecationInfo';
import {
  PipelineLoadCell,
  getStageRulePipelineSharePercent,
  useProcessingLoadContext,
  type ProcessingLoadResponse,
} from 'components/pipelines/processing-load';

type Props = {
  pipeline: PipelineType;
  stage: StageType;
  rules?: Array<RuleType | undefined>;
  canRemoveRoutingRules?: boolean;
  removingRuleId?: string;
  onRemoveRule?: (rule: RuleType) => void;
  showLoadColumn?: boolean;
  processingLoad?: ProcessingLoadResponse;
  processingLoadError?: boolean;
};

type InvalidRule = {
  id: string;
  title: string;
  description: string;
  isInvalid: true;
};

type RuleData = RuleType | InvalidRule;
export const INPUT_SETUP_WIZARD_ROUTING_RULE_DESCRIPTION = 'Input setup wizard routing rule';

export const isInputSetupWizardRoutingRule = (rule: RuleType | undefined): rule is RuleType =>
  !!rule && rule.description === INPUT_SETUP_WIZARD_ROUTING_RULE_DESCRIPTION;
const isInvalidRule = (rule: RuleData): rule is InvalidRule => 'isInvalid' in rule && rule.isInvalid;

const STAGE_RULES_HEADER_KEYS = {
  Title: 'stage_rules.headers.title',
  Description: 'stage_rules.headers.description',
  Throughput: 'stage_rules.headers.throughput',
  Errors: 'stage_rules.headers.errors',
  'Rule Load (15m)': 'stage_rules.headers.rule_load',
  Actions: 'stage_rules.headers.actions',
};

const StageRules = ({
  pipeline,
  stage,
  rules = [],
  canRemoveRoutingRules = false,
  removingRuleId = undefined,
  onRemoveRule = undefined,
  showLoadColumn: showLoadColumnProp = undefined,
  processingLoad: processingLoadProp = undefined,
  processingLoadError: processingLoadErrorProp = undefined,
}: Props) => {
  const { t } = useTranslation('pipelines');
  const {
    metricsEnabled,
    processingLoad: processingLoadContext,
    processingLoadError: processingLoadErrorContext,
  } = useProcessingLoadContext();
  const showLoadColumn = showLoadColumnProp ?? metricsEnabled;
  const processingLoad = processingLoadProp ?? processingLoadContext;
  const processingLoadError = processingLoadErrorProp ?? processingLoadErrorContext;
  const headers = [
    'Title',
    'Description',
    'Throughput',
    'Errors',
    ...(showLoadColumn ? ['Rule Load (15m)'] : []),
    ...(canRemoveRoutingRules ? ['Actions'] : []),
  ];

  // The English header texts identify the columns, the displayed texts are translated.
  const headerCellFormatter = useCallback(
    (header: string) => <th>{t(STAGE_RULES_HEADER_KEYS[header] ?? header)}</th>,
    [t],
  );

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
        description: t('stage_rules.invalid_rule', { name: invalidRuleName }),
        isInvalid: true,
      };
    },
    [stage.rules, t],
  );

  const ruleRowFormatter = useCallback(
    (ruleArg: RuleType | undefined, ruleIdx: number) => {
      const rule = getRuleData(ruleArg, ruleIdx);
      const isInvalid = isInvalidRule(rule);
      const removableRule: RuleType | undefined = isInvalid ? undefined : rule;
      const showRemoveAction =
        canRemoveRoutingRules && isInputSetupWizardRoutingRule(removableRule) && typeof onRemoveRule === 'function';

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
              <CounterRate suffix={t('stage_rules.throughput_suffix')} />
            </MetricContainer>
          </td>
          <td>
            <MetricContainer name={getMetricName(rule.id, 'failed')}>
              <CounterRate showTotal suffix={t('stage_rules.errors_suffix')} />
            </MetricContainer>
          </td>
          {showLoadColumn && (
            <td>
              {isInvalid ? null : (
                <PipelineLoadCell
                  loadPercent={getStageRulePipelineSharePercent(processingLoad, pipeline.id, rule.id, stage.stage)}
                  error={processingLoadError}
                />
              )}
            </td>
          )}
          {canRemoveRoutingRules && (
            <td className="actions">
              {showRemoveAction && (
                <Button
                  bsStyle="danger"
                  bsSize="xsmall"
                  disabled={Boolean(removingRuleId)}
                  onClick={() => removableRule && onRemoveRule(removableRule)}
                  title={t('stage_rules.remove_title', { title: removableRule.title })}>
                  {removingRuleId === removableRule.id ? t('stage_rules.removing') : t('stage_rules.remove')}
                </Button>
              )}
            </td>
          )}
        </tr>
      );
    },
    [
      canRemoveRoutingRules,
      getRuleData,
      getMetricName,
      onRemoveRule,
      removingRuleId,
      showLoadColumn,
      processingLoad,
      processingLoadError,
      pipeline.id,
      stage.stage,
      t,
    ],
  );

  return (
    <DataTable
      id={`stage-rules-${pipeline.id}-${stage.stage}`}
      headers={headers}
      headerCellFormatter={headerCellFormatter}
      rows={rules}
      dataRowFormatter={ruleRowFormatter}
      noDataText={t('stage_rules.no_rules')}
      filterLabel=""
      filterKeys={[]}
    />
  );
};

export default StageRules;

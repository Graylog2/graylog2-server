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
import React, { useCallback, useMemo, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Trans, useTranslation } from 'react-i18next';

import { Col, Button } from 'components/bootstrap';
import { ConfirmDialog, EntityListItem, Spinner } from 'components/common';
import { MetricContainer, CounterRate } from 'components/metrics';
import type { PipelineType, StageType } from 'components/pipelines/types';
import { useRules, deleteRule } from 'components/rules/hooks/useRules';
import type { RuleType } from 'components/rules/hooks/useRules';
import usePermissions from 'hooks/usePermissions';
import { PIPELINE_QUERY_KEY } from 'hooks/usePipeline';

import StageForm from './StageForm';
import StageRules, { INPUT_SETUP_WIZARD_ROUTING_RULE_DESCRIPTION } from './StageRules';

const DEFAULT_ROUTING_PIPELINE = 'Default Routing';

type Props = {
  stage: StageType;
  pipeline: PipelineType;
  isLastStage: boolean;
  onUpdate: (nextStage: StageType, callback: () => void) => void;
  onDelete: () => void;
  disableEdit?: boolean;
};

const Stage = ({ stage, pipeline, isLastStage, onUpdate, onDelete, disableEdit = false }: Props) => {
  const { t } = useTranslation('pipelines');
  const { isPermitted } = usePermissions();
  const queryClient = useQueryClient();
  const { data: allRules } = useRules();
  const [removingRuleId, setRemovingRuleId] = useState<string | undefined>(undefined);
  const [rulePendingRemoval, setRulePendingRemoval] = useState<RuleType | undefined>(undefined);

  const stageRules = useMemo(
    () => stage.rules.map((name) => allRules?.find((rule) => rule.title === name)),
    [allRules, stage.rules],
  );

  const canRemoveRoutingRules = pipeline.title === DEFAULT_ROUTING_PIPELINE && isPermitted('pipeline_rule:delete');

  const openRemoveRoutingRuleDialog = useCallback(
    (rule: RuleType) => {
      if (removingRuleId) return;
      if (!rule?.id || rule.description !== INPUT_SETUP_WIZARD_ROUTING_RULE_DESCRIPTION) return;
      setRulePendingRemoval(rule);
    },
    [removingRuleId],
  );

  const closeRemoveRoutingRuleDialog = useCallback(() => {
    setRulePendingRemoval(undefined);
  }, []);

  const confirmRemoveRoutingRule = useCallback(() => {
    if (removingRuleId) return;
    if (!rulePendingRemoval?.id || rulePendingRemoval.description !== INPUT_SETUP_WIZARD_ROUTING_RULE_DESCRIPTION)
      return;

    const ruleToDelete = rulePendingRemoval;

    setRulePendingRemoval(undefined);
    setRemovingRuleId(ruleToDelete.id);
    deleteRule(ruleToDelete)
      .then(() => queryClient.invalidateQueries({ queryKey: [...PIPELINE_QUERY_KEY, pipeline.id] }))
      .catch(() => {
        /* feedback handled in deleteRule */
      })
      .finally(() => setRemovingRuleId(undefined));
  }, [pipeline.id, queryClient, removingRuleId, rulePendingRemoval]);

  const suffix = t('stage.contains_rules', { count: stage.rules.length });

  const throughput = (
    <MetricContainer
      name={`org.graylog.plugins.pipelineprocessor.ast.Pipeline.${pipeline.id}.stage.${stage.stage}.executed`}>
      <CounterRate
        showTotal={false}
        prefix={`${t('list_item.throughput_prefix')} `}
        suffix={t('list_item.throughput_suffix')}
      />
    </MetricContainer>
  );

  const actions = [
    <Button
      disabled={!isPermitted('pipeline:edit') || disableEdit}
      key={`delete-stage-${stage}`}
      bsStyle="danger"
      onClick={onDelete}>
      {t('list_item.delete')}
    </Button>,
    <StageForm
      key={`edit-stage-${stage}`}
      pipeline={pipeline}
      stage={stage}
      save={onUpdate}
      disableEdit={disableEdit}
    />,
  ];

  let description;

  if (isLastStage) {
    description = t('stage.last_stage');
  } else {
    const matchKeys = {
      ALL: 'stage.continue_all',
      EITHER: 'stage.continue_either',
      PASS: 'stage.continue_pass',
    };

    description = (
      <span>
        <Trans t={t} i18nKey={matchKeys[stage.match] ?? 'stage.continue_unknown'} components={{ matchText: <strong /> }} />
      </span>
    );
  }

  const block = (
    <span>
      {description}
      <br />
      {throughput}
    </span>
  );

  const content = allRules ? (
    <StageRules
      pipeline={pipeline}
      stage={stage}
      rules={stageRules}
      canRemoveRoutingRules={canRemoveRoutingRules}
      removingRuleId={removingRuleId}
      onRemoveRule={openRemoveRoutingRuleDialog}
    />
  ) : (
    <Spinner />
  );

  return (
    <>
      <EntityListItem
        title={t('stage.title', { stage: stage.stage })}
        titleSuffix={suffix}
        actions={actions}
        description={block}
        contentRow={<Col md={12}>{content}</Col>}
      />
      {rulePendingRemoval && (
        <ConfirmDialog
          title={t('stage.remove_routing_rule_title')}
          show
          onConfirm={confirmRemoveRoutingRule}
          onCancel={closeRemoveRoutingRuleDialog}
          btnConfirmText={t('stage_rules.remove')}>
          <span>
            <Trans
              t={t}
              i18nKey="stage.remove_routing_rule_confirm"
              values={{ title: rulePendingRemoval.title }}
              components={{ ruleTitle: <b /> }}
            />
          </span>
        </ConfirmDialog>
      )}
    </>
  );
};

export default Stage;

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
import React, { useEffect } from 'react';
import { useParams } from 'react-router-dom';

import { DocumentTitle, Spinner } from 'components/common';
import Rule from 'components/rules/Rule';
import Routes from 'routing/Routes';
import useQuery from 'routing/useQuery';
import { PipelineRulesProvider } from 'components/rules/RuleContext';
import { useRule } from 'components/rules/hooks/useRules';
import usePipelines from 'hooks/usePipelines';
import type { PipelineType, StageType } from 'components/pipelines/types';

import useHistory from '../routing/useHistory';

function filterPipelines(pipelines: Array<PipelineType> = [], title = '') {
  return pipelines.filter((pipeline) => pipeline.stages.some((stage: StageType) => stage.rules.indexOf(title) !== -1));
}

const RuleDetailsPage = () => {
  const { ruleId } = useParams<{ ruleId: string }>();
  const isNewRule = ruleId === 'new';
  const { data: pipelines, isInitialLoading: isInitialLoadingPipelines } = usePipelines({ enabled: !isNewRule });
  const {
    data: currentRule,
    isInitialLoading: isInitialLoadingRule,
    error: ruleError,
  } = useRule(ruleId, { enabled: !isNewRule });
  const history = useHistory();
  const { rule_builder } = useQuery();

  const isRuleBuilder = rule_builder === 'true';
  const title = currentRule?.title || '';
  const pageTitle = isNewRule ? 'New pipeline rule' : `Pipeline rule ${title}`;

  const pipelinesUsingRule = isNewRule ? [] : filterPipelines(pipelines, title);

  useEffect(() => {
    if (!isNewRule && (ruleError as { status?: number })?.status === 404) {
      history.push(Routes.SYSTEM.PIPELINES.RULES);
    }
  }, [history, isNewRule, ruleError]);

  const isLoading = !isNewRule && (isInitialLoadingRule || isInitialLoadingPipelines);

  if (isLoading) {
    return <Spinner text="Loading Rule Details..." />;
  }

  return (
    <DocumentTitle title={pageTitle}>
      <PipelineRulesProvider usedInPipelines={pipelinesUsingRule} rule={currentRule}>
        <Rule create={isNewRule} isRuleBuilder={isRuleBuilder} title={title} />
      </PipelineRulesProvider>
    </DocumentTitle>
  );
};

export default RuleDetailsPage;

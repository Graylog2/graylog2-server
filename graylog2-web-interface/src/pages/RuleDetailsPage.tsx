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
import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';

import { useStore } from 'stores/connect';
import { DocumentTitle, Spinner } from 'components/common';
import Rule from 'components/rules/Rule';
import Routes from 'routing/Routes';
import useQuery from 'routing/useQuery';
import { PipelineRulesProvider } from 'components/rules/RuleContext';
import type { RulesStoreState } from 'stores/rules/RulesStore';
import { RulesActions, RulesStore } from 'stores/rules/RulesStore';
import usePipelines from 'hooks/usePipelines';

import useHistory from '../routing/useHistory';

const getCurrentRule = (ruleStoreState: RulesStoreState, ruleId: string) =>
  ruleStoreState?.rules?.filter((r) => r.id === ruleId)[0];

function filterPipelines(pipelines = [], title = '') {
  return pipelines.filter((pipeline) => pipeline.stages.some((stage) => stage.rules.indexOf(title) !== -1));
}

const RuleDetailsPage = () => {
  const { ruleId } = useParams<{ ruleId: string }>();
  const ruleStoreState = useStore(RulesStore);
  const isNewRule = ruleId === 'new';
  const { data: pipelines } = usePipelines({ enabled: !isNewRule });
  const [isLoading, setIsLoading] = useState(true);
  const [currentRule, setCurrentRule] = useState(undefined);
  const history = useHistory();
  const { rule_builder } = useQuery();

  const isRuleBuilder = rule_builder === 'true';
  const title = currentRule?.title || '';
  const pageTitle = isNewRule ? 'New pipeline rule' : `Pipeline rule ${title}`;

  const pipelinesUsingRule = isNewRule ? [] : filterPipelines(pipelines, title);

  useEffect(() => {
    setCurrentRule(getCurrentRule(ruleStoreState, ruleId));
  }, [ruleId, ruleStoreState]);

  useEffect(() => {
    if (isNewRule) {
      setIsLoading(false);
    } else {
      RulesActions.get(ruleId).then(
        () => {},
        (error) => {
          if (error.status === 404) {
            history.push(Routes.SYSTEM.PIPELINES.RULES);
          }
        },
      );

      setIsLoading(!(currentRule && pipelines.length > 0));
    }
  }, [currentRule, history, isNewRule, ruleId, pipelines]);

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

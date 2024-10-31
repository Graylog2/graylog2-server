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
import { PipelinesStore, PipelinesActions } from 'stores/pipelines/PipelinesStore';
import { RulesActions, RulesStore } from 'stores/rules/RulesStore';

import useHistory from '../routing/useHistory';

function filterRules(rule, ruleId) {
  return rule?.rules?.filter((r) => r.id === ruleId)[0];
}

function filterPipelines(pipelines = [], title = '') {
  return pipelines.filter((pipeline) => pipeline.stages.some((stage) => stage.rules.indexOf(title) !== -1));
}

const RuleDetailsPage = () => {
  const { ruleId } = useParams<{ ruleId: string }>();
  const rule = useStore(RulesStore);
  const pipelines = useStore(PipelinesStore, ({ pipelines: state }) => state);
  const [isLoading, setIsLoading] = useState(true);
  const [filteredRule, setFilteredRule] = useState(undefined);
  const history = useHistory();
  const { rule_builder } = useQuery();

  const isRuleBuilder = rule_builder === 'true';
  const isNewRule = ruleId === 'new';
  const title = filteredRule?.title || '';
  const pageTitle = isNewRule ? 'New pipeline rule' : `Pipeline rule ${title}`;

  const pipelinesUsingRule = isNewRule ? [] : filterPipelines(pipelines, title);

  useEffect(() => {
    setFilteredRule(filterRules(rule, ruleId));
  }, [ruleId, rule]);

  useEffect(() => {
    if (isNewRule) {
      setIsLoading(false);
    } else {
      PipelinesActions.list();

      RulesActions.get(ruleId).then(() => {}, (error) => {
        if (error.status === 404) {
          history.push(Routes.SYSTEM.PIPELINES.RULES);
        }
      });

      setIsLoading(!(filteredRule && pipelines));
    }
  }, [filteredRule, history, isNewRule, ruleId, pipelines]);

  if (isLoading) {
    return <Spinner text="Loading Rule Details..." />;
  }

  return (
    <DocumentTitle title={pageTitle}>
      <PipelineRulesProvider usedInPipelines={pipelinesUsingRule} rule={filteredRule}>
        <Rule create={isNewRule} isRuleBuilder={isRuleBuilder} title={title} />
      </PipelineRulesProvider>
    </DocumentTitle>
  );
};

export default RuleDetailsPage;

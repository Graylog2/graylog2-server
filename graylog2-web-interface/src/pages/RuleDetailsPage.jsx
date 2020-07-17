import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';

import connect from 'stores/connect';
import { DocumentTitle, Spinner } from 'components/common';
import Rule from 'components/rules/Rule';
import CombinedProvider from 'injection/CombinedProvider';
import { PipelineRulesProvider } from 'components/rules/RuleContext';

const { RulesStore, RulesActions } = CombinedProvider.get('Rules');
const { PipelinesStore, PipelinesActions } = CombinedProvider.get('Pipelines');

function filterRules(rule, ruleId) {
  return rule?.rules?.filter((r) => r.id === ruleId)[0];
}

function filterPipelines(pipelines = [], title = '') {
  return pipelines.filter((pipeline) => {
    return pipeline.stages.some((stage) => stage.rules.indexOf(title) !== -1);
  });
}

const RuleDetailsPage = ({ params, rule, pipelines }) => {
  const [isLoading, setIsLoading] = useState(true);
  const [filteredRule, setFilteredRule] = useState(undefined);

  const isNewRule = params.ruleId === 'new';
  const title = filteredRule?.title || '';
  const pageTitle = isNewRule ? 'New pipeline rule' : `Pipeline rule ${title}`;

  const pipelinesUsingRule = isNewRule ? [] : filterPipelines(pipelines, title);

  useEffect(() => {
    setFilteredRule(filterRules(rule, params.ruleId));
  }, [params, rule]);

  useEffect(() => {
    if (isNewRule) {
      setIsLoading(false);
    } else {
      PipelinesActions.list();
      RulesActions.get(params.ruleId);
      setIsLoading(!(filteredRule && pipelines));
    }
  }, [filteredRule]);

  if (isLoading) {
    return <Spinner text="Loading Rule Details..." />;
  }

  return (
    <DocumentTitle title={pageTitle}>
      <PipelineRulesProvider usedInPipelines={pipelinesUsingRule} rule={filteredRule}>
        <Rule create={isNewRule} title={title} />
      </PipelineRulesProvider>
    </DocumentTitle>
  );
};

RuleDetailsPage.propTypes = {
  params: PropTypes.shape({
    ruleId: PropTypes.string,
  }).isRequired,
  rule: PropTypes.shape({
    id: PropTypes.string,
    title: PropTypes.string,
    description: PropTypes.string,
    source: PropTypes.string,
    value: PropTypes.string,
  }),
  pipelines: PropTypes.any,
};

RuleDetailsPage.defaultProps = {
  rule: undefined,
  pipelines: undefined,
};

export default connect(RuleDetailsPage, {
  rule: RulesStore,
  pipelines: PipelinesStore,
},
({ pipelines, ...restProps }) => ({
  pipelines: pipelines.pipelines || [],
  ...restProps,
}));

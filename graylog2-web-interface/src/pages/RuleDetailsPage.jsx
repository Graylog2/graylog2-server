import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';

import connect from 'stores/connect';
import { DocumentTitle, Spinner } from 'components/common';
import Rule from 'components/rules/Rule';
import CombinedProvider from 'injection/CombinedProvider';
import { PipelineRulesProvider } from 'components/rules/RuleContext';

const { RulesStore, RulesActions } = CombinedProvider.get('Rules');
const { PipelinesStore, PipelinesActions } = CombinedProvider.get('Pipelines');

function filterRules({ rules }, { params }) {
  return rules ? rules.filter((r) => r.id === params.ruleId)[0] : undefined;
}

const RuleDetailsPage = ({ params, rule, pipelines }) => {
  const isNewRule = params.ruleId === 'new';
  const title = rule?.title || '';
  const pageTitle = isNewRule ? 'New pipeline rule' : `Pipeline rule ${title}`;
  const [isLoading, setIsLoading] = useState(false);

  const pipelinesUsingRule = isNewRule ? [] : pipelines.filter((pipeline) => {
    return pipeline.stages.some((stage) => stage.rules.indexOf(title) !== -1);
  });

  useEffect(() => {
    if (!isNewRule) {
      PipelinesActions.list();
      RulesActions.get(params.ruleId);
      setIsLoading(!(rule && pipelines));
    }
  }, []);

  if (isLoading) {
    return <Spinner />;
  }

  return (
    <DocumentTitle title={pageTitle}>
      <PipelineRulesProvider usedInPipelines={pipelinesUsingRule} rule={rule}>
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
({ rule, pipelines, ...otherProps }) => ({
  rule: filterRules(rule, otherProps),
  pipelines: pipelines.pipelines,
  ...otherProps,
}));

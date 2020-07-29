import React, { useContext } from 'react';
import PropTypes from 'prop-types';
import { Link } from 'react-router';

import Routes from 'routing/Routes';
import { PipelineRulesContext } from 'components/rules/RuleContext';
import { Input } from 'components/bootstrap';

import RuleFormStyle from './RuleForm.css';

const PipelinesUsingRule = ({ create }) => {
  const { usedInPipelines } = useContext(PipelineRulesContext);

  if (create) {
    return null;
  }

  const formattedPipelines = () => usedInPipelines.map((pipeline) => {
    return (
      <li key={pipeline.id}>
        <Link to={Routes.SYSTEM.PIPELINES.PIPELINE(pipeline.id)}>
          {pipeline.title}
        </Link>
      </li>
    );
  });

  return (
    <Input id="used-in-pipelines" label="Used in pipelines" help="Pipelines that use this rule in one or more of their stages.">
      <div className="form-control-static">
        {usedInPipelines.length === 0
          ? 'This rule is not being used in any pipelines.' : (
            <ul className={RuleFormStyle.usedInPipelines}>
              {formattedPipelines()}
            </ul>
          )}
      </div>
    </Input>
  );
};

PipelinesUsingRule.propTypes = {
  create: PropTypes.bool.isRequired,
};

export default PipelinesUsingRule;

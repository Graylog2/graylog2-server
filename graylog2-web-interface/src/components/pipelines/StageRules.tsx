import React from 'react';
import PropTypes from 'prop-types';

import { DataTable, Icon } from 'components/common';
import { Link } from 'components/graylog/router';
import Routes from 'routing/Routes';
import { MetricContainer, CounterRate } from 'components/metrics';
import { PipelineType, StageType } from 'stores/pipelines/PipelinesStore';
import { RuleType } from 'stores/rules/RulesStore';

type Props = {
  pipeline: PipelineType,
  stage: StageType,
  rules: RuleType[],
};

const StageRules = ({ pipeline, stage, rules }: Props) => {
  const headers = ['Title', 'Description', 'Throughput', 'Errors'];

  const _ruleRowFormatter = (ruleArg, ruleIdx) => {
    let rule = ruleArg;

    let ruleTitle;

    // this can happen when a rule has been renamed, but not all references are updated
    if (!rule) {
      rule = {
        id: `invalid-${ruleIdx}`,
        description: `Rule ${stage.rules[ruleIdx]} has been renamed or removed. This rule will be skipped.`,
      };

      ruleTitle = <span><Icon name="exclamation-triangle" className="text-danger" /> {stage.rules[ruleIdx]}</span>;
    } else {
      ruleTitle = (
        <Link to={Routes.SYSTEM.PIPELINES.RULE(rule.id)}>
          {rule.title}
        </Link>
      );
    }

    return (
      <tr key={rule.id}>
        <td style={{ width: 400 }}>
          {ruleTitle}
        </td>
        <td>{rule.description}</td>
        <td>
          <MetricContainer name={`org.graylog.plugins.pipelineprocessor.ast.Rule.${rule.id}.${pipeline.id}.${stage.stage}.executed`}>
            <CounterRate zeroOnMissing suffix="msg/s" />
          </MetricContainer>
        </td>
        <td>
          <MetricContainer name={`org.graylog.plugins.pipelineprocessor.ast.Rule.${rule.id}.${pipeline.id}.${stage.stage}.failed`}>
            <CounterRate showTotal zeroOnMissing suffix="errors/s" />
          </MetricContainer>
        </td>
      </tr>
    );
  };

  return (
    <DataTable id="processing-timeline"
               className="table-hover"
               headers={headers}
               headerCellFormatter={(header) => (<th>{header}</th>)}
               rows={rules}
               dataRowFormatter={_ruleRowFormatter}
               noDataText="This stage has no rules yet. Click on edit to add some."
               filterLabel=""
               filterKeys={[]} />
  );
};

StageRules.propTypes = {
  pipeline: PropTypes.object.isRequired,
  stage: PropTypes.object.isRequired,
  rules: PropTypes.array,
};

StageRules.defaultProps = {
  rules: [],
};

export default StageRules;

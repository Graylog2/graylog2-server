import React, { PropTypes } from 'react';
import Reflux from 'reflux';
import { Col, Button } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import { DataTable, EntityListItem, Spinner } from 'components/common';
import RulesStore from 'rules/RulesStore';
import StageForm from './StageForm';
import { MetricContainer, CounterRate } from 'components/metrics';

import Routes from 'routing/Routes';

const Stage = React.createClass({
  propTypes: {
    stage: PropTypes.object.isRequired,
    pipeline: PropTypes.object.isRequired,
    isLastStage: PropTypes.bool,
    onUpdate: PropTypes.func.isRequired,
    onDelete: PropTypes.func.isRequired,
  },
  mixins: [Reflux.connect(RulesStore)],

  _ruleHeaderFormatter(header) {
    return <th>{header}</th>;
  },

  _ruleRowFormatter(stage, ruleArg, ruleIdx) {
    let rule = ruleArg;

    let ruleTitle;
    // this can happen when a rule has been renamed, but not all references are updated
    if (!rule) {
      rule = {
        id: `invalid-${ruleIdx}`,
        description: `Rule ${stage.rules[ruleIdx]} has been renamed or removed. This rule will be skipped.`,
      };
      ruleTitle = <span><i className="fa fa-warning text-danger"/> {stage.rules[ruleIdx]}</span>;
    } else {
      ruleTitle = (<LinkContainer to={Routes.pluginRoute('SYSTEM_PIPELINES_RULES_RULEID')(rule.id)}>
          <a>{rule.title}</a>
        </LinkContainer>
      );

    }
    return (
      <tr key={rule.id}>
        <td style={{ width: 400 }}>
          {ruleTitle}
        </td>
        <td>{rule.description}</td>
        <td>
          <MetricContainer name={`org.graylog.plugins.pipelineprocessor.ast.Rule.${rule.id}.${this.props.pipeline.id}.${stage.stage}.executed`}>
            <CounterRate zeroOnMissing suffix="msg/s" />
          </MetricContainer>
        </td>
        <td>
          <MetricContainer name={`org.graylog.plugins.pipelineprocessor.ast.Rule.${rule.id}.${this.props.pipeline.id}.${stage.stage}.failed`}>
            <CounterRate showTotal zeroOnMissing suffix="errors/s"/>
          </MetricContainer>
        </td>
      </tr>
    );
  },

  _formatRules(stage, rules) {
    const headers = ['Title', 'Description', 'Throughput', 'Errors'];

    return (
      <DataTable id="processing-timeline"
                 className="table-hover"
                 headers={headers}
                 headerCellFormatter={this._ruleHeaderFormatter}
                 rows={rules}
                 dataRowFormatter={(rule, i) => this._ruleRowFormatter(stage, rule, i)}
                 noDataText="This stage has no rules yet. Click on edit to add some."
                 filterLabel=""
                 filterKeys={[]} />
    );
  },

  render() {
    const stage = this.props.stage;

    let suffix = `Contains ${(stage.rules.length === 1 ? '1 rule' : `${stage.rules.length} rules`)}`;

    const throughput = (<MetricContainer name={`org.graylog.plugins.pipelineprocessor.ast.Pipeline.${this.props.pipeline.id}.stage.${stage.stage}.executed`}>
      <CounterRate showTotal={false} prefix="Throughput: " suffix="msg/s" />
    </MetricContainer>);

    const actions = [
      <Button key="delete-stage" bsStyle="primary" onClick={this.props.onDelete}>Delete</Button>,
      <StageForm key="edit-stage" stage={stage} save={this.props.onUpdate} />,
    ];

    let description;
    if (this.props.isLastStage) {
      description = 'There are no further stages in this pipeline. Once rules in this stage are applied, the pipeline will have finished processing.';
    } else {
      description = (
        <span>
          Messages satisfying <strong>{stage.match_all ? 'all rules' : 'at least one rule'}</strong>{' '}
          in this stage, will continue to the next stage.
        </span>
      );
    }

    let block = (<span>
      {description}
      <br />
      {throughput}
    </span>);
    let content;
    // We check if we have the rules details before trying to render them
    if (this.state.rules) {
      content = this._formatRules(stage, this.props.stage.rules.map(name => this.state.rules.filter(r => r.title === name)[0]));
    } else {
      content = <Spinner />;
    }

    return (
      <EntityListItem title={`Stage ${stage.stage}`}
                      titleSuffix={suffix}
                      actions={actions}
                      description={block}
                      contentRow={<Col md={12}>{content}</Col>} />
    );
  },
});

export default Stage;

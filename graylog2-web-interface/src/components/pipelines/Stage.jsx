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
import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { Link } from 'components/graylog/router';
import { Col, Button } from 'components/graylog';
import { DataTable, EntityListItem, Spinner, Icon } from 'components/common';
import { MetricContainer, CounterRate } from 'components/metrics';
import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';

import StageForm from './StageForm';

const { RulesStore } = CombinedProvider.get('Rules');

const Stage = createReactClass({
  displayName: 'Stage',

  propTypes: {
    stage: PropTypes.object.isRequired,
    pipeline: PropTypes.object.isRequired,
    isLastStage: PropTypes.bool.isRequired,
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
          <MetricContainer name={`org.graylog.plugins.pipelineprocessor.ast.Rule.${rule.id}.${this.props.pipeline.id}.${stage.stage}.executed`}>
            <CounterRate zeroOnMissing suffix="msg/s" />
          </MetricContainer>
        </td>
        <td>
          <MetricContainer name={`org.graylog.plugins.pipelineprocessor.ast.Rule.${rule.id}.${this.props.pipeline.id}.${stage.stage}.failed`}>
            <CounterRate showTotal zeroOnMissing suffix="errors/s" />
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
    const { onUpdate, pipeline, stage } = this.props;

    const suffix = `Contains ${(stage.rules.length === 1 ? '1 rule' : `${stage.rules.length} rules`)}`;

    const throughput = (
      <MetricContainer name={`org.graylog.plugins.pipelineprocessor.ast.Pipeline.${pipeline.id}.stage.${stage.stage}.executed`}>
        <CounterRate showTotal={false} prefix="Throughput: " suffix="msg/s" />
      </MetricContainer>
    );

    const actions = [
      <Button key="delete-stage" bsStyle="primary" onClick={this.props.onDelete}>Delete</Button>,
      <StageForm key={`edit-stage-${stage.stage}`} pipeline={pipeline} stage={stage} save={onUpdate} />,
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

    const block = (
      <span>
        {description}
        <br />
        {throughput}
      </span>
    );
    let content;

    // We check if we have the rules details before trying to render them
    if (this.state.rules) {
      content = this._formatRules(stage, stage.rules.map((name) => this.state.rules.filter((r) => r.title === name)[0]));
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

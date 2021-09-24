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

import { LinkContainer, Link } from 'components/graylog/router';
import connect from 'stores/connect';
import { Button, ButtonToolbar } from 'components/graylog';
import { DataTable, Timestamp } from 'components/common';
import { MetricContainer, CounterRate } from 'components/metrics';
import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';
import { RuleType, MetricsConfigType, RulesStoreState } from 'stores/rules/RulesStore';
import { Store } from 'stores/StoreTypes';

import RuleMetricsConfigContainer from './RuleMetricsConfigContainer';

const { RulesActions, RulesStore } = CombinedProvider.get('Rules');

type Props = {
  rules: Array<RuleType>,
  metricsConfig?: MetricsConfigType,
  onDelete: (RuleType) => void,
  searchFilter: React.ReactNode,
};

type State = {
  openMetricsConfig: boolean,
};

class RuleList extends React.Component<Props, State> {
  static propTypes = {
    rules: PropTypes.array.isRequired,
    metricsConfig: PropTypes.exact({
      metrics_enabled: PropTypes.bool.isRequired,
    }),
    onDelete: PropTypes.func.isRequired,
    searchFilter: PropTypes.node.isRequired,
  };

  static defaultProps = {
    metricsConfig: undefined,
  };

  constructor(props) {
    super(props);

    this.state = {
      openMetricsConfig: false,
    };
  }

  componentDidMount() {
    RulesActions.loadMetricsConfig();
  }

  _headerCellFormatter = (header) => {
    return <th>{header}</th>;
  };

  _ruleInfoFormatter = (rule) => {
    const { onDelete } = this.props;

    const actions = [
      <Button key="delete" bsStyle="primary" bsSize="xsmall" onClick={onDelete(rule)} title="Delete rule">
        Delete
      </Button>,
      <span key="space">&nbsp;</span>,
      <LinkContainer key="edit" to={Routes.SYSTEM.PIPELINES.RULE(rule.id)}>
        <Button bsStyle="info" bsSize="xsmall">Edit</Button>
      </LinkContainer>,
    ];

    return (
      <tr key={rule.title}>
        <td>
          <Link to={Routes.SYSTEM.PIPELINES.RULE(rule.id)}>
            {rule.title}
          </Link>
        </td>
        <td className="limited">{rule.description}</td>
        <td className="limited"><Timestamp dateTime={rule.created_at} relative /></td>
        <td className="limited"><Timestamp dateTime={rule.modified_at} relative /></td>
        <td>
          <MetricContainer name={`org.graylog.plugins.pipelineprocessor.ast.Rule.${rule.id}.executed`} zeroOnMissing>
            <CounterRate suffix="msg/s" />
          </MetricContainer>
        </td>
        <td>
          <MetricContainer name={`org.graylog.plugins.pipelineprocessor.ast.Rule.${rule.id}.failed`}>
            <CounterRate showTotal suffix="errors/s" hideOnMissing />
          </MetricContainer>
        </td>
        <td className="actions">{actions}</td>
      </tr>
    );
  };

  toggleMetricsConfig = () => {
    const { openMetricsConfig } = this.state;

    this.setState({ openMetricsConfig: !openMetricsConfig });
  };

  renderDebugMetricsButton = (metricsConfig) => {
    if (metricsConfig && metricsConfig.metrics_enabled) {
      return <Button bsStyle="warning" onClick={this.toggleMetricsConfig}>Debug Metrics: ON</Button>;
    }

    return <Button onClick={this.toggleMetricsConfig}>Debug Metrics</Button>;
  };

  render() {
    const { rules, metricsConfig, searchFilter } = this.props;
    const headers = ['Title', 'Description', 'Created', 'Last modified', 'Throughput', 'Errors', 'Actions'];
    const { openMetricsConfig } = this.state;

    return (
      <div>
        <DataTable id="rule-list"
                   className="table-hover"
                   headers={headers}
                   headerCellFormatter={this._headerCellFormatter}
                   sortByKey="title"
                   rows={rules}
                   customFilter={searchFilter}
                   dataRowFormatter={this._ruleInfoFormatter}
                   filterKeys={[]}>
          <ButtonToolbar className="pull-right">
            <LinkContainer to={Routes.SYSTEM.PIPELINES.RULE('new')}>
              <Button bsStyle="success">Create Rule</Button>
            </LinkContainer>
            {this.renderDebugMetricsButton(metricsConfig)}
          </ButtonToolbar>
        </DataTable>
        {openMetricsConfig && <RuleMetricsConfigContainer onClose={this.toggleMetricsConfig} />}
      </div>
    );
  }
}

export default connect(RuleList, { rules: RulesStore as Store<RulesStoreState> }, ({ rules }) => {
  return { metricsConfig: rules.metricsConfig };
});

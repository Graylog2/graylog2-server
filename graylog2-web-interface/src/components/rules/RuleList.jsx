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
import { RulesActions, RulesStore } from 'stores/rules/RulesStore';

import RuleMetricsConfigContainer from './RuleMetricsConfigContainer';
import RuleListEntry from './RuleListEntry';

class RuleList extends React.Component {
  static propTypes = {
    rules: PropTypes.array.isRequired,
    metricsConfig: PropTypes.object,
  };

  static defaultProps = {
    metricsConfig: undefined,
  };

  state = {
    openMetricsConfig: false,
  };

  componentDidMount() {
    RulesActions.loadMetricsConfig();
  }

  _delete = (rule) => {
    return () => {
      if (window.confirm(`Do you really want to delete rule "${rule.title}"?`)) {
        RulesActions.delete(rule);
      }
    };
  };

  _headerCellFormatter = (header) => {
    return <th>{header}</th>;
  };

  _ruleInfoFormatter = (rule) => {
    return <RuleListEntry rule={rule} onDelete={this._delete} />;
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
    const { rules, metricsConfig } = this.props;
    const filterKeys = ['title', 'description'];
    const headers = ['Title', 'Description', 'Created', 'Last modified', 'Throughput', 'Errors', 'Pipelines', 'Actions'];
    const { openMetricsConfig } = this.state;

    return (
      <div>
        <DataTable id="rule-list"
                   className="table-hover"
                   headers={headers}
                   headerCellFormatter={this._headerCellFormatter}
                   sortByKey="title"
                   rows={rules}
                   filterBy="Title"
                   dataRowFormatter={this._ruleInfoFormatter}
                   filterLabel="Filter Rules"
                   filterKeys={filterKeys}>
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

export default connect(RuleList, { rules: RulesStore }, ({ rules }) => {
  return { metricsConfig: rules ? rules.metricsConfig : rules };
});

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

import { LinkContainer, Link } from 'components/common/router';
import connect from 'stores/connect';
import { Button, ButtonToolbar } from 'components/bootstrap';
import { DataTable } from 'components/common';
import Routes from 'routing/Routes';
import { RulesActions, RulesStore } from 'stores/rules/RulesStore';
import type { RuleType, MetricsConfigType, RulesStoreState } from 'stores/rules/RulesStore';
import { Store } from 'stores/StoreTypes';

import RuleMetricsConfigContainer from './RuleMetricsConfigContainer';
import RuleListEntry from './RuleListEntry';

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

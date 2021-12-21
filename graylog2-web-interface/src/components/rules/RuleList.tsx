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
import React from 'react';
import PropTypes from 'prop-types';

import { DataTable } from 'components/common';
import type { RuleType, MetricsConfigType, RulesContext } from 'stores/rules/RulesStore';

import RuleListEntry from './RuleListEntry';

type Props = {
  rules: Array<RuleType>,
  metricsConfig?: MetricsConfigType,
  rulesContext?: RulesContext,
  onDelete: (RuleType) => () => void,
  searchFilter: React.ReactNode,
};

type State = {
  openMetricsConfig: boolean,
};

class RuleList extends React.Component<Props, State> {
  static propTypes = {
    rules: PropTypes.array.isRequired,

    rulesContext: PropTypes.exact({
      used_in_pipelines: PropTypes.objectOf(PropTypes.any),
    }),
    onDelete: PropTypes.func.isRequired,
    searchFilter: PropTypes.node.isRequired,
  };

  static defaultProps = {
    rulesContext: undefined,
  };

  _headerCellFormatter = (header) => {
    return <th>{header}</th>;
  };

  _ruleInfoFormatter = (rule) => {
    const { onDelete, rulesContext: { used_in_pipelines: usingPipelines } = {} } = this.props;

    return <RuleListEntry key={rule.id} rule={rule} usingPipelines={usingPipelines[rule.id]} onDelete={onDelete} />;
  };

  render() {
    const { rules, searchFilter } = this.props;
    const headers = ['Title', 'Description', 'Created', 'Last modified', 'Throughput', 'Errors', 'Pipelines', 'Actions'];

    return (
      <DataTable id="rule-list"
                 className="table-hover"
                 headers={headers}
                 headerCellFormatter={this._headerCellFormatter}
                 sortByKey="title"
                 rows={rules}
                 customFilter={searchFilter}
                 dataRowFormatter={this._ruleInfoFormatter}
                 filterKeys={[]} />
    );
  }
}

export default RuleList;

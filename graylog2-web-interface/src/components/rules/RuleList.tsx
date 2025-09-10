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

import { DataTable } from 'components/common';
import type { RuleType, RulesContext } from 'stores/rules/RulesStore';

import RuleListEntry from './RuleListEntry';

const headers = [
  'Title',
  '',
  'Description',
  'Created',
  'Last modified',
  'Throughput',
  'Errors',
  'Pipelines',
  'Actions',
];

const headerCellFormatter = (header: React.ReactNode) => <th>{header}</th>;

type Props = {
  rules: Array<RuleType>;
  rulesContext?: RulesContext;
  onDelete: (ruleType: RuleType) => () => void;
  searchFilter: React.ReactNode;
};

const RuleList = ({ rules, searchFilter, onDelete, rulesContext = undefined }: Props) => {
  const ruleInfoFormatter = (rule) => (
    <RuleListEntry
      key={rule.id}
      rule={rule}
      usingPipelines={rulesContext?.used_in_pipelines[rule.id]}
      onDelete={onDelete}
    />
  );

  return (
    <DataTable
      id="rule-list"
      className="table-hover"
      headers={headers}
      headerCellFormatter={headerCellFormatter}
      sortByKey="title"
      rows={rules}
      customFilter={searchFilter}
      dataRowFormatter={ruleInfoFormatter}
      filterKeys={[]}
    />
  );
};

export default RuleList;

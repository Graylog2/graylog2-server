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
import { useProcessingLoadContext, type ProcessingLoadResponse } from 'components/pipelines/processing-load';

import RuleListEntry from './RuleListEntry';

const headerCellFormatter = (header: React.ReactNode) => <th>{header}</th>;

type Props = {
  rules: Array<RuleType>;
  rulesContext?: RulesContext;
  onDelete: (ruleType: RuleType) => () => void;
  searchFilter: React.ReactNode;
  showLoadColumn?: boolean;
  processingLoad?: ProcessingLoadResponse;
  processingLoadError?: boolean;
};

const RuleList = ({
  rules,
  searchFilter,
  onDelete,
  rulesContext = undefined,
  showLoadColumn: showLoadColumnProp = undefined,
  processingLoad: processingLoadProp = undefined,
  processingLoadError: processingLoadErrorProp = undefined,
}: Props) => {
  const {
    metricsEnabled,
    processingLoad: processingLoadContext,
    processingLoadError: processingLoadErrorContext,
  } = useProcessingLoadContext();
  const showLoadColumn = showLoadColumnProp ?? metricsEnabled;
  const processingLoad = processingLoadProp ?? processingLoadContext;
  const processingLoadError = processingLoadErrorProp ?? processingLoadErrorContext;
  const headers = [
    'Title',
    'Description',
    'Created',
    'Last modified',
    'Throughput',
    'Errors',
    ...(showLoadColumn ? ['Pipeline Load (15m)'] : []),
    'Pipelines',
    'Actions',
  ];

  const ruleInfoFormatter = (rule) => (
    <RuleListEntry
      key={rule.id}
      rule={rule}
      usingPipelines={rulesContext?.used_in_pipelines[rule.id]}
      onDelete={onDelete}
      showLoadColumn={showLoadColumn}
      processingLoad={processingLoad}
      processingLoadError={processingLoadError}
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

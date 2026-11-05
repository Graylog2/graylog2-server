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
import { useTranslation } from 'react-i18next';

import { DataTable } from 'components/common';
import type { RuleType, RulesContext } from 'components/rules/hooks/useRules';
import { useProcessingLoadContext, type ProcessingLoadResponse } from 'components/pipelines/processing-load';

import RuleListEntry from './RuleListEntry';

// The English header texts identify the columns, the displayed texts are translated.
const HEADER_KEYS = {
  Title: 'rule_list.headers.title',
  Description: 'rule_list.headers.description',
  Created: 'rule_list.headers.created',
  'Last modified': 'rule_list.headers.last_modified',
  Throughput: 'rule_list.headers.throughput',
  Errors: 'rule_list.headers.errors',
  'Pipeline Load (15m)': 'rule_list.headers.pipeline_load',
  Pipelines: 'rule_list.headers.pipelines',
  Actions: 'rule_list.headers.actions',
};

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
  const { t } = useTranslation('pipelines');
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

  const headerCellFormatter = (header: string) => <th>{t(HEADER_KEYS[header] ?? header)}</th>;

  const ruleInfoFormatter = (rule: RuleType) => (
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

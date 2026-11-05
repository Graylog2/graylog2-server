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
import * as React from 'react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import styled, { css } from 'styled-components';
import * as Immutable from 'immutable';

import type { Stream } from 'logic/streams/types';
import { DEFAULT_PAGINATION } from 'stores/PaginationTypes';
import type { StreamOutputFilterRule } from 'components/streams/StreamDetails/output-filter/Types';
import i18n from 'i18n';
import { DataTable, NoSearchResult, PaginatedList, Spinner, Text } from 'components/common';
import { DEFAULT_PAGE_SIZES } from 'hooks/usePaginationQueryParameter';
import useStreamOutputFilters from 'components/streams/hooks/useStreamOutputFilters';
import FilterStatusCell from 'components/streams/StreamDetails/output-filter/FilterStatusCell';

const StyledText = styled(Text)(
  ({ theme }) => css`
    color: ${theme.colors.gray[50]};
  `,
);

const destinationTitle = (destinationType: string) => {
  if (destinationType === 'indexer') {
    return i18n.t('streams_overview.expanded.destination_index_set', { ns: 'streams' });
  }

  if (destinationType === 'data-lake') {
    return i18n.t('streams_overview.expanded.destination_data_lake', { ns: 'streams' });
  }

  return i18n.t('streams_overview.expanded.destination_output', { ns: 'streams' });
};

const _headerCellFormatter = (header: string) => <th>{header}</th>;

const filterRuleItem = (filter: StreamOutputFilterRule) => (
  <tr key={filter.id}>
    <td>
      {filter.title}
      <StyledText>{filter.description}</StyledText>
    </td>
    <td>{destinationTitle(filter.destination_type)}</td>
    <td>
      <FilterStatusCell filterOutputRule={filter} />
    </td>
  </tr>
);

type Props = {
  stream: Stream;
};

const ExpandedDestinationFilterRulesSection = ({ stream }: Props) => {
  const { t } = useTranslation('streams');
  const tableHeaders = [
    t('streams_overview.expanded.filter_rules_header_title'),
    t('streams_overview.expanded.filter_rules_header_destination'),
    t('streams_overview.expanded.filter_rules_header_status'),
  ];
  const [pagination, setPagination] = useState(DEFAULT_PAGINATION);
  const { data: paginatedFilters, isLoading } = useStreamOutputFilters(stream.id, undefined, pagination);

  const onPaginationChange = (newPage: number, newPerPage: number) =>
    setPagination((currentPagination) => ({
      ...currentPagination,
      page: newPage,
      perPage: newPerPage,
    }));

  if (isLoading && !paginatedFilters) {
    return <Spinner />;
  }

  const filters = paginatedFilters?.list ?? Immutable.List<StreamOutputFilterRule>();
  const total = paginatedFilters?.pagination?.total ?? 0;

  return (
    <>
      <p>{t('streams_overview.expanded.filter_rules_summary', { count: total })}</p>
      <PaginatedList
        totalItems={total}
        pageSize={DEFAULT_PAGE_SIZES[0]}
        onChange={onPaginationChange}
        useQueryParameter={false}
        showPageSizeSelect={false}>
        <DataTable
          id="stream-filter-rule-list"
          className="striped"
          rowClassName="no-bm"
          headers={tableHeaders}
          headerCellFormatter={_headerCellFormatter}
          sortByKey="title"
          noDataText={<NoSearchResult>{t('streams_overview.expanded.no_filter_rules')}</NoSearchResult>}
          rows={filters.toJS()}
          dataRowFormatter={filterRuleItem}
        />
      </PaginatedList>
    </>
  );
};

export default ExpandedDestinationFilterRulesSection;

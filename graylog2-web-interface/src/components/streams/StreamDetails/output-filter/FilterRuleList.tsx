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
import styled, { css } from 'styled-components';

import SectionComponent from 'components/common/Section/SectionComponent';
import { IfPermitted, DataTable, PaginatedList, NoSearchResult } from 'components/common';
import type { StreamOutputFilterRule } from 'components/streams/StreamDetails/output-filter/Types';
import FilterRuleEditButton from 'components/streams/StreamDetails/output-filter/FilterRuleEditButton';
import { Alert } from 'components/bootstrap';
import type { PaginatedList as PaginatedListType } from 'stores/PaginationTypes';

import FilterStatusCell from './FilterStatusCell';
import FilterActions from './FilterActions';

const TABLE_HEADERS = ['Title', 'Status', ''];

export const StyledSectionComponent = styled(SectionComponent)(({ theme }) => css`
  margin-bottom: ${theme.spacings.xs};
  &.content {
    background-color: ${theme.colors.table.row.backgroundHover};
    padding: ${theme.spacings.sm} ${theme.spacings.xxs};
  }
  &.row {
    margin: 0  ${theme.spacings.xxs} ${theme.spacings.sm};
  }
  h2 {
    font-size: ${theme.fonts.size.h3};
  }
  .table > tbody > tr {
    background-color: transparent;
   }
  .table > thead > tr > th {
    border-bottom-color: ${theme.utils.colorLevel(theme.colors.variant.default, -5)};
    border-bottom-width: 1px;
  }
`);

type Props = {
  streamId: string,
  destinationType: string,
  paginatedFilters: PaginatedListType<StreamOutputFilterRule>,
};
const _headerCellFormatter = (header: string) => (<th>{header}</th>);
const buildFilterItem = (destinationType: string) => (filter: StreamOutputFilterRule) => (
  <tr key={filter.id}>
    <td>{filter.title}
      {filter.description}
    </td>
    <td><FilterStatusCell filterOutputRule={filter} /></td>
    <td><FilterActions filterRule={filter} destinationType={destinationType} /></td>
  </tr>
);

const FilterRulesList = ({ streamId, destinationType, paginatedFilters }: Props) => {
  const { list: filters, pagination: { total } } = paginatedFilters;

  return (
    <StyledSectionComponent title="Filter Rules"
                            headerActions={(
                              <IfPermitted permissions="">
                                <FilterRuleEditButton filterRule={{ stream_id: streamId }}
                                                      destinationType={destinationType}
                                                      streamId={streamId} />
                              </IfPermitted>
             )}>
      <Alert bsStyle="default">
        Messages which meet the criteria of the following filter rule(s) will not be routed to the  {destinationType === 'indexer' ? 'Index Set' : 'Data Warehouse'}.
      </Alert>
      <PaginatedList totalItems={total}>
        <DataTable id="filter-list"
                   className=""
                   rowClassName="no-bm"
                   headers={TABLE_HEADERS}
                   headerCellFormatter={_headerCellFormatter}
                   sortByKey="title"
                   noDataText={<NoSearchResult>No filter have been found.</NoSearchResult>}
                   rows={filters.toJS()}
                   dataRowFormatter={buildFilterItem(destinationType)} />
      </PaginatedList>
    </StyledSectionComponent>
  );
};

export default FilterRulesList;

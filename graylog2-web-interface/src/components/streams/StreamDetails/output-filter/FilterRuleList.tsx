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
import { useCallback } from 'react';
import styled, { css } from 'styled-components';

import type { ColumnRenderers } from 'components/common/EntityDataTable/types';
import type { SearchParams } from 'stores/PaginationTypes';
import SectionComponent from 'components/common/Section/SectionComponent';
import { IfPermitted, PaginatedEntityTable, QueryHelper } from 'components/common';
import { ADDITIONAL_ATTRIBUTES, COLUMNS_ORDER, DEFAULT_LAYOUT } from 'components/streams/StreamDetails/output-filter/Constants';
import useTableElements from 'components/streams/StreamDetails/output-filter/hooks/useTableComponents';
import type { StreamOutputFilterRule } from 'components/streams/StreamDetails/output-filter/Types';
import FilterRuleEditButton from 'components/streams/StreamDetails/output-filter/FilterRuleEditButton';
import { keyFn, fetchStreamOutputFilters } from 'components/streams/hooks/useStreamOutputFilters';
import { Alert } from 'components/bootstrap';

import FilterStatusCell from './FilterStatusCell';

export const StyledSectionComponent = styled(SectionComponent)(({ theme }) => css`
  &.content {
    background-color: ${theme.colors.variant.dark.gray};
    padding: ${theme.spacings.sm} ${theme.spacings.xs};
  }
  &.row {
    margin: 0 ${theme.spacings.xs};
  }
  h2 {
    font-size: ${theme.fonts.size.h3};
  }
`);

type Props = {
  streamId: string,
  destinationType: string,
};

const FilterRulesList = ({ streamId, destinationType }: Props) => {
  const { entityActions } = useTableElements(destinationType);
  const _keyFn = useCallback((searchParams: SearchParams) => keyFn(streamId, destinationType, searchParams), [streamId, destinationType]);
  const _fetchStreamOutputFilters = useCallback((searchParams: SearchParams) => fetchStreamOutputFilters(streamId, { ...searchParams, query: `destination_type:${destinationType}` }), [streamId, destinationType]);

  const customColumnRenderers = (): ColumnRenderers<StreamOutputFilterRule> => ({
    attributes: {
      status: {
        renderCell: (_title: string, filterOutputRule) => <FilterStatusCell filterOutputRule={filterOutputRule} />,
      },
    },
  });

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
      <PaginatedEntityTable<StreamOutputFilterRule> humanName="filter"
                                                    columnsOrder={COLUMNS_ORDER}
                                                    additionalAttributes={ADDITIONAL_ATTRIBUTES}
                                                    queryHelpComponent={<QueryHelper entityName="streamOutputFilters" />}
                                                    entityActions={entityActions}
                                                    tableLayout={DEFAULT_LAYOUT}
                                                    fetchEntities={_fetchStreamOutputFilters}
                                                    columnRenderers={customColumnRenderers()}
                                                    keyFn={_keyFn}
                                                    entityAttributesAreCamelCase={false} />

    </StyledSectionComponent>
  );
};

export default FilterRulesList;

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
import styled from 'styled-components';

import SectionComponent from 'components/common/Section/SectionComponent';
import { PaginatedEntityTable, QueryHelper } from 'components/common';

import { ADDITIONAL_ATTRIBUTES, COLUMNS_ORDER, DEFAULT_LAYOUT } from './Constants';
import useTableElements from './hooks/useTableComponents';

import { keyFn, fetchStreamOutputFilters } from '../../hooks/useStreamOutputFilters';

export const StyledSectionComponent = styled(SectionComponent)`
  &.content {
    background-color: transparent;
  }
`;

type Props = {
  streamId: string,
};

const FilterRulesList = ({ streamId }: Props) => {
  const { entityActions } = useTableElements();

  return (
    <StyledSectionComponent title="Filter Rule">
      <PaginatedEntityTable<any> humanName="filter"
                                 columnsOrder={COLUMNS_ORDER}
                                 additionalAttributes={ADDITIONAL_ATTRIBUTES}
                                 queryHelpComponent={<QueryHelper entityName="streamOutputFilters" />}
                                 entityActions={entityActions}
                                 tableLayout={DEFAULT_LAYOUT}
                                 fetchEntities={(searchParams) => fetchStreamOutputFilters(streamId, searchParams)}
                                 columnRenderers={{}}
                                 keyFn={keyFn}
                                 entityAttributesAreCamelCase={false} />

    </StyledSectionComponent>
  );
};

export default FilterRulesList;

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
import styled, { css } from 'styled-components';

import { PaginatedEntityTable } from 'components/common';

import { fetchIncompatibleIndices, incompatibleIndicesKeyFn } from './fetchIncompatibleIndices';
import type { IncompatibleIndexRow } from './fetchIncompatibleIndices';
import { createColumnRenderers, DEFAULT_DISPLAYED_COLUMNS } from './IncompatibleIndicesColumnRenderers';
import IncompatibleIndexTableActions from './IncompatibleIndexTableActions';
import IncompatibleIndicesBulkActions from './IncompatibleIndicesBulkActions';
import IncompatibleIndicesContext from './IncompatibleIndicesContext';
import useIncompatibleIndexActionState from './hooks/useIncompatibleIndexActionState';
import useTrackedIncompatibleIndices from './hooks/useTrackedIncompatibleIndices';

const Heading = styled.h4(
  ({ theme }) => css`
    margin-top: ${theme.spacings.md};
    margin-bottom: ${theme.spacings.sm};
  `,
);

const TABLE_LAYOUT = {
  entityTableId: 'incompatible_indices',
  defaultSort: { attributeId: 'index_name', direction: 'asc' as const },
  defaultDisplayedAttributes: DEFAULT_DISPLAYED_COLUMNS,
  defaultPageSize: 10,
  defaultColumnOrder: ['index_name', 'category', 'version', 'begin', 'end'],
};

const IncompatibleIndicesTable = () => {
  const { selectedIndices, trackedIndices, hasLoaded, onDataLoaded, onChangeSelection } =
    useTrackedIncompatibleIndices();
  const contextValue = useIncompatibleIndexActionState({ trackedIndices, isLoading: !hasLoaded });
  const columnRenderers = createColumnRenderers();
  const renderActions = (index: IncompatibleIndexRow) => <IncompatibleIndexTableActions index={index} />;

  return (
    <IncompatibleIndicesContext.Provider value={contextValue}>
      <Heading>Incompatible indices</Heading>
      <PaginatedEntityTable<IncompatibleIndexRow>
        humanName="incompatible indices"
        tableLayout={TABLE_LAYOUT}
        fetchEntities={fetchIncompatibleIndices}
        keyFn={incompatibleIndicesKeyFn}
        columnRenderers={columnRenderers}
        entityActions={renderActions}
        bulkSelection={{ onChangeSelection, actions: <IncompatibleIndicesBulkActions indices={selectedIndices} /> }}
        onDataLoaded={onDataLoaded}
        entityAttributesAreCamelCase={false}
      />
    </IncompatibleIndicesContext.Provider>
  );
};

export default IncompatibleIndicesTable;

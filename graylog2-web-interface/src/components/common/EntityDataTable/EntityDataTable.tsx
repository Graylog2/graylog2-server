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
import { useMemo, useState } from 'react';
import styled, { css } from 'styled-components';
import merge from 'lodash/merge';
import type { ColumnDef } from '@tanstack/react-table';
import { createColumnHelper } from '@tanstack/react-table';

import { Table, ButtonGroup } from 'components/bootstrap';
import { isPermitted, isAnyPermitted } from 'util/PermissionsMixin';
import useCurrentUser from 'hooks/useCurrentUser';
import ColumnsVisibilitySelect from 'components/common/EntityDataTable/ColumnsVisibilitySelect';
import DefaultColumnRenderers from 'components/common/EntityDataTable/DefaultColumnRenderers';
import { CELL_PADDING } from 'components/common/EntityDataTable/Constants';
import type { Sort } from 'stores/PaginationTypes';
import { PageSizeSelect } from 'components/common';
import SelectedEntitiesProvider from 'components/common/EntityDataTable/contexts/SelectedEntitiesProvider';
import MetaDataProvider from 'components/common/EntityDataTable/contexts/MetaDataProvider';
import ExpandedSections from 'components/common/EntityDataTable/ExpandedSections';
import useTable from 'components/common/EntityDataTable/hooks/useTable';
import useElementWidths from 'components/common/EntityDataTable/hooks/useElementWidths';
import useVisibleColumnOrder from 'components/common/EntityDataTable/hooks/useVisibleColumnOrder';
import useBulkSelectColumnDefinition from 'components/common/EntityDataTable/hooks/useBulkSelectColumnDefinition';
import useActionsColumnDefinition from 'components/common/EntityDataTable/hooks/useActionsColumnDefinition';
import useAttributeColumnDefinitions from 'components/common/EntityDataTable/hooks/useAttributeColumnDefinitions';
import TableDndProvider from 'components/common/EntityDataTable/TableDndProvider';
import TableCell from 'components/common/EntityDataTable/TableCell';

import type {
  ColumnRenderers,
  ColumnSchema,
  EntityBase,
  ExpandedSectionRenderer,
  ColumnRenderersByAttribute,
  ColumnPreferences,
} from './types';
import ExpandedSectionsProvider from './contexts/ExpandedSectionsProvider';
import TableHead from './TableHead';
import BulkActionsRow from './BulkActionsRow';

const ScrollContainer = styled.div`
  width: 100%;
  overflow-x: auto;
`;

const StyledTable = styled(Table)(
  ({ theme }) => css`
    table-layout: fixed;

    thead > tr > th,
    tbody > tr > td {
      padding: ${CELL_PADDING}px;
    }

    && {
      > tbody:nth-of-type(even) > tr {
        background-color: ${theme.colors.table.row.backgroundStriped};
      }

      > tbody:nth-of-type(odd) > tr {
        background-color: ${theme.colors.table.row.background};
      }
    }
  `,
);

const ActionsRow = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  min-height: 22px;
`;

const LayoutConfigRow = styled.div`
  display: flex;
  align-items: center;
  gap: 5px;
`;

const useAuthorizedColumnSchemas = (columnSchemas: Array<ColumnSchema>) => {
  const currentUser = useCurrentUser();

  return useMemo(
    () =>
      columnSchemas.filter(({ permissions, anyPermissions, hidden }) => {
        if (hidden) {
          return false;
        }

        if (permissions?.length) {
          return anyPermissions
            ? isAnyPermitted(currentUser.permissions, permissions)
            : isPermitted(currentUser.permissions, permissions);
        }

        return true;
      }),
    [columnSchemas, currentUser.permissions],
  );
};

const useColumnRenderers = <Entity extends EntityBase, Meta = unknown>(
  columnSchemas: Array<ColumnSchema>,
  customColumnRenderers: ColumnRenderers<Entity, Meta>,
) =>
  useMemo(() => {
    const renderers = merge({}, DefaultColumnRenderers, customColumnRenderers);

    return Object.fromEntries(
      columnSchemas.map(({ id, type }) => {
        const typeRenderer = renderers.types?.[type];
        const attributeRenderer = renderers.attributes?.[id];

        const columnRenderer = merge({}, typeRenderer, attributeRenderer);

        return [id, columnRenderer];
      }),
    );
  }, [columnSchemas, customColumnRenderers]);

const useColumnDefinitions = <Entity extends EntityBase, Meta>({
  actionsColWidth,
  actionsRef,
  columnRenderersByAttribute,
  columnSchemas,
  columnWidths,
  displayActionsCol,
  displayBulkSelectCol,
  entityActions,
  entityAttributesAreCamelCase,
  meta,
}: {
  actionsColWidth: number;
  actionsRef: React.MutableRefObject<HTMLDivElement>;
  columnRenderersByAttribute: ColumnRenderersByAttribute<Entity, Meta>;
  columnSchemas: Array<ColumnSchema>;
  columnWidths: { [attributeId: string]: number };
  displayActionsCol: boolean;
  displayBulkSelectCol: boolean;
  entityActions?: (entity: Entity) => React.ReactNode;
  entityAttributesAreCamelCase: boolean;
  meta: Meta;
}) => {
  const columnHelper = createColumnHelper<Entity>();
  const bulkSelectCol = useBulkSelectColumnDefinition(displayBulkSelectCol);
  const actionsCol = useActionsColumnDefinition(displayActionsCol, actionsColWidth, entityActions, actionsRef);
  const attributeCols = useAttributeColumnDefinitions<Entity, Meta>({
    columnSchemas,
    columnRenderersByAttribute,
    columnWidths,
    entityAttributesAreCamelCase,
    meta,
    columnHelper,
  });

  return useMemo(
    () =>
      [...(bulkSelectCol ? [bulkSelectCol] : []), ...attributeCols, ...(actionsCol ? [actionsCol] : [])] as Array<
        ColumnDef<Entity, unknown>
      >,
    [bulkSelectCol, attributeCols, actionsCol],
  );
};

type Props<Entity extends EntityBase, Meta = unknown> = {
  /**
   * Needs to be defined when not all action cells in every row have the same width.
   * When they have the same width, the column width will be calculated automatically.
   * Should not include the actions col padding. Should be the max width an action cell can have.
   */
  actionsCellWidth?: number;
  /** Currently active sort */
  activeSort?: Sort;
  /**
   * The column ids are always snake case. By default, entity attributes are camel case.
   * This prop controls if the column ids need to be transformed to camel case to connect them with the entity attributes.
   */
  entityAttributesAreCamelCase: boolean;
  bulkSelection?: {
    /** Supported bulk actions */
    actions?: React.ReactNode;
    /** Callback which runs on selection change */
    onChangeSelection?: (selectedEntities: Array<Entity['id']>, data: Readonly<Array<Entity>>) => void;
    /** Initial selected items */
    initialSelection?: Array<Entity['id']>;
    isEntitySelectable?: (entity: Entity) => boolean;
  };
  /** List of all available columns. Column ids need to be snake case. */
  columnSchemas: Array<ColumnSchema>;
  /** Custom cell and header renderer for a column. Column ids need to be snake case. */
  columnRenderers?: ColumnRenderers<Entity, Meta>;
  /** Define default columns order. Column ids need to be snake case. */
  columnsOrder?: Array<string>;
  /** The table data. */
  entities: ReadonlyArray<Entity>;
  /** Allows you to extend a row with additional information * */
  expandedSectionsRenderer?: {
    [sectionName: string]: ExpandedSectionRenderer<Entity>;
  };
  /** Function to handle changes of columns visibility */
  onColumnPreferencesChange: (newColumnPreferences: ColumnPreferences) => void;
  /** Function to handle sort changes */
  onSortChange: (newSort: Sort) => void;
  /** Function to handle page size changes */
  onPageSizeChange?: (newPageSize: number) => void;
  /** Active page size */
  pageSize?: number;
  /** Actions for each row. */
  entityActions?: (entity: Entity) => React.ReactNode;
  /** Which columns should be displayed. */
  columnPreferences?: ColumnPreferences;
  /** Meta data. */
  meta?: Meta;
};

/**
 * Flexible data table component which allows defining custom column renderers.
 */
const EntityDataTable = <Entity extends EntityBase, Meta = unknown>({
  actionsCellWidth: fixedActionsCellWidth = undefined,
  activeSort = undefined,
  entityAttributesAreCamelCase,
  bulkSelection: { actions, onChangeSelection, initialSelection, isEntitySelectable } = {},
  columnSchemas,
  columnRenderers: customColumnRenderers = undefined,
  columnsOrder: attributeColumnsOder = [],
  entities,
  expandedSectionsRenderer = undefined,
  onColumnPreferencesChange,
  onPageSizeChange = undefined,
  onSortChange,
  pageSize = undefined,
  entityActions = undefined,
  columnPreferences = {},
  meta = undefined,
}: Props<Entity, Meta>) => {
  const [selectedEntities, setSelectedEntities] = useState<Array<Entity['id']>>(initialSelection ?? []);

  const displayActionsCol = typeof entityActions === 'function';
  const displayBulkAction = !!actions;
  const displayBulkSelectCol = typeof onChangeSelection === 'function' || displayBulkAction;
  const displayPageSizeSelect = typeof onPageSizeChange === 'function';
  const authorizedColumnSchemas = useAuthorizedColumnSchemas(columnSchemas);
  const columnRenderersByAttribute = useColumnRenderers<Entity, Meta>(authorizedColumnSchemas, customColumnRenderers);

  const visibleColumnOrder = useVisibleColumnOrder(
    columnPreferences,
    attributeColumnsOder,
    displayActionsCol,
    displayBulkSelectCol,
  );

  const [columnOrder, setColumnOrder] = useState(visibleColumnOrder);

  const { tableRef, actionsRef, actionsColWidth, columnWidths } = useElementWidths<Entity, Meta>({
    columnRenderersByAttribute,
    columnSchemas: authorizedColumnSchemas,
    displayBulkSelectCol,
    fixedActionsCellWidth,
    visibleColumns: columnOrder,
  });

  const columnsDefinitions = useColumnDefinitions<Entity, Meta>({
    actionsRef,
    actionsColWidth,
    columnRenderersByAttribute,
    columnSchemas: authorizedColumnSchemas,
    columnWidths,
    displayActionsCol,
    displayBulkSelectCol,
    entityActions,
    entityAttributesAreCamelCase,
    meta,
  });

  const table = useTable<Entity>({
    columnsDefinitions,
    displayBulkSelectCol,
    entities,
    isEntitySelectable,
    onChangeSelection,
    onColumnPreferencesChange,
    onSortChange,
    selectedEntities,
    setSelectedEntities,
    sort: activeSort,
    visibleColumnOrder: columnOrder,
    onColumnOrderChange: setColumnOrder,
  });

  return (
    <MetaDataProvider<Meta> meta={meta}>
      <SelectedEntitiesProvider<Entity> table={table} selectedEntities={selectedEntities}>
        <ExpandedSectionsProvider>
          <ActionsRow>
            <div>{displayBulkAction && <BulkActionsRow bulkActions={actions} />}</div>
            <LayoutConfigRow>
              Show
              <ButtonGroup>
                {displayPageSizeSelect && (
                  <PageSizeSelect pageSize={pageSize} showLabel={false} onChange={onPageSizeChange} />
                )}
                <ColumnsVisibilitySelect<Entity> table={table} />
              </ButtonGroup>
            </LayoutConfigRow>
          </ActionsRow>
          <TableDndProvider columnOrder={columnOrder} onColumnOrderChange={setColumnOrder} table={table}>
            <ScrollContainer id="scroll-container" ref={tableRef}>
              <StyledTable striped condensed hover>
                <TableHead table={table} />
                {table.getRowModel().rows.map((row) => (
                  <tbody key={`table-row-${row.id}`} data-testid={`table-row-${row.id}`}>
                    <tr>
                      {row.getVisibleCells().map((cell) => (
                        <TableCell key={cell.id} cell={cell} />
                      ))}
                    </tr>
                    <ExpandedSections
                      key={`expanded-sections-${row.id}`}
                      expandedSectionsRenderer={expandedSectionsRenderer}
                      entity={row.original}
                    />
                  </tbody>
                ))}
              </StyledTable>
            </ScrollContainer>
          </TableDndProvider>
        </ExpandedSectionsProvider>
      </SelectedEntitiesProvider>
    </MetaDataProvider>
  );
};

export default EntityDataTable;

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
import { useMemo, useRef, useCallback } from 'react';
import styled, { css } from 'styled-components';
import type * as Immutable from 'immutable';
import merge from 'lodash/merge';
import { useReactTable, createColumnHelper, getCoreRowModel, flexRender } from '@tanstack/react-table';
import camelCase from 'lodash/camelCase';
import RowCheckbox from 'components/common/EntityDataTable/RowCheckbox';
import { Table, ButtonGroup } from 'components/bootstrap';
import { isPermitted, isAnyPermitted } from 'util/PermissionsMixin';
import useCurrentUser from 'hooks/useCurrentUser';
import ColumnsVisibilitySelect from 'components/common/EntityDataTable/ColumnsVisibilitySelect';
import DefaultColumnRenderers from 'components/common/EntityDataTable/DefaultColumnRenderers';
import { CELL_PADDING, BULK_SELECT_COLUMN_WIDTH } from 'components/common/EntityDataTable/Constants';
import useColumnsWidths from 'components/common/EntityDataTable/hooks/useColumnsWidths';
import useElementDimensions from 'hooks/useElementDimensions';
import type { Sort } from 'stores/PaginationTypes';
import { PageSizeSelect } from 'components/common';
import SelectedEntitiesProvider from 'components/common/EntityDataTable/contexts/SelectedEntitiesProvider';
import MetaDataProvider from 'components/common/EntityDataTable/contexts/MetaDataProvider';
import BulkActionsRow from './BulkActionsRow';
import TableHead from './TableHead';
import ExpandedSections from 'components/common/EntityDataTable/ExpandedSections';
import ExpandedSectionsProvider from './contexts/ExpandedSectionsProvider';
import type {
  ColumnRenderers,
  ColumnRenderer,
  Column,
  EntityBase,
  ColumnRenderersByAttribute,
  ExpandedSectionRenderer,
} from './types';
import BulkSelectHead from 'components/common/EntityDataTable/BulkSelectHead';
import ButtonToolbar from '../../bootstrap/ButtonToolbar';

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

const Td = styled.td`
  word-break: break-word;
`;

const filterAccessibleColumns = (columns: Array<Column>, userPermissions: Immutable.List<string>) =>
  columns.filter(({ permissions, anyPermissions, hidden }) => {
    if (hidden) {
      return false;
    }

    if (permissions?.length) {
      return anyPermissions ? isAnyPermitted(userPermissions, permissions) : isPermitted(userPermissions, permissions);
    }

    return true;
  });

// todo use tanstack query logic instead
const filterVisibleColumns = (columnDefinitions: Array<Column>, visibleColumns: Array<string>) =>
  visibleColumns.map((columnId) => columnDefinitions.find(({ id }) => id === columnId)).filter((column) => !!column);

const useElementsWidths = <Entity extends EntityBase, Meta>({
  columns,
  columnRenderersByAttribute,
  displayBulkSelectCol,
  fixedActionsCellWidth,
}: {
  columns: Array<Column>;
  columnRenderersByAttribute: ColumnRenderersByAttribute<Entity, Meta>;
  displayBulkSelectCol: boolean;
  fixedActionsCellWidth: number | undefined;
}) => {
  const tableRef = useRef<HTMLTableElement>(null);
  const actionsRef = useRef<HTMLDivElement>();
  const { width: tableWidth } = useElementDimensions(tableRef);
  const columnsIds = useMemo(() => columns.map(({ id }) => id), [columns]);
  const actionsColInnerWidth = fixedActionsCellWidth ?? actionsRef.current?.offsetWidth ?? 0;
  const actionsColWidth = actionsColInnerWidth ? actionsColInnerWidth + CELL_PADDING * 2 : 0;

  const columnsWidths = useColumnsWidths<Entity>({
    actionsColWidth,
    bulkSelectColWidth: displayBulkSelectCol ? BULK_SELECT_COLUMN_WIDTH : 0,
    columnRenderersByAttribute,
    columnsIds,
    tableWidth,
  });

  return { tableRef, actionsRef, columnsWidths, actionsColWidth };
};

const mergeColumnsRenderers = <Entity extends EntityBase, Meta = unknown>(
  columns: Array<Column>,
  customColumnRenderers: ColumnRenderers<Entity, Meta>,
) => {
  const renderers = merge({}, DefaultColumnRenderers, customColumnRenderers);

  return Object.fromEntries(
    columns.map(({ id, type }) => {
      const typeRenderer = renderers.types?.[type];
      const attributeRenderer = renderers.attributes?.[id];

      const columnRenderer = merge({}, typeRenderer, attributeRenderer);

      return [id, columnRenderer];
    }),
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
  columnDefinitions: Array<Column>;
  /** Custom cell and header renderer for a column. Column ids need to be snake case. */
  columnRenderers?: ColumnRenderers<Entity, Meta>;
  /** Define default columns order. Column ids need to be snake case. */
  columnsOrder?: Array<string>;
  /** The table data. */
  entities: Array<Entity>;
  /** Allows you to extend a row with additional information * */
  expandedSectionsRenderer?: {
    [sectionName: string]: ExpandedSectionRenderer<Entity>;
  };
  /** Function to handle changes of columns visibility */
  onColumnsChange: (columnIds: Array<string>) => void;
  /** Function to handle sort changes */
  onSortChange: (newSort: Sort) => void;
  /** Function to handle page size changes */
  onPageSizeChange?: (newPageSize: number) => void;
  /** Active page size */
  pageSize?: number;
  /** Actions for each row. */
  entityActions?: (entity: Entity) => React.ReactNode;
  /** Which columns should be displayed. */
  visibleColumns: Array<string>;
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
  columnDefinitions,
  columnRenderers: customColumnRenderers = undefined,
  columnsOrder = [],
  entities,
  expandedSectionsRenderer = undefined,
  onColumnsChange,
  onPageSizeChange = undefined,
  onSortChange,
  pageSize = undefined,
  entityActions = undefined,
  visibleColumns,
  meta = undefined,
}: Props<Entity, Meta>) => {
  const currentUser = useCurrentUser();
  const displayActionsCol = typeof entityActions === 'function';
  const displayBulkAction = !!actions;
  const displayBulkSelectCol = typeof onChangeSelection === 'function' || displayBulkAction;
  const displayPageSizeSelect = typeof onPageSizeChange === 'function';
  const _isEntitySelectable = useCallback(
    (entity: Entity) => {
      if (!displayBulkSelectCol) return false;

      if (typeof isEntitySelectable === 'function') return isEntitySelectable(entity);

      return true;
    },
    [displayBulkSelectCol, isEntitySelectable],
  );

  const accessibleColumns = useMemo(
    () => filterAccessibleColumns(columnDefinitions, currentUser.permissions),
    [columnDefinitions, currentUser.permissions],
  );

  const columns = useMemo(
    () => filterVisibleColumns(accessibleColumns, visibleColumns),
    [accessibleColumns, visibleColumns],
  );

  const columnRenderersByAttribute = useMemo(
    () => mergeColumnsRenderers<Entity, Meta>(columns, customColumnRenderers),
    [columns, customColumnRenderers],
  );

  const { tableRef, actionsRef, actionsColWidth, columnsWidths } = useElementsWidths<Entity, Meta>({
    columns,
    columnRenderersByAttribute,
    displayBulkSelectCol,
    fixedActionsCellWidth,
  });

  const selectableData = useMemo(() => entities.filter(_isEntitySelectable), [entities, _isEntitySelectable]);
  const columnHelper = createColumnHelper<Entity>();

  const _columns = useMemo(
    () => [
      ...(displayBulkSelectCol
        ? [
            columnHelper.display({
              id: 'bulk-select',
              size: BULK_SELECT_COLUMN_WIDTH,
              header: () => <BulkSelectHead data={entities} />,
              cell: ({ row }) => {
                const isSelected = row.getIsSelected();
                return (
                  <RowCheckbox
                    onChange={row.getToggleSelectedHandler()}
                    title={`${isSelected ? 'Deselect' : 'Select'} entity`}
                    checked={isSelected}
                    disabled={!row.getCanSelect()}
                    aria-label={row.id}
                  />
                );
              },
            }),
          ]
        : []),
      ...columns.map((col) =>
        columnHelper.accessor(col.id, {
          cell: (info) =>
            columnRenderersByAttribute[col.id].renderCell(info.cell.getValue(), info.row.original, col, meta),
          accessorKey: entityAttributesAreCamelCase ? camelCase(col.id) : col.id,
          header: (info) =>
            columnRenderersByAttribute[col.id]?.renderHeader
              ? columnRenderersByAttribute[col.id].renderHeader(info)
              : col.title,
          size: columnsWidths[col.id],
          enableSorting: col.sortable ?? false,
        }),
      ),
      ...(displayActionsCol
        ? [
            columnHelper.display({
              id: 'actions',
              size: actionsColWidth,
              header: 'Actions',
              cell: ({ row }) => <ButtonToolbar>{entityActions(row.original)}</ButtonToolbar>,
            }),
          ]
        : []),
    ],
    [columnHelper, columnRenderersByAttribute, columns, columnsWidths, entityAttributesAreCamelCase, meta],
  );

  const table = useReactTable({
    data: entities,
    columns: _columns,
    getCoreRowModel: getCoreRowModel(),
    manualSorting: true,
    enableSortingRemoval: false,
    initialState: {
      columnOrder: [...(displayBulkSelectCol ? ['bulk-select'] : []), ...columnsOrder],
    },
    state: {
      sorting: activeSort ? [{ id: activeSort.attributeId, desc: activeSort.direction === 'desc' }] : [],
    },
    onSortingChange: (newSortFn) => {
      const newSort = newSortFn();
      onSortChange({ attributeId: newSort[0].id, direction: newSort[0].desc ? 'desc' : 'asc' });
    },
  });

  return (
    <MetaDataProvider<Meta> meta={meta}>
      <SelectedEntitiesProvider<Entity>
        initialSelection={initialSelection}
        onChangeSelection={onChangeSelection}
        entities={entities}>
        <ExpandedSectionsProvider>
          <ActionsRow>
            <div>{displayBulkAction && <BulkActionsRow bulkActions={actions} />}</div>
            <LayoutConfigRow>
              Show
              <ButtonGroup>
                {displayPageSizeSelect && (
                  <PageSizeSelect pageSize={pageSize} showLabel={false} onChange={onPageSizeChange} />
                )}
                <ColumnsVisibilitySelect
                  allColumns={accessibleColumns}
                  selectedColumns={visibleColumns}
                  onChange={onColumnsChange}
                />
              </ButtonGroup>
            </LayoutConfigRow>
          </ActionsRow>
          <ScrollContainer id="scroll-container" ref={tableRef}>
            <StyledTable striped condensed hover>
              <TableHead table={table} />
              {table.getRowModel().rows.map((row) => (
                <tbody key={`table-row-${row.id}`} data-testid={`table-row-${row.id}`}>
                  <tr>
                    {row.getVisibleCells().map((cell) => (
                      <Td key={cell.id}>{flexRender(cell.column.columnDef.cell, cell.getContext())}</Td>
                    ))}
                  </tr>
                  <ExpandedSections
                    key={`expanded-sections-${row.id}`}
                    expandedSectionsRenderer={expandedSectionsRenderer}
                    row={row}
                  />
                </tbody>
              ))}
            </StyledTable>
          </ScrollContainer>
        </ExpandedSectionsProvider>
      </SelectedEntitiesProvider>
    </MetaDataProvider>
  );
};

export default EntityDataTable;

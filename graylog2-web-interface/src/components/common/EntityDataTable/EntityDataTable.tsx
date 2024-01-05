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
import { useMemo, useRef, useCallback } from 'react';
import type * as Immutable from 'immutable';
import merge from 'lodash/merge';

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
import ExpandedSections from 'components/common/EntityDataTable/ExpandedSections';
import SelectedEntitiesProvider from 'components/common/EntityDataTable/contexts/SelectedEntitiesProvider';

import BulkActionsRow from './BulkActionsRow';
import TableHead from './TableHead';
import TableRow from './TableRow';
import ExpandedSectionsProvider from './contexts/ExpandedSectionsProvider';
import type { ColumnRenderers, Column, EntityBase, ColumnRenderersByAttribute, ExpandedSectionRenderer } from './types';

const ScrollContainer = styled.div`
  width: 100%;
  overflow-x: auto;
`;

const StyledTable = styled(Table)(({ theme }) => css`
  table-layout: fixed;

  thead > tr > th,
  tbody > tr > td {
    padding: ${CELL_PADDING}px;
  }

  && {
    > tbody:nth-of-type(even) > tr {
      background-color: ${theme.colors.table.background};
    }

    > tbody:nth-of-type(odd) > tr {
      background-color: ${theme.colors.table.backgroundAlt};
    }
  }
`);

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

const filterAccessibleColumns = (
  columns: Array<Column>,
  userPermissions: Immutable.List<string>,
) => columns.filter(({ permissions, anyPermissions, hidden }) => {
  if (hidden) {
    return false;
  }

  if (permissions?.length) {
    return anyPermissions
      ? isAnyPermitted(userPermissions, permissions)
      : isPermitted(userPermissions, permissions);
  }

  return true;
});

const filterVisibleColumns = (
  columnDefinitions: Array<Column>,
  visibleColumns: Array<string>,
) => visibleColumns.map((columnId) => columnDefinitions
  .find(({ id }) => id === columnId))
  .filter((column) => !!column);

const useElementsWidths = <Entity extends EntityBase>({
  columns,
  columnRenderersByAttribute,
  displayBulkSelectCol,
  fixedActionsCellWidth,
}: {
  columns: Array<Column>,
  columnRenderersByAttribute: ColumnRenderersByAttribute<Entity>,
  displayBulkSelectCol: boolean
  fixedActionsCellWidth: number | undefined
}) => {
  const tableRef = useRef<HTMLTableElement>();
  const actionsRef = useRef<HTMLDivElement>();
  const { width: tableWidth } = useElementDimensions(tableRef);
  const columnsIds = useMemo(() => columns.map(({ id }) => id), [columns]);
  const actionsColInnerWidth = fixedActionsCellWidth ?? (actionsRef.current?.offsetWidth ?? 0);
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

const mergeColumnsRenderers = <Entity extends EntityBase>(columns: Array<Column>, customColumnRenderers: ColumnRenderers<Entity>) => {
  const renderers = merge({}, DefaultColumnRenderers, customColumnRenderers);

  return Object.fromEntries(columns.map(({ id, type }) => {
    const typeRenderer = renderers.types?.[type];
    const attributeRenderer = renderers.attributes?.[id];

    const columnRenderer = merge({}, typeRenderer, attributeRenderer);

    return [id, columnRenderer];
  }));
};

type Props<Entity extends EntityBase> = {
  /**
   * Needs to be defined when not all action cells in every row have the same width.
   * When they have the same width, the column width will be calculated automatically.
   * Should not include the actions col padding. Should be teh max width an action cell can have.
   */
  actionsCellWidth?: number
  /** Currently active sort */
  activeSort?: Sort,
  /**
   * The column ids are always snake case. By default, entity attributes are camel case.
   * This prop controls if the column ids need to be transformed to camel case to connect them with the entity attributes.
   */
  entityAttributesAreCamelCase?: boolean,
  bulkSelection?: {
    /** Supported bulk actions */
    actions?: React.ReactNode,
    /** Callback which runs on selection change */
    onChangeSelection?: (selectedEntities: Array<Entity['id']>) => void,
    /** Initial selected items */
    initialSelection?: Array<Entity['id']>,
    isEntitySelectable?: (entity: Entity) => boolean
  },
  /** List of all available columns. Column ids need to be snake case. */
  columnDefinitions: Array<Column>,
  /** Custom cell and header renderer for a column. Column ids need to be snake case. */
  columnRenderers?: ColumnRenderers<Entity>,
  /** Define default columns order. Column ids need to be snake case. */
  columnsOrder?: Array<string>,
  /** The table data. */
  data: Readonly<Array<Entity>>,
  /** Allows you to extend a row with additional information * */
  expandedSectionsRenderer?: {
    [sectionName: string]: ExpandedSectionRenderer<Entity>
  },
  /** Function to handle changes of columns visibility */
  onColumnsChange: (columnIds: Array<string>) => void,
  /** Function to handle sort changes */
  onSortChange: (newSort: Sort) => void,
  /** Function to handle page size changes */
  onPageSizeChange?: (newPageSize: number) => void,
  /** Active page size */
  pageSize?: number
  /** Actions for each row. */
  rowActions?: (entity: Entity) => React.ReactNode,
  /** Which columns should be displayed. */
  visibleColumns: Array<string>,
};

/**
 * Flexible data table component which allows defining custom column renderers.
 */
const EntityDataTable = <Entity extends EntityBase>({
  actionsCellWidth: fixedActionsCellWidth,
  activeSort,
  entityAttributesAreCamelCase,
  bulkSelection: {
    actions,
    onChangeSelection,
    initialSelection,
    isEntitySelectable,
  },
  columnDefinitions,
  columnRenderers: customColumnRenderers,
  columnsOrder,
  data,
  expandedSectionsRenderer,
  onColumnsChange,
  onPageSizeChange,
  onSortChange,
  pageSize,
  rowActions,
  visibleColumns,
}: Props<Entity>) => {
  const currentUser = useCurrentUser();
  const displayActionsCol = typeof rowActions === 'function';
  const displayBulkAction = !!actions;
  const displayBulkSelectCol = typeof onChangeSelection === 'function' || displayBulkAction;
  const displayPageSizeSelect = typeof onPageSizeChange === 'function';
  const _isEntitySelectable = useCallback((entity: Entity) => {
    if (!displayBulkSelectCol) return false;

    if (typeof isEntitySelectable === 'function') return isEntitySelectable(entity);

    return true;
  }, [displayBulkSelectCol, isEntitySelectable]);

  const accessibleColumns = useMemo(
    () => filterAccessibleColumns(columnDefinitions, currentUser.permissions),
    [columnDefinitions, currentUser.permissions],
  );

  const columns = useMemo(
    () => filterVisibleColumns(accessibleColumns, visibleColumns),
    [accessibleColumns, visibleColumns],
  );

  const columnRenderersByAttribute = useMemo(() => mergeColumnsRenderers<Entity>(columns, customColumnRenderers), [columns, customColumnRenderers]);

  const { tableRef, actionsRef, actionsColWidth, columnsWidths } = useElementsWidths<Entity>({
    columns,
    columnRenderersByAttribute,
    displayBulkSelectCol,
    fixedActionsCellWidth,
  });

  const selectableData = useMemo(() => data.filter(_isEntitySelectable), [data, _isEntitySelectable]);

  return (
    <SelectedEntitiesProvider<Entity> initialSelection={initialSelection} onChangeSelection={onChangeSelection}>

      <ExpandedSectionsProvider>
        <ActionsRow>
          <div>
            {displayBulkAction && <BulkActionsRow bulkActions={actions} />}
          </div>
          <LayoutConfigRow>
            Show
            <ButtonGroup>
              {displayPageSizeSelect && (
                <PageSizeSelect pageSize={pageSize} showLabel={false} onChange={onPageSizeChange} />
              )}
              <ColumnsVisibilitySelect allColumns={accessibleColumns}
                                       selectedColumns={visibleColumns}
                                       onChange={onColumnsChange} />
            </ButtonGroup>
          </LayoutConfigRow>
        </ActionsRow>
        <ScrollContainer id="scroll-container" ref={tableRef}>
          <StyledTable striped condensed hover>
            <TableHead columns={columns}
                       columnsOrder={columnsOrder}
                       actionsColWidth={actionsColWidth}
                       columnsWidths={columnsWidths}
                       data={selectableData}
                       columnRenderersByAttribute={columnRenderersByAttribute}
                       onSortChange={onSortChange}
                       displayBulkSelectCol={displayBulkSelectCol}
                       activeSort={activeSort}
                       displayActionsCol={displayActionsCol} />
            {data.map((entity, index) => (
              <tbody key={`table-row-${entity.id}`} data-testid={`table-row-${entity.id}`}>
                <TableRow entity={entity}
                          index={index}
                          entityAttributesAreCamelCase={entityAttributesAreCamelCase}
                          actionsRef={actionsRef}
                          columnRenderersByAttribute={columnRenderersByAttribute}
                          rowActions={rowActions}
                          displaySelect={displayBulkSelectCol}
                          isEntitySelectable={_isEntitySelectable}
                          displayActions={displayActionsCol}
                          columns={columns} />
                <ExpandedSections key={`expanded-sections-${entity.id}`}
                                  expandedSectionsRenderer={expandedSectionsRenderer}
                                  entity={entity} />
              </tbody>
            ))}
          </StyledTable>
        </ScrollContainer>
      </ExpandedSectionsProvider>
    </SelectedEntitiesProvider>

  );
};

EntityDataTable.defaultProps = {
  actionsCellWidth: undefined,
  activeSort: undefined,
  columnRenderers: undefined,
  columnsOrder: [],
  expandedSectionsRenderer: undefined,
  onPageSizeChange: undefined,
  pageSize: undefined,
  rowActions: undefined,
  entityAttributesAreCamelCase: true,
  bulkSelection: {},
};

export default EntityDataTable;

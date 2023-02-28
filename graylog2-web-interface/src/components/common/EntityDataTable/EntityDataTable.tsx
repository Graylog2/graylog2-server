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
import { useMemo, useState, useCallback, useRef } from 'react';
import type * as Immutable from 'immutable';
import { merge } from 'lodash';

import { Button, Table, ButtonGroup, ButtonToolbar } from 'components/bootstrap';
import { isPermitted, isAnyPermitted } from 'util/PermissionsMixin';
import useCurrentUser from 'hooks/useCurrentUser';
import StringUtils from 'util/StringUtils';
import ColumnsVisibilitySelect from 'components/common/EntityDataTable/ColumnsVisibilitySelect';
import DefaultColumnRenderers from 'components/common/EntityDataTable/DefaultColumnRenderers';
import { CELL_PADDING, BULK_SELECT_COLUMN_WIDTH } from 'components/common/EntityDataTable/Constants';
import useColumnsWidths from 'components/common/EntityDataTable/hooks/useColumnsWidths';
import useElementDimensions from 'hooks/useElementDimensions';
import type { Sort } from 'stores/PaginationTypes';
import { PageSizeSelect } from 'components/common';

import TableHead from './TableHead';
import TableRow from './TableRow';
import type { ColumnRenderers, Column, EntityBase } from './types';

const ScrollContainer = styled.div`
  overflow-x: auto;
`;

const StyledTable = styled(Table)`
  table-layout: fixed;

  thead > tr > th,
  tbody > tr > td {
    padding: ${CELL_PADDING}px;
  }
`;

const ActionsRow = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  min-height: 22px;
`;

const BulkActionsWrapper = styled.div`
  display: flex;
  align-items: center;
`;

const BulkActions = styled(ButtonToolbar)`
  margin-left: 5px;
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
  columnRenderers,
  displayBulkSelectCol,
}: {
  columns: Array<Column>,
  columnRenderers: ColumnRenderers<Entity>,
  displayBulkSelectCol: boolean
}) => {
  const tableRef = useRef<HTMLTableElement>();
  const actionsRef = useRef<HTMLDivElement>();
  const { width: tableWidth } = useElementDimensions(tableRef);
  const columnsIds = useMemo(() => columns.map(({ id }) => id), [columns]);
  const actionsColWidth = actionsRef.current?.offsetWidth ? (actionsRef.current.offsetWidth ?? 0) + CELL_PADDING * 2 : 0;

  const columnsWidths = useColumnsWidths<Entity>({
    actionsColWidth,
    bulkSelectColWidth: displayBulkSelectCol ? BULK_SELECT_COLUMN_WIDTH : 0,
    columnRenderers,
    columnsIds,
    tableWidth,
  });

  return { tableRef, actionsRef, columnsWidths, actionsColWidth };
};

type Props<Entity extends EntityBase> = {
  /** Currently active sort */
  activeSort?: Sort,
  /** Supported batch operations */
  bulkActions?: (selectedEntities: Array<string>, setSelectedEntities: (streamIds: Array<string>) => void) => React.ReactNode
  /** List of all available columns. */
  columnDefinitions: Array<Column>,
  /** Custom cell and header renderer for a column */
  columnRenderers?: ColumnRenderers<Entity>,
  /** Define default columns order */
  columnsOrder?: Array<string>,
  /** The table data. */
  data: Readonly<Array<Entity>>,
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
  activeSort,
  bulkActions,
  columnRenderers: customColumnRenderers,
  columnDefinitions,
  columnsOrder,
  onPageSizeChange,
  pageSize,
  data,
  onSortChange,
  rowActions,
  onColumnsChange,
  visibleColumns,
}: Props<Entity>) => {
  const currentUser = useCurrentUser();
  const [selectedEntities, setSelectedEntities] = useState<Array<string>>([]);
  const columnRenderers = merge(DefaultColumnRenderers, customColumnRenderers);
  const displayActionsCol = typeof rowActions === 'function';
  const displayBulkSelectCol = typeof bulkActions === 'function';
  const displayPageSizeSelect = typeof onPageSizeChange === 'function';

  const accessibleColumns = useMemo(
    () => filterAccessibleColumns(columnDefinitions, currentUser.permissions),
    [columnDefinitions, currentUser.permissions],
  );

  const columns = useMemo(
    () => filterVisibleColumns(accessibleColumns, visibleColumns),
    [accessibleColumns, visibleColumns],
  );

  const { tableRef, actionsRef, actionsColWidth, columnsWidths } = useElementsWidths({
    columns,
    columnRenderers,
    displayBulkSelectCol,
  });

  const onToggleEntitySelect = useCallback((itemId: string) => {
    setSelectedEntities(((cur) => {
      if (cur.includes(itemId)) {
        return cur.filter((id) => id !== itemId);
      }

      return [...cur, itemId];
    }));
  }, []);

  const unselectAllItems = useCallback(() => setSelectedEntities([]), []);

  return (
    <>
      <ActionsRow>
        <div>
          {(displayBulkSelectCol && !!selectedEntities?.length) && (
            <BulkActionsWrapper>
              {selectedEntities.length} {StringUtils.pluralize(selectedEntities.length, 'item', 'items')} selected
              <BulkActions>
                {bulkActions(selectedEntities, setSelectedEntities)}
                <Button bsSize="xsmall" onClick={unselectAllItems}>Cancel</Button>
              </BulkActions>
            </BulkActionsWrapper>
          )}
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
      <ScrollContainer ref={tableRef}>
        <StyledTable striped condensed hover>
          <TableHead columns={columns}
                     columnsOrder={columnsOrder}
                     actionsColWidth={actionsColWidth}
                     columnsWidths={columnsWidths}
                     selectedEntities={selectedEntities}
                     setSelectedEntities={setSelectedEntities}
                     data={data}
                     columnRenderers={columnRenderers}
                     onSortChange={onSortChange}
                     displayBulkSelectCol={displayBulkSelectCol}
                     activeSort={activeSort}
                     displayActionsCol={displayActionsCol} />
          <tbody>
            {data.map((entity, index) => (
              <TableRow entity={entity}
                        key={entity.id}
                        index={index}
                        actionsRef={actionsRef}
                        onToggleEntitySelect={onToggleEntitySelect}
                        columnRenderers={columnRenderers}
                        isSelected={!!selectedEntities?.includes(entity.id)}
                        rowActions={rowActions}
                        displaySelect={displayBulkSelectCol}
                        displayActions={displayActionsCol}
                        columns={columns} />
            ))}
          </tbody>
        </StyledTable>
      </ScrollContainer>
    </>
  );
};

EntityDataTable.defaultProps = {
  activeSort: undefined,
  bulkActions: undefined,
  columnRenderers: undefined,
  columnsOrder: [],
  onPageSizeChange: undefined,
  pageSize: undefined,
  rowActions: undefined,
};

export default EntityDataTable;

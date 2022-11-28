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
import { useMemo, useState, useCallback } from 'react';
import type * as Immutable from 'immutable';
import { merge } from 'lodash';

import { Button, Table, ButtonToolbar } from 'components/bootstrap';
import { isPermitted, isAnyPermitted } from 'util/PermissionsMixin';
import TableHead from 'components/common/EntityDataTable/TableHead';
import TableRow from 'components/common/EntityDataTable/TableRow';
import useCurrentUser from 'hooks/useCurrentUser';
import StringUtils from 'util/StringUtils';
import ColumnsVisibilitySelect from 'components/common/EntityDataTable/ColumnsVisibilitySelect';
import DefaultColumnRenderers from 'components/common/EntityDataTable/DefaultColumnRenderers';
import useCalculateColumnWidths from 'components/common/EntityDataTable/hooks/useCalculateColumnsWidths';
import WindowDimensionsContextProvider from 'contexts/WindowDimensionsContextProvider';

import type { ColumnRenderers, Column, Sort } from './types';

const ScrollContainer = styled.div(({ theme }) => css`
  //@media (max-width: ${theme.breakpoints.max.md}) {
    overflow-x: auto;
  //}
`);

const StyledTable = styled(Table)`
  table-layout: fixed;
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

const filterAccessibleColumns = (
  columns: Array<Column>,
  userPermissions: Immutable.List<string>,
) => columns.filter(({ permissions, anyPermissions }) => {
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

type Props<Entity extends { id: string }> = {
  /** Currently active sort */
  activeSort?: Sort,
  /** Supported batch operations */
  bulkActions?: (selectedEntities: Array<string>, setSelectedEntities: (streamIds: Array<string>) => void) => React.ReactNode
  /** List of all available columns. */
  columnDefinitions: Array<Column>,
  /** Custom cell and header renderer for a column */
  columnRenderers?: ColumnRenderers<Entity>,
  /** The table data. */
  data: Array<Entity>,
  /** Function to handle changes of columns visibility */
  onColumnsChange: (columnIds: Array<string>) => void,
  /** Function to handle sort changes */
  onSortChange: (newSort: Sort) => void,
  /** Actions for each row. */
  rowActions?: (entity: Entity) => React.ReactNode,
  /** Which columns should be displayed. */
  visibleColumns: Array<string>,
};

/**
 * Flexible data table component which allows defining custom column renderers.
 */
const EntityDataTable = <Entity extends { id: string }>({
  activeSort,
  bulkActions,
  columnRenderers: customColumnRenderers,
  columnDefinitions,
  data,
  onSortChange,
  rowActions,
  onColumnsChange,
  visibleColumns,
}: Props<Entity>) => {
  const currentUser = useCurrentUser();
  const [selectedEntities, setSelectedEntities] = useState<Array<string>>([]);
  const columnRenderers = merge(DefaultColumnRenderers, customColumnRenderers ?? {});
  const accessibleColumns = useMemo(
    () => filterAccessibleColumns(columnDefinitions, currentUser.permissions),
    [columnDefinitions, currentUser.permissions],
  );

  const columns = useMemo(
    () => filterVisibleColumns(accessibleColumns, visibleColumns),
    [accessibleColumns, visibleColumns],
  );

  const onToggleEntitySelect = useCallback((itemId: string) => {
    setSelectedEntities(((cur) => {
      if (cur.includes(itemId)) {
        return cur.filter((id) => id !== itemId);
      }

      return [...cur, itemId];
    }));
  }, []);

  const unselectAllItems = useCallback(() => setSelectedEntities([]), []);
  const displayActionsCol = typeof rowActions === 'function';
  const displayBulkActionsCol = typeof bulkActions === 'function';
  const { actionsRef, tableRef, columnsWidths, actionsColWidth } = useCalculateColumnWidths<Entity>(columns, columnRenderers, displayActionsCol, displayBulkActionsCol);

  return (
    <ScrollContainer ref={tableRef}>
      <ActionsRow>
        <div>
          {(displayBulkActionsCol && !!selectedEntities?.length) && (
            <BulkActionsWrapper>
              {selectedEntities.length} {StringUtils.pluralize(selectedEntities.length, 'item', 'items')} selected
              <BulkActions>
                {bulkActions(selectedEntities, setSelectedEntities)}
                <Button bsSize="xsmall" onClick={unselectAllItems}>Cancel</Button>
              </BulkActions>
            </BulkActionsWrapper>
          )}
        </div>
        <div>
          <ColumnsVisibilitySelect allColumns={accessibleColumns}
                                   selectedColumns={visibleColumns}
                                   onChange={onColumnsChange} />
        </div>
      </ActionsRow>
      <StyledTable striped condensed hover>
        <TableHead columns={columns}
                   columnsWidths={columnsWidths}
                   selectedEntities={selectedEntities}
                   setSelectedEntities={setSelectedEntities}
                   data={data}
                   columnRenderers={columnRenderers}
                   onSortChange={onSortChange}
                   displayBulkActionsCol={displayBulkActionsCol}
                   activeSort={activeSort}
                   actionsColWidth={actionsColWidth}
                   displayActionsCol={displayActionsCol} />
        <tbody>
          {data.map((entity, index) => (
            <TableRow entity={entity}
                      key={entity.id}
                      index={index}
                      actionsRef={actionsRef}
                      onToggleEntitySelect={onToggleEntitySelect}
                      columnRenderers={columnRenderers}
                      columnsWidths={columnsWidths}
                      isSelected={!!selectedEntities?.includes(entity.id)}
                      rowActions={rowActions}
                      displaySelect={displayBulkActionsCol}
                      displayActions={displayActionsCol}
                      actionsColWidth={actionsColWidth}
                      columns={columns} />
          ))}
        </tbody>
      </StyledTable>
    </ScrollContainer>
  );
};

EntityDataTable.defaultProps = {
  activeSort: undefined,
  bulkActions: undefined,
  columnRenderers: undefined,
  rowActions: undefined,
};

export default (props) => (
  <WindowDimensionsContextProvider>
    <EntityDataTable {...props} />
  </WindowDimensionsContextProvider>
);

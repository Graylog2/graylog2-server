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
import { useMemo, useState, useCallback, useEffect } from 'react';
import styled, { css } from 'styled-components';
import merge from 'lodash/merge';
import type { ColumnDef } from '@tanstack/react-table';
import { createColumnHelper } from '@tanstack/react-table';

import { ButtonGroup } from 'components/bootstrap';
import { isPermitted, isAnyPermitted } from 'util/PermissionsMixin';
import useCurrentUser from 'hooks/useCurrentUser';
import ColumnsVisibilitySelect from 'components/common/EntityDataTable/ColumnsVisibilitySelect';
import DefaultColumnRenderers from 'components/common/EntityDataTable/DefaultColumnRenderers';
import type { Sort } from 'stores/PaginationTypes';
import { PageSizeSelect } from 'components/common';
import SelectedEntitiesProvider from 'components/common/EntityDataTable/contexts/SelectedEntitiesProvider';
import MetaDataProvider from 'components/common/EntityDataTable/contexts/MetaDataProvider';
import useTable from 'components/common/EntityDataTable/hooks/useTable';
import useElementWidths from 'components/common/EntityDataTable/hooks/useElementWidths';
import useVisibleColumnOrder from 'components/common/EntityDataTable/hooks/useVisibleColumnOrder';
import useBulkSelectColumnDefinition from 'components/common/EntityDataTable/hooks/useBulkSelectColumnDefinition';
import useActionsColumnDefinition from 'components/common/EntityDataTable/hooks/useActionsColumnDefinition';
import useAttributeColumnDefinitions from 'components/common/EntityDataTable/hooks/useAttributeColumnDefinitions';
import TableDndProvider from 'components/common/EntityDataTable/TableDndProvider';
import Table from 'components/common/EntityDataTable/Table';
import DndStylesContext from 'components/common/EntityDataTable/contexts/DndStylesContext';
import { columnTransformVar, columnWidthVar, columnOpacityVar } from 'components/common/EntityDataTable/CSSVariables';

import type {
  ColumnRenderers,
  ColumnSchema,
  EntityBase,
  ColumnRenderersByAttribute,
  ColumnPreferences,
  ExpandedSectionRenderers,
} from './types';
import ExpandedSectionsProvider from './contexts/ExpandedSectionsProvider';
import BulkActionsRow from './BulkActionsRow';

const ScrollContainer = styled.div<{
  $columnWidths: { [_attributeId: string]: number };
  $activeColId: string | null;
  $columnTransform: { [_attributeId: string]: string };
}>(
  ({ $columnWidths, $activeColId, $columnTransform }) => css`
    width: 100%;
    overflow-x: auto;

    ${Object.entries($columnWidths)
      .map(([id, width]) => `${columnWidthVar(id)}: ${width}px;`)
      .join('\n')}

    ${$activeColId ? `${columnOpacityVar($activeColId)}: 0.4;` : ''}
    ${$activeColId ? `--col-transition: transform 0.2s ease-in-out;` : ''}

    ${Object.entries($columnTransform)
      .map(([id, transform]) => `${columnTransformVar(id)}: ${transform};`)
      .join('\n')}
  `,
);

const ActionsRow = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  min-height: 22px;
  width: 100%;
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
  actionsRef,
  columnRenderersByAttribute,
  columnSchemas,
  columnWidths,
  hasRowActions,
  displayBulkSelectCol,
  entityActions,
  entityAttributesAreCamelCase,
  meta,
}: {
  actionsRef: React.MutableRefObject<HTMLDivElement>;
  columnRenderersByAttribute: ColumnRenderersByAttribute<Entity, Meta>;
  columnSchemas: Array<ColumnSchema>;
  columnWidths: { [_attributeId: string]: number };
  hasRowActions: boolean;
  displayBulkSelectCol: boolean;
  entityActions?: (entity: Entity) => React.ReactNode;
  entityAttributesAreCamelCase: boolean;
  meta: Meta;
}) => {
  const columnHelper = createColumnHelper<Entity>();
  const bulkSelectCol = useBulkSelectColumnDefinition(displayBulkSelectCol);
  const actionsCol = useActionsColumnDefinition(hasRowActions, columnWidths.actions, entityActions, actionsRef);
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
  /** Min width of actions cell **/
  minActionsCellWidth?: number;
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
  defaultDisplayedColumns: Array<string>;
  defaultColumnOrder: Array<string>;
  /** The table data. */
  entities: ReadonlyArray<Entity>;
  /** Allows you to extend a row with additional information * */
  expandedSectionRenderers?: ExpandedSectionRenderers<Entity>;
  /** User layout preferences */
  layoutPreferences: {
    attributes?: ColumnPreferences;
    order?: Array<string>;
  };
  /** Function to handle update of user layout preferences */
  onLayoutPreferencesChange: ({
    attributes,
    order,
  }: {
    attributes?: ColumnPreferences;
    order?: Array<string>;
  }) => Promise<void>;
  /** Function to handle sort changes */
  onSortChange: (newSort: Sort) => void;
  /** Function to handle page size changes */
  onPageSizeChange?: (newPageSize: number) => void;
  /** Function to handle layout preferences reset */
  onResetLayoutPreferences: () => Promise<void>;
  /** Active page size */
  pageSize?: number;
  /** Actions for each row. */
  entityActions?: (entity: Entity) => React.ReactNode;
  /** Meta data. */
  meta?: Meta;
};

/**
 * Flexible data table component which allows defining custom column renderers.
 */
const EntityDataTable = <Entity extends EntityBase, Meta = unknown>({
  activeSort = undefined,
  bulkSelection: { actions, onChangeSelection, initialSelection, isEntitySelectable } = {},
  columnRenderers: customColumnRenderers = undefined,
  columnSchemas,
  defaultColumnOrder,
  defaultDisplayedColumns,
  entities,
  entityActions = undefined,
  entityAttributesAreCamelCase,
  expandedSectionRenderers = undefined,
  layoutPreferences,
  meta = undefined,
  minActionsCellWidth: fixedActionsCellWidth = undefined,
  onLayoutPreferencesChange,
  onPageSizeChange = undefined,
  onResetLayoutPreferences,
  onSortChange,
  pageSize = undefined,
}: Props<Entity, Meta>) => {
  const [headerSectionsWidth, setHeaderSectionsWidth] = useState<{
    [colId: string]: { left?: number; right?: number };
  }>({});
  const headerMinWidths = useMemo(
    () =>
      Object.fromEntries(
        Object.entries(headerSectionsWidth).map(([colId, { left = 0, right = 0 }]) => [
          colId,
          Math.round(left + right),
        ]),
      ),
    [headerSectionsWidth],
  );

  useEffect(() => {
    // Reset header sections width when layout preferences change
    console.log('headerMinWidths', headerMinWidths);
  }, [headerMinWidths]);

  const handleHeaderSectionResize = useCallback((colId: string, part: 'left' | 'right', width: number) => {
    setHeaderSectionsWidth((cur) => {
      const currentCol = cur[colId] ?? {};
      const roundedWidth = Math.round(width);

      if (currentCol[part] === roundedWidth) {
        return cur;
      }

      return { ...cur, [colId]: { ...currentCol, [part]: roundedWidth } };
    });
  }, []);
  const [selectedEntities, setSelectedEntities] = useState<Array<Entity['id']>>(initialSelection ?? []);
  const hasRowActions = typeof entityActions === 'function';
  const displayBulkAction = !!actions;
  const displayBulkSelectCol = typeof onChangeSelection === 'function' || displayBulkAction;
  const displayPageSizeSelect = typeof onPageSizeChange === 'function';
  const authorizedColumnSchemas = useAuthorizedColumnSchemas(columnSchemas);
  const columnRenderersByAttribute = useColumnRenderers<Entity, Meta>(authorizedColumnSchemas, customColumnRenderers);
  const [internalAttributeColumnOrder, setInternalAttributeColumnOrder] = useState<Array<string>>(
    layoutPreferences?.order ?? defaultColumnOrder,
  );
  const [internalColumnWidthPreferences, setInternalColumnWidthPreferences] = useState<{
    [attributeId: string]: number;
  }>(() =>
    Object.fromEntries(
      Object.entries(layoutPreferences?.attributes ?? {}).flatMap(([key, { width }]) =>
        typeof width === 'number' ? [[key, width]] : [],
      ),
    ),
  );

  const columnOrder = useVisibleColumnOrder(
    layoutPreferences?.attributes,
    internalAttributeColumnOrder,
    defaultDisplayedColumns,
    displayBulkSelectCol,
  );

  const { tableRef, actionsRef, columnWidths } = useElementWidths<Entity, Meta>({
    columnRenderersByAttribute,
    columnSchemas: authorizedColumnSchemas,
    columnWidthPreferences: internalColumnWidthPreferences,
    displayBulkSelectCol,
    fixedActionsCellWidth,
    headerMinWidths,
    visibleColumns: columnOrder,
  });

  const columnDefinitions = useColumnDefinitions<Entity, Meta>({
    actionsRef,
    columnRenderersByAttribute,
    columnSchemas: authorizedColumnSchemas,
    columnWidths,
    hasRowActions,
    displayBulkSelectCol,
    entityActions,
    entityAttributesAreCamelCase,
    meta,
  });
  const table = useTable<Entity>({
    columnOrder,
    columnRenderersByAttribute,
    columnWidths,
    columnDefinitions,
    defaultColumnOrder,
    displayBulkSelectCol,
    entities,
    headerMinWidths,
    internalColumnWidthPreferences,
    isEntitySelectable,
    layoutPreferences,
    onChangeSelection,
    onLayoutPreferencesChange,
    onSortChange,
    selectedEntities,
    setInternalAttributeColumnOrder,
    setInternalColumnWidthPreferences,
    setSelectedEntities,
    sort: activeSort,
  });

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const headerGroups = useMemo(() => table.getHeaderGroups(), [columnOrder]);

  const resetLayoutPreferences = useCallback(() => {
    onResetLayoutPreferences().then(() => {
      setInternalAttributeColumnOrder(defaultColumnOrder);
      setInternalColumnWidthPreferences({});
    });
  }, [defaultColumnOrder, onResetLayoutPreferences]);

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
                <ColumnsVisibilitySelect<Entity> table={table} onResetLayoutPreferences={resetLayoutPreferences} />
              </ButtonGroup>
            </LayoutConfigRow>
          </ActionsRow>
          <TableDndProvider table={table}>
            <DndStylesContext.Consumer>
              {({ activeColId, columnTransform }) => (
                <ScrollContainer
                  id="scroll-container"
                  ref={tableRef}
                  $columnWidths={columnWidths}
                  $activeColId={activeColId}
                  $columnTransform={columnTransform}>
                  <Table<Entity>
                    onHeaderSectionResize={handleHeaderSectionResize}
                    expandedSectionRenderers={expandedSectionRenderers}
                    headerGroups={headerGroups}
                    rows={table.getRowModel().rows}
                    hasRowActions={hasRowActions}
                  />
                </ScrollContainer>
              )}
            </DndStylesContext.Consumer>
          </TableDndProvider>
        </ExpandedSectionsProvider>
      </SelectedEntitiesProvider>
    </MetaDataProvider>
  );
};

export default EntityDataTable;

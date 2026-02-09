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
import { useMemo, useState, useCallback, useRef } from 'react';
import styled, { css } from 'styled-components';

import { ButtonGroup } from 'components/bootstrap';
import ColumnsVisibilitySelect from 'components/common/EntityDataTable/ColumnsVisibilitySelect';
import type { Sort } from 'stores/PaginationTypes';
import { PageSizeSelect } from 'components/common';
import SelectedEntitiesProvider from 'components/common/EntityDataTable/contexts/SelectedEntitiesProvider';
import MetaDataProvider from 'components/common/EntityDataTable/contexts/MetaDataProvider';
import useTable from 'components/common/EntityDataTable/hooks/useTable';
import useElementWidths from 'components/common/EntityDataTable/hooks/useElementWidths';
import useVisibleColumnOrder from 'components/common/EntityDataTable/hooks/useVisibleColumnOrder';
import TableDndProvider from 'components/common/EntityDataTable/TableDndProvider';
import Table from 'components/common/EntityDataTable/Table';
import DndStylesContext from 'components/common/EntityDataTable/contexts/DndStylesContext';
import {
  actionsHeaderWidthVar,
  columnOpacityVar,
  columnTransition,
  columnTransformVar,
  columnWidthVar,
  displayScrollRightIndicatorVar,
} from 'components/common/EntityDataTable/CSSVariables';
import useHeaderMinWidths from 'components/common/EntityDataTable/hooks/useHeaderMinWidths';
import useColumnDefinitions from 'components/common/EntityDataTable/hooks/useColumnDefinitions';
import useColumnRenderers from 'components/common/EntityDataTable/hooks/useColumnRenderers';
import useAuthorizedColumnSchemas from 'components/common/EntityDataTable/hooks/useAuthorizedColumnSchemas';
import useIntersectionObserver from 'hooks/useIntersectionObserver';
import { CELL_PADDING } from 'components/common/EntityDataTable/Constants';
import ActiveSliceColContext from 'components/common/EntityDataTable/contexts/ActiveSliceColContext';

import type { ColumnRenderers, ColumnSchema, EntityBase, ColumnPreferences, ExpandedSectionRenderers } from './types';
import ExpandedSectionsProvider from './contexts/ExpandedSectionsProvider';
import BulkActionsRow from './BulkActionsRow';

const cssVariable = (variable: string, value: string | number) => css`
  ${variable}: ${value};
`;

const ScrollContainer = styled.div<{
  $columnWidths: { [_attributeId: string]: number };
  $activeColId: string | null;
  $columnTransform: { [_attributeId: string]: string };
  $actionsHeaderWidth: number;
  $canScrollRight: boolean;
}>(
  ({ $columnWidths, $activeColId, $columnTransform, $actionsHeaderWidth, $canScrollRight }) => css`
    width: 100%;
    overflow-x: auto;

    ${Object.entries($columnWidths).map(([id, width]) => cssVariable(columnWidthVar(id), `${width}px`))}
    ${Object.entries($columnTransform).map(([id, transform]) => cssVariable(columnTransformVar(id), transform))}
    ${$actionsHeaderWidth && cssVariable(actionsHeaderWidthVar, `${$actionsHeaderWidth}px`)}
    ${$canScrollRight && cssVariable(displayScrollRightIndicatorVar, 'block')}
    ${$activeColId &&
    css`
      ${cssVariable(columnOpacityVar($activeColId), 0.4)}
      ${cssVariable(columnTransition(), 'transform 0.2s ease-in-out')}
    `}
  `,
);

const InnerContainer = styled.div`
  position: relative;
  height: 100%;
  width: fit-content;
`;

const ScrollRightIndicator = styled.div`
  position: absolute;
  top: 0;
  bottom: 0;
  right: 0;
  width: ${CELL_PADDING}px;
  pointer-events: none;
  z-index: 2;
`;

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

type Props<Entity extends EntityBase, Meta = unknown> = {
  /** Currently active sort */
  activeSort?: Sort;
  /** Currently active slicing column */
  activeSliceCol?: string;
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
  onChangeSlicing: (sliceCol: string | undefined, slice?: string) => void;
  /** Function to handle sort changes */
  onSortChange: (newSort: Sort) => void;
  /** Function to handle page size changes */
  onPageSizeChange?: (newPageSize: number) => void;
  /** Function to handle layout preferences reset */
  onResetLayoutPreferences: () => Promise<void>;
  /** Active page size */
  pageSize?: number;
  /** Required when parent container does not use contentBackground for the background */
  parentBgColor?: string;
  appSection?: string;
  /** Actions for each row. */
  entityActions?: (entity: Entity) => React.ReactNode | null;
  /** Meta data. */
  meta?: Meta;
};

/**
 * Flexible data table component which allows defining custom column renderers.
 */
const EntityDataTable = <Entity extends EntityBase, Meta = unknown>({
  activeSort = undefined,
  activeSliceCol = undefined,
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
  onChangeSlicing,
  onLayoutPreferencesChange,
  onPageSizeChange = undefined,
  onResetLayoutPreferences,
  onSortChange,
  pageSize = undefined,
  parentBgColor = undefined,
  appSection = undefined,
}: Props<Entity, Meta>) => {
  const [selectedEntities, setSelectedEntities] = useState<Array<Entity['id']>>(initialSelection ?? []);
  const hasRowActions = typeof entityActions === 'function';
  const displayBulkAction = !!actions;
  const displayBulkSelectCol = typeof onChangeSelection === 'function' || displayBulkAction;
  const displayPageSizeSelect = typeof onPageSizeChange === 'function';
  const authorizedColumnSchemas = useAuthorizedColumnSchemas(columnSchemas);
  const columnRenderersByAttribute = useColumnRenderers<Entity, Meta>(authorizedColumnSchemas, customColumnRenderers);
  const { headerMinWidths, handleHeaderSectionResize } = useHeaderMinWidths();
  const scrollContainerRef = useRef<HTMLDivElement>(null);
  const scrolledToRightIndicator = useRef<HTMLDivElement>();
  const scrolledToRight = useIntersectionObserver(scrollContainerRef, scrolledToRightIndicator);

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

  const { columnWidths, handleActionsWidthChange, tableIsCompressed, actionsColMinWidth } = useElementWidths<
    Entity,
    Meta
  >({
    columnRenderersByAttribute,
    columnSchemas: authorizedColumnSchemas,
    columnWidthPreferences: internalColumnWidthPreferences,
    displayBulkSelectCol,
    entities,
    hasRowActions,
    headerMinWidths,
    scrollContainerRef,
    visibleColumns: columnOrder,
  });

  const columnDefinitions = useColumnDefinitions<Entity, Meta>({
    actionsColMinWidth,
    columnRenderersByAttribute,
    columnSchemas: authorizedColumnSchemas,
    columnWidths,
    displayBulkSelectCol,
    entityActions,
    entityAttributesAreCamelCase,
    hasRowActions,
    meta,
    onActionsWidthChange: handleActionsWidthChange,
    onChangeSlicing,
    onHeaderSectionResize: handleHeaderSectionResize,
    parentBgColor,
    appSection,
  });

  const table = useTable<Entity>({
    columnOrder,
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
      <SelectedEntitiesProvider<Entity>
        table={table}
        selectedEntities={selectedEntities}
        isSomeRowsSelected={table.getIsSomeRowsSelected()}
        isAllRowsSelected={table.getIsAllRowsSelected()}>
        <ActiveSliceColContext.Provider value={activeSliceCol}>
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
                    ref={scrollContainerRef}
                    $actionsHeaderWidth={actionsColMinWidth}
                    $activeColId={activeColId}
                    $columnTransform={columnTransform}
                    $columnWidths={columnWidths}
                    $canScrollRight={scrolledToRight && tableIsCompressed}>
                    <InnerContainer>
                      <Table<Entity>
                        expandedSectionRenderers={expandedSectionRenderers}
                        headerGroups={headerGroups}
                        rows={table.getRowModel().rows}
                      />
                      <ScrollRightIndicator ref={scrolledToRightIndicator} />
                    </InnerContainer>
                  </ScrollContainer>
                )}
              </DndStylesContext.Consumer>
            </TableDndProvider>
          </ExpandedSectionsProvider>
        </ActiveSliceColContext.Provider>
      </SelectedEntitiesProvider>
    </MetaDataProvider>
  );
};

export default EntityDataTable;

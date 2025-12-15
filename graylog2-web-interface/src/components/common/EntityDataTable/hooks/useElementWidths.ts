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

import { useMemo } from 'react';

import type { EntityBase, ColumnRenderersByAttribute } from 'components/common/EntityDataTable/types';
import type { ColumnSchema } from 'components/common/EntityDataTable';
import useElementDimensions from 'hooks/useElementDimensions';
import { BULK_SELECT_COLUMN_WIDTH, ACTIONS_COL_ID } from 'components/common/EntityDataTable/Constants';
import useColumnWidths from 'components/common/EntityDataTable/hooks/useColumnWidths';
import useActionsColumnWidth from 'components/common/EntityDataTable/hooks/useActionsColumnWidth';

type Props<Entity extends EntityBase, Meta> = {
  columnRenderersByAttribute: ColumnRenderersByAttribute<Entity, Meta>;
  columnSchemas: Array<ColumnSchema>;
  columnWidthPreferences: { [colId: string]: number };
  displayBulkSelectCol: boolean;
  hasRowActions: boolean;
  headerMinWidths: { [colId: string]: number };
  visibleColumns: Array<string>;
  entities: ReadonlyArray<Entity>;
  scrollContainerRef: React.RefObject<HTMLDivElement>;
};

const useElementWidths = <Entity extends EntityBase, Meta>({
  columnRenderersByAttribute,
  columnSchemas,
  columnWidthPreferences,
  displayBulkSelectCol,
  entities,
  hasRowActions,
  headerMinWidths,
  scrollContainerRef,
  visibleColumns,
}: Props<Entity, Meta>) => {
  const { colMinWidth: actionsColMinWidth, handleWidthChange: handleActionsWidthChange } =
    useActionsColumnWidth<Entity>(entities, hasRowActions);
  const { width: scrollContainerWidth } = useElementDimensions(scrollContainerRef);
  const columnIds = useMemo(
    () => columnSchemas.filter(({ id }) => visibleColumns.includes(id)).map(({ id }) => id),
    [columnSchemas, visibleColumns],
  );
  const columnWidths = useColumnWidths<Entity>({
    actionsColMinWidth,
    bulkSelectColWidth: displayBulkSelectCol ? BULK_SELECT_COLUMN_WIDTH : 0,
    columnIds,
    columnRenderersByAttribute,
    columnWidthPreferences,
    headerMinWidths,
    scrollContainerWidth,
  });

  return {
    handleActionsWidthChange,
    columnWidths,
    tableIsCompressed: actionsColMinWidth === columnWidths[ACTIONS_COL_ID],
    actionsColMinWidth,
  };
};

export default useElementWidths;

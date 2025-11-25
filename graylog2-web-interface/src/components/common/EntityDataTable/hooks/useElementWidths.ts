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

import { useRef, useMemo } from 'react';

import type { EntityBase, ColumnRenderersByAttribute } from 'components/common/EntityDataTable/types';
import type { ColumnSchema } from 'components/common/EntityDataTable';
import useElementDimensions from 'hooks/useElementDimensions';
import { CELL_PADDING, BULK_SELECT_COLUMN_WIDTH } from 'components/common/EntityDataTable/Constants';
import useColumnWidths from 'components/common/EntityDataTable/hooks/useColumnWidths';

type Props<Entity extends EntityBase, Meta> = {
  columnSchemas: Array<ColumnSchema>;
  columnRenderersByAttribute: ColumnRenderersByAttribute<Entity, Meta>;
  displayBulkSelectCol: boolean;
  fixedActionsCellWidth: number | undefined;
  visibleColumns: Array<string>;
};

const useElementWidths = <Entity extends EntityBase, Meta>({
  columnSchemas,
  columnRenderersByAttribute,
  displayBulkSelectCol,
  fixedActionsCellWidth,
  visibleColumns,
}: Props<Entity, Meta>) => {
  const tableRef = useRef<HTMLTableElement>(null);
  const actionsRef = useRef<HTMLDivElement>();
  const { width: tableWidth } = useElementDimensions(tableRef);
  const columnIds = useMemo(
    () => columnSchemas.filter(({ id }) => visibleColumns.includes(id)).map(({ id }) => id),
    [columnSchemas, visibleColumns],
  );
  // eslint-disable-next-line react-hooks/refs
  const actionsColInnerWidth = fixedActionsCellWidth ?? actionsRef.current?.offsetWidth ?? 0;
  // eslint-disable-next-line react-hooks/refs
  const actionsColWidth = actionsColInnerWidth ? actionsColInnerWidth + CELL_PADDING * 2 : 0;

  const columnWidths = useColumnWidths<Entity>({
    actionsColWidth,
    bulkSelectColWidth: displayBulkSelectCol ? BULK_SELECT_COLUMN_WIDTH : 0,
    columnRenderersByAttribute,
    columnIds,
    tableWidth,
  });

  return { tableRef, actionsRef, columnWidths, actionsColWidth };
};

export default useElementWidths;

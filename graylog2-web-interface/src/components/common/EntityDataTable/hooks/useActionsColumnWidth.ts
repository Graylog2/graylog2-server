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

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

import { CELL_PADDING } from 'components/common/EntityDataTable/Constants';

import type { EntityBase } from '../types';

const useActionsColumnWidth = <Entity extends EntityBase>(entities: ReadonlyArray<Entity>, hasRowActions: boolean) => {
  const [rowWidths, setRowWidths] = useState<{ [rowId: string]: number }>({});
  const visibleRowIds = useMemo(() => new Set(entities.map(({ id }) => id)), [entities]);
  const visibleRowIdsRef = useRef<Set<string>>(visibleRowIds);

  useEffect(() => {
    visibleRowIdsRef.current = visibleRowIds;
  }, [visibleRowIds]);

  const handleWidthChange = useCallback((rowId: string, width: number) => {
    const rounded = Math.round(width);

    if (rounded <= 0 || !visibleRowIdsRef.current.has(rowId)) {
      return;
    }

    setRowWidths((cur) => (cur[rowId] === rounded ? cur : { ...cur, [rowId]: rounded }));
  }, []);

  const colMinWidth = useMemo(() => {
    if (!visibleRowIds.size) {
      return CELL_PADDING * 2;
    }

    const maxWidth = Math.max(
      0,
      ...Array.from(visibleRowIds)
        .map((rowId) => rowWidths[rowId])
        .filter((width) => !!width),
    );

    return hasRowActions ? maxWidth + CELL_PADDING * 2 : 0;
  }, [hasRowActions, rowWidths, visibleRowIds]);

  return { colMinWidth, handleWidthChange };
};

export default useActionsColumnWidth;

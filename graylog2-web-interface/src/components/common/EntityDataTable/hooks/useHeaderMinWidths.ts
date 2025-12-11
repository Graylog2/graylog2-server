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

import { useState, useMemo, useCallback } from 'react';

import { CELL_PADDING } from 'components/common/EntityDataTable/Constants';

const useHeaderMinWidths = () => {
  const [headerSectionsWidth, setHeaderSectionsWidth] = useState<{
    [colId: string]: { left?: number; right?: number };
  }>({});
  const headerMinWidths = useMemo(
    () =>
      Object.fromEntries(
        Object.entries(headerSectionsWidth).map(([colId, { left = 0, right = 0 }]) => [
          colId,
          Math.round(left + right) + CELL_PADDING * 2,
        ]),
      ),
    [headerSectionsWidth],
  );

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

  return { headerMinWidths, handleHeaderSectionResize };
};

export default useHeaderMinWidths;

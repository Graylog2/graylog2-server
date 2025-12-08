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

import { useCallback, useState } from 'react';

import { CELL_PADDING } from 'components/common/EntityDataTable/Constants';

const useActionsColumnWidth = () => {
  const [colMinWidth, setColMinWidth] = useState(0);

  const handleWidthChange = useCallback((width: number) => {
    const rounded = Math.round(width);
    if (rounded > 0) {
      setColMinWidth((cur) => (rounded > cur ? rounded : cur));
    }
  }, []);

  return { colMinWidth: colMinWidth + CELL_PADDING * 2, handleWidthChange };
};

export default useActionsColumnWidth;

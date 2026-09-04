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
import { useMemo, useState } from 'react';
import { useDndContext } from '@dnd-kit/core';

import DndStylesContext from './DndStylesContext';

const DndStylesProvider = ({ children }: React.PropsWithChildren) => {
  const [columnTransform, setColumnTransform] = useState<{ [colId: string]: string }>({});
  const dndContext = useDndContext();
  const activeColId = dndContext.active?.id.toString();

  const contextValue = useMemo(
    () => ({
      columnTransform,
      setColumnTransform,
      activeColId,
    }),
    [activeColId, columnTransform],
  );

  return <DndStylesContext.Provider value={contextValue}>{children}</DndStylesContext.Provider>;
};

export default DndStylesProvider;

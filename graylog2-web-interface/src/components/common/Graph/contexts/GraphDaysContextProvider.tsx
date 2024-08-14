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
import { useState, useMemo } from 'react';

import GraphDaysContext from 'components/common/Graph/contexts/GraphDaysContext';
import { DAYS } from 'components/common/Graph/types';

const GraphDaysContextProvider = ({ children }: { children: React.ReactNode }): React.ReactElement => {
  const [graphDays, setGraphDays] = useState<number>(DAYS[0]);

  return (
    <GraphDaysContext.Provider value={useMemo(() => ({ graphDays, setGraphDays }), [graphDays])}>
      {children}
    </GraphDaysContext.Provider>
  );
};

export default GraphDaysContextProvider;

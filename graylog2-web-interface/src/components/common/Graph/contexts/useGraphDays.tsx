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
import { useContext } from 'react';

import { DAYS } from 'components/common/Graph/types';
import type { GraphDaysContextType } from 'components/common/Graph/contexts/GraphDaysContext';
import GraphDaysContext from 'components/common/Graph/contexts/GraphDaysContext';

const useGraphDays = (): GraphDaysContextType => {
  try {
    const context = useContext(GraphDaysContext);

    if (!context) {
      throw new Error('useGraphDays must be used within a GraphDaysContextProvider');
    }

    return context;
  } catch {
    return { graphDays: DAYS[0], setGraphDays: () => undefined };
  }
};

export default useGraphDays;

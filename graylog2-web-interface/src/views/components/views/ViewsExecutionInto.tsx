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
import React from 'react';
import isEmpty from 'lodash/isEmpty';

import useViewsSelector from 'views/stores/useViewsSelector';
import { selectCurrentQueryResults } from 'views/logic/slices/viewSelectors';
import ExecutionInfo from 'views/components/views/ExecutionInfo';

const ViewsExecutionInfo = () => {
  const result = useViewsSelector(selectCurrentQueryResults);

  if (isEmpty(result)) {
    return <i>No query executed yet.</i>;
  }

  const total = result?.searchTypes && Object.values(result?.searchTypes)?.find((e) => e.total)?.total;

  return (
    <ExecutionInfo
      duration={result?.duration}
      executedAt={result?.timestamp}
      executionFinished={!isEmpty(result)}
      total={total}
    />
  );
};

export default ViewsExecutionInfo;

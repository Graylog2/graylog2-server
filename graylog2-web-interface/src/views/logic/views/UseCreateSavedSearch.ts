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

import View from 'views/logic/views/View';
import type { ElasticsearchQueryString, TimeRange } from 'views/logic/queries/Query';
import ViewGenerator from 'views/logic/views/ViewGenerator';

const useCreateSavedSearch = ({
  streamId,
  timeRange,
  queryString,
}:{
  streamId?: string | string[],
  timeRange?: TimeRange,
  queryString?: ElasticsearchQueryString,
}) => useMemo(
  () => ViewGenerator({ type: View.Type.Search, streamId, timeRange, queryString }),
  // eslint-disable-next-line react-hooks/exhaustive-deps
  [],
);

export default useCreateSavedSearch;

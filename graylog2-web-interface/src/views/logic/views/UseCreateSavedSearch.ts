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
import type Parameter from 'views/logic/parameters/Parameter';

type Props = {
  streamId?: string | string[];
  streamCategory?: string | string[];
  timeRange?: TimeRange;
  queryString?: ElasticsearchQueryString;
  parameters?: Array<Parameter>;
};

type Deps = Array<Props[keyof Props]> | [];
const useCreateSavedSearch = (
  { streamId, streamCategory, timeRange, queryString, parameters }: Props,
  deps: Deps = [],
) =>
  useMemo(
    () => ViewGenerator({ type: View.Type.Search, streamId, streamCategory, timeRange, queryString, parameters }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    deps,
  );

export default useCreateSavedSearch;

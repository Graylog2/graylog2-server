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
import type { TimeRange, ElasticsearchQueryString } from 'views/logic/queries/Query';
import UpdateSearchForWidgets from 'views/logic/views/UpdateSearchForWidgets';

import View from './View';
import ViewStateGenerator from './ViewStateGenerator';
import type { ViewType } from './View';

import Search from '../search/Search';
import QueryGenerator from '../queries/QueryGenerator';

export default async (
  type: ViewType,
  streamId: string | string[] | undefined | null,
  timeRange?: TimeRange,
  queryString?: ElasticsearchQueryString,
) => {
  const query = QueryGenerator(streamId, undefined, timeRange, queryString);
  const search = Search.create().toBuilder().queries([query]).build();
  const viewState = await ViewStateGenerator(type, streamId);

  const view = View.create()
    .toBuilder()
    .newId()
    .type(type)
    .state({ [query.id]: viewState })
    .search(search)
    .build();

  return UpdateSearchForWidgets(view);
};

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
import { DEFAULT_TIMERANGE } from 'views/Constants';
import Query, { createElasticsearchQueryString } from 'views/logic/queries/Query';
import Search from 'views/logic/search/Search';
import View from 'views/logic/views/View';
import ViewState from 'views/logic/views/ViewState';
import UpdateSearchForWidgets from 'views/logic/views/UpdateSearchForWidgets';
import type Widget from 'views/logic/widgets/Widget';
import createSearch from 'views/logic/slices/createSearch';

export const STANDALONE_QUERY_ID = 'standalone-query';

// A dashboard view, so each widget's own query, time range and streams are used instead of the query's.
const createStandaloneView = async (widgets: Array<Widget>, titles: { [widgetId: string]: string }) => {
  const query = Query.builder()
    .id(STANDALONE_QUERY_ID)
    .query(createElasticsearchQueryString())
    .timerange(DEFAULT_TIMERANGE)
    .build();
  const state = ViewState.create().toBuilder().widgets(widgets).titles({ widget: titles }).build();
  const view = UpdateSearchForWidgets(
    View.create()
      .toBuilder()
      .newId()
      .type(View.Type.Dashboard)
      .state({ [STANDALONE_QUERY_ID]: state })
      .search(Search.create().toBuilder().queries([query]).build())
      .build(),
  );
  const search = await createSearch(view.search);

  return view.toBuilder().search(search).build();
};

export default createStandaloneView;

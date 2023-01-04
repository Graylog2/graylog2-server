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
import { PluginStore } from 'graylog-web-plugin/plugin';

import View from 'views/logic/views/View';
import UpdateSearchForWidgets from 'views/logic/views/UpdateSearchForWidgets';

const CopyPageToDashboard = (queryId: string, sourceDashboard: View, targetDashboard: View): View => {
  if (targetDashboard.type !== View.Type.Dashboard) {
    return undefined;
  }

  const copyHooks = PluginStore.exports('views.hooks.copyWidgetToDashboard');

  const selectedViewState = sourceDashboard.state.get(queryId);
  const selectedQuery = sourceDashboard.search.queries.find((query) => query.id === queryId);
  const newQuery = selectedQuery.toBuilder().newId().build();
  const newState = targetDashboard.state.set(newQuery.id, selectedViewState.duplicate());
  const newQueries = targetDashboard.search.queries.add(newQuery);
  const newSearch = targetDashboard.search.toBuilder().queries(newQueries).build();

  const updatedDashboard = UpdateSearchForWidgets(targetDashboard.toBuilder()
    .state(newState)
    .search(newSearch)
    .build());

  return copyHooks.reduce((previousDashboard, copyHook) => copyHook(sourceDashboard, previousDashboard), updatedDashboard);
};

export default CopyPageToDashboard;

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
import * as Immutable from 'immutable';
import { PluginStore } from 'graylog-web-plugin/plugin';

import type View from 'views/logic/views/View';
import type SearchExecutionState from 'views/logic/search/SearchExecutionState';
import GlobalOverride from 'views/logic/search/GlobalOverride';

const visualizationDefinition = (visualization: string) =>
  PluginStore.exports('visualizationTypes').find(({ type }) => type === visualization);

const searchTypeIdsWithNullValuesForEmptyBuckets = (view: View): Array<string> =>
  (view.state ?? Immutable.Map())
    .valueSeq()
    .flatMap((viewState) =>
      viewState.widgets
        .filter((widget) => visualizationDefinition(widget.config?.visualization)?.nullValuesForEmptyBuckets)
        .flatMap((widget) => viewState.widgetMapping.get(widget.id, Immutable.Set<string>())),
    )
    .toArray();

// Options which depend on the visualization are passed with every execution, instead of being stored in the search types.
// This way they also apply to existing searches and searches from content packs.
const withVisualizationSearchTypeOptions = (executionState: SearchExecutionState, view: View | undefined) => {
  const searchTypeIds = view ? searchTypeIdsWithNullValuesForEmptyBuckets(view) : [];

  if (searchTypeIds.length === 0) {
    return executionState;
  }

  const globalOverride = executionState.globalOverride ?? GlobalOverride.empty();
  const searchTypes = globalOverride.searchTypes ?? {};
  const searchTypesWithOptions = {
    ...searchTypes,
    ...Object.fromEntries(
      searchTypeIds.map((searchTypeId) => [
        searchTypeId,
        { ...searchTypes[searchTypeId], null_values_for_empty_buckets: true },
      ]),
    ),
  };

  return executionState
    .toBuilder()
    .globalOverride(globalOverride.toBuilder().searchTypes(searchTypesWithOptions).build())
    .build();
};

export default withVisualizationSearchTypeOptions;

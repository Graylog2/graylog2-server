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
import { get } from 'lodash';

import View from 'views/logic/views/View';
import SearchTypesGenerator from 'views/logic/searchtypes/SearchTypesGenerator';

const UpdateSearchForWidgets = (view: View): View => {
  const { state: states } = view;
  const searchTypes = states.map((state) => SearchTypesGenerator(state.widgets));

  const search = get(view, 'search');
  const newQueries = search.queries
    .map((q) => q.toBuilder().searchTypes(searchTypes.get(q.id)?.searchTypes ?? []).build());
  const newSearch = search.toBuilder().newId().queries(newQueries.toOrderedSet()).build();
  let newView = view.toBuilder().search(newSearch).build();

  searchTypes.map(({ widgetMapping }) => widgetMapping)
    .forEach((widgetMapping, queryId) => {
      const newStates = newView.state;

      if (states.has(queryId)) {
        newView = newView.toBuilder()
          .state(newStates.update(queryId, (state) => state.toBuilder().widgetMapping(widgetMapping).build()))
          .build();
      }
    });

  return newView;
};

export default UpdateSearchForWidgets;

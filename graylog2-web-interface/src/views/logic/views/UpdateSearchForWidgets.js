// @flow strict
import { get } from 'lodash';
import View from 'views/logic/views/View';
import SearchTypesGenerator from 'views/logic/searchtypes/SearchTypesGenerator';

const UpdateSearchForWidgets = (view: View): View => {
  const { state: states } = view;
  const searchTypes = states.map((state) => SearchTypesGenerator(state.widgets));

  const search = get(view, 'search');
  const newQueries = search.queries
    .map((q) => q.toBuilder().searchTypes(searchTypes.get(q.id, {}).searchTypes).build());
  const newSearch = search.toBuilder().queries(newQueries).build();
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

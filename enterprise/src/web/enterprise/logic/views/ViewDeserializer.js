import * as Immutable from 'immutable';
import { get } from 'lodash';

import { SearchActions } from 'enterprise/stores/SearchStore';
import { ViewActions } from 'enterprise/stores/ViewStore';
import Search from 'enterprise/logic/search/Search';
import { SearchExecutionStateActions } from 'enterprise/stores/SearchExecutionStateStore';
import { SearchParameterStore } from 'enterprise/stores/SearchParameterStore';
import View from './View';

export default class ViewDeserializer {
  static deserializeFrom(viewResponse) {
    const view = View.fromJSON(viewResponse);
    return SearchActions.get(viewResponse.search_id)
      .then(search => Search.fromJSON(search))
      .then((search) => { // clear execution state
        SearchExecutionStateActions.clear();
        const searchParameters = get(search, 'parameters', Immutable.Set());
        SearchParameterStore.load(searchParameters);
        return search;
      })
      .then(search => view.toBuilder().search(search).build())
      .then((v) => { ViewActions.load(v); return v; });
  }
}

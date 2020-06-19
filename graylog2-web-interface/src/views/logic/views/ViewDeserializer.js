// @flow strict
import { SearchActions } from 'views/stores/SearchStore';
import { ViewActions } from 'views/stores/ViewStore';
import Search from 'views/logic/search/Search';
import type { SearchJson } from 'views/logic/search/Search';

import View from './View';
import type { ViewJson } from './View';

export default function ViewDeserializer(viewResponse: ViewJson): Promise<View> {
  const view: View = View.fromJSON(viewResponse);
  return SearchActions.get(viewResponse.search_id)
    .then((search: SearchJson): Search => Search.fromJSON(search))
    .then((search: Search): View => view.toBuilder().search(search).build())
    .then((v: View): Promise<View> => ViewActions.load(v).then(() => v));
}

// @flow strict
import View from './View';
import Search from '../search/Search';
import QueryGenerator from '../queries/QueryGenerator';
import ViewStateGenerator from './ViewStateGenerator';
import type { ViewType } from './View';

export default async (type: ViewType, streamId: ?string) => {
  const query = QueryGenerator(streamId);
  const search = Search.create().toBuilder().queries([query]).build();
  const viewState = await ViewStateGenerator(type, streamId);
  return View.create()
    .toBuilder()
    .type(type)
    .state({ [query.id]: viewState })
    .search(search)
    .build();
};
